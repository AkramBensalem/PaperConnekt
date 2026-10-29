package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akram.bensalem.paper.connekt.models.Paper
import com.akram.bensalem.paper.connekt.core.aiassistant.PaperAiTask
import com.akram.bensalem.paper.connekt.core.aiassistant.PaperTriageUsage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

class AssistantViewModel(
    private val repository: AssistantRepository,
    private val ideaNoteStore: IdeaNoteStore = syncedIdeaNoteStore()
) : ViewModel() {

    var state by mutableStateOf(AssistantUiState())
        private set
    private var selectedPaperId: String? = null
    private val ideaSaveMutex = Mutex()
    private var ideaLoadJob: Job? = null
    private var ideaLoadError: String? = null
    private var historyUserId: String? = null
    private var historyGeneration = 0
    private val historyMutex = Mutex()
    private var historyLoaded = false
    private data class StudyTimer(
        val userId: String,
        val generation: Int,
        val paperId: String,
        var lastRecordedAt: Long
    )
    private var studyTimer: StudyTimer? = null
    private var studyTimerJob: Job? = null

    fun updateHistoryAccount(userId: String?) {
        if (historyUserId == userId) {
            if (!historyLoaded) refreshHistory()
            return
        }
        stopStudyTimer()
        historyUserId = userId
        historyGeneration++
        historyLoaded = false
        state = state.copy(
            history = ResearchHistory(), historyError = null,
            historyAccountId = userId, historyLoading = userId != null
        )
        refreshHistory()
    }

    fun refreshHistory() {
        val userId = historyUserId
        val generation = historyGeneration
        viewModelScope.launch {
            historyMutex.withLock {
                if (historyGeneration != generation) return@withLock
                state = state.copy(historyLoading = true, historyError = null)
                try {
                    val history = repository.loadUsageHistory(userId)
                    if (historyGeneration == generation) {
                        historyLoaded = true
                        state = state.copy(history = history, historyLoading = false)
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    Napier.e("Could not load research history", exception)
                    if (historyGeneration == generation) {
                        historyLoaded = false
                        state = state.copy(historyLoading = false, historyError = "Could not load research history. ${exception.message.orEmpty()}")
                    }
                }
            }
        }
    }

    private fun recordWorkspaceOpen(target: ZoteroPdfTarget) {
        val userId = historyUserId ?: return
        val generation = historyGeneration
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            historyMutex.withLock {
                if (historyGeneration != generation) return@withLock
                try {
                    // Read before writing so an unreadable history file is never silently replaced.
                    val history = if (historyLoaded) state.history else repository.loadHistory(userId)
                    if (historyGeneration != generation) return@withLock
                    val updated = history.record(target, timestamp)
                    repository.saveHistory(userId, updated)
                    if (historyGeneration == generation) {
                        historyLoaded = true
                        state = state.copy(history = updated, historyLoading = false, historyError = null)
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    Napier.e("Could not save research history", exception)
                    if (historyGeneration == generation) {
                        state = state.copy(historyLoading = false, historyError = "Could not save this workspace visit. ${exception.message.orEmpty()}")
                    }
                }
            }
        }
    }

    private fun startStudyTimer(target: ZoteroPdfTarget) {
        stopStudyTimer()
        val userId = historyUserId ?: return
        val timer = StudyTimer(
            userId = userId,
            generation = historyGeneration,
            paperId = StudiedPaper(target, 0L, 0L).id,
            lastRecordedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )
        studyTimer = timer
        studyTimerJob = viewModelScope.launch {
            try {
                while (studyTimer === timer) {
                    delay(STUDY_TIME_SAVE_INTERVAL_MILLIS)
                    recordStudyTime(timer, kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
                }
            } finally {
                withContext(NonCancellable) {
                    if (studyTimer === timer) studyTimer = null
                    recordStudyTime(timer, kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), allowInactiveAccount = true)
                }
            }
        }
    }

    private fun stopStudyTimer() {
        val timer = studyTimer
        studyTimer = null
        studyTimerJob?.cancel()
        studyTimerJob = null
        if (timer != null) viewModelScope.launch {
            withContext(NonCancellable) {
                recordStudyTime(timer, kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), allowInactiveAccount = true)
            }
        }
    }

    private suspend fun recordStudyTime(
        timer: StudyTimer,
        timestamp: Long,
        allowInactiveAccount: Boolean = false
    ) {
        historyMutex.withLock {
            val activeAccount = historyGeneration == timer.generation && historyUserId == timer.userId
            if (!activeAccount && !allowInactiveAccount) return@withLock
            val fromTimestamp = timer.lastRecordedAt
            if (timestamp <= fromTimestamp) return@withLock
            try {
                // Load before the first time update so an unreadable history is never overwritten.
                val history = if (activeAccount && historyLoaded) state.history else repository.loadHistory(timer.userId)
                check(history.papers.any { it.id == timer.paperId }) { "The paper is missing from research history." }
                val updated = history.recordStudyTime(timer.paperId, fromTimestamp, timestamp)
                if (updated != history) repository.saveHistory(timer.userId, updated)
                timer.lastRecordedAt = timestamp
                if (activeAccount) {
                    historyLoaded = true
                    state = state.copy(history = updated, historyLoading = false, historyError = null)
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                Napier.e("Could not save research study time", exception)
                if (activeAccount) state = state.copy(
                    historyLoading = false,
                    historyError = "Could not save study time. ${exception.message.orEmpty()}"
                )
            }
        }
    }

    suspend fun recordUsage(paperId: String?, event: AiUsageEvent) {
        val userId = historyUserId ?: return
        historyMutex.withLock {
            val history = if (historyLoaded) state.history else repository.loadHistory(userId)
            val updated = history.recordUsage(paperId, event)
            if (updated != history) repository.saveHistory(userId, updated)
            historyLoaded = true
            state = state.copy(history = updated, historyLoading = false, historyError = null)
        }
    }

    suspend fun recordGraphTriageUsage(usage: PaperTriageUsage) {
        historyMutex.withLock {
            try {
                val graphUsage = repository.recordGraphUsage(
                    usage.selectedPaperId,
                    AiUsageEvent(
                        service = if (usage.task == PaperAiTask.RELEVANCE_SCORING) AiUsageService.RELEVANCE_SCORING
                        else AiUsageService.LITERATURE_TRIAGE,
                        timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                        inputUnits = usage.estimatedInputTokens,
                        outputUnits = usage.estimatedOutputTokens,
                        model = usage.model
                    )
                )
                state = state.copy(history = state.history.copy(graphUsage = graphUsage))
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                Napier.e("Could not save AI literature usage.", exception)
                state = state.copy(historyError = "Could not save AI literature cost. ${exception.message.orEmpty()}")
                throw IllegalStateException("Could not save AI literature cost. ${exception.message.orEmpty()}", exception)
            }
        }
    }

    fun selectPaper(paper: Paper) {
        ideaLoadJob?.cancel()
        ideaLoadError = null
        selectedPaperId = paper.id
        state = state.copy(
            selectedPaper = paper,
            notes = emptyList(),
            ideas = emptyList(),
            ideasLoading = true,
            ideaError = null,
            isWorkplaceActive = true,
            chatMessages = listOf(
                ChatMessage(
                    id = "1",
                    sender = MessageSender.AI_AGENT,
                    content = "Hello! I'm your AI Research Assistant. I've analyzed '${paper.title}'. How can I help you today?"
                )
            )
        )
        loadIdeas(paper.id)
    }

    fun selectZoteroPaper(target: ZoteroPdfTarget) {
        stopStudyTimer()
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val item = target.item
        selectPaper(Paper(
            id = target.groupId?.let { "zotero:group:$it:${target.attachment.key}" } ?: "zotero:${target.attachment.key}",
            title = item.data.title ?: target.attachment.data.filename ?: "Zotero PDF",
            authors = item.data.creators.map { it.name ?: listOfNotNull(it.firstName, it.lastName).joinToString(" ") },
            abstract = item.data.abstractNote,
            publicationDate = item.data.date,
            doi = item.data.doi,
            createdAt = now, updatedAt = now, visitedAt = now
        ))
        state = state.copy(chatMessages = emptyList())
        recordWorkspaceOpen(target)
        startStudyTimer(target)
    }

    fun exitWorkplace() {
        stopStudyTimer()
        selectedPaperId = null
        state = state.copy(isWorkplaceActive = false, selectedPaper = null)
    }

    fun addNote(content: String, pageNumber: Int? = null) {
        val newNote = Note(
            id = (state.notes.size + 1).toString(),
            content = content,
            pageNumber = pageNumber
        )
        state = state.copy(notes = state.notes + newNote)
    }

    fun deleteNote(id: String) {
        state = state.copy(notes = state.notes.filter { it.id != id })
    }

    fun sendMessage(content: String) {
        val userMessage = ChatMessage(
            id = (state.chatMessages.size + 1).toString(),
            sender = MessageSender.USER,
            content = content
        )
        state = state.copy(chatMessages = state.chatMessages + userMessage)

        // Mock AI response
        viewModelScope.launch {
            val aiMessage = ChatMessage(
                id = (state.chatMessages.size + 1).toString(),
                sender = MessageSender.AI_AGENT,
                content = "That's an interesting point about '${state.selectedPaper?.title}'. According to my analysis, this relates to the transformer architecture's attention mechanism."
            )
            state = state.copy(chatMessages = state.chatMessages + aiMessage)
        }
    }

    fun addIdea(title: String, description: String) {
        val paperId = selectedPaperId ?: return
        editIdea(paperId) { createIdeaNote(paperId, title, description) }
    }

    fun updateIdea(id: String, title: String, description: String, pageColor: String) {
        val paperId = selectedPaperId ?: return
        editIdea(paperId) { updateIdeaNote(paperId, id, title, description, pageColor) }
    }

    suspend fun awaitIdeaNotes(paperId: String): List<Idea> {
        require(selectedPaperId == paperId) { "The selected paper has changed." }
        ideaLoadJob?.join()
        require(selectedPaperId == paperId) { "The selected paper has changed." }
        check(!state.ideasLoading) { "Idea notes are still loading." }
        check(ideaLoadError == null) { ideaLoadError.orEmpty() }
        return state.ideas
    }

    suspend fun createIdeaNote(paperId: String, title: String, content: String): Idea =
        saveIdeaNote(paperId) { ideas ->
            val idea = Idea(newIdeaId(), title.trim().also {
                require(it.isNotEmpty()) { "The idea title is empty." }
            }, content, x = 100f, y = 100f)
            idea to (ideas + idea)
        }

    suspend fun updateIdeaNote(
        paperId: String, id: String, title: String?, content: String?, pageColor: String? = null
    ): Idea = saveIdeaNote(paperId) { ideas ->
        val existing = requireNotNull(ideas.find { it.id == id }) { "No Idea Board note with ID $id." }
        val idea = existing.copy(
            title = title?.trim()?.also { require(it.isNotEmpty()) { "The idea title is empty." } } ?: existing.title,
            description = content ?: existing.description,
            pageColor = pageColor ?: existing.pageColor
        )
        idea to ideas.map { if (it.id == id) idea else it }
    }

    private suspend fun saveIdeaNote(
        paperId: String, update: (List<Idea>) -> Pair<Idea, List<Idea>>
    ): Idea {
        awaitIdeaNotes(paperId)
        return ideaSaveMutex.withLock {
            require(selectedPaperId == paperId) { "The selected paper has changed." }
            val (idea, ideas) = update(state.ideas)
            try {
                ideaNoteStore.save(paperId, ideas)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                if (selectedPaperId == paperId) {
                    state = state.copy(ideaError = "Could not save idea notes: ${exception.message}")
                }
                throw exception
            }
            if (selectedPaperId == paperId) state = state.copy(ideas = ideas, ideaError = null)
            return@withLock idea
        }
    }

    private fun editIdea(paperId: String, edit: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                edit()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                Napier.e("Could not edit idea note", exception)
                if (selectedPaperId == paperId && state.ideaError == null) {
                    state = state.copy(ideaError = "Could not edit idea note: ${exception.message}")
                }
            }
        }
    }

    private fun loadIdeas(paperId: String) {
        ideaLoadJob = viewModelScope.launch {
            try {
                val ideas = ideaNoteStore.load(paperId)
                if (selectedPaperId == paperId) {
                    state = state.copy(ideas = ideas, ideasLoading = false, ideaError = null)
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                if (selectedPaperId == paperId) {
                    ideaLoadError = "Could not load idea notes: ${exception.message}"
                    state = state.copy(ideasLoading = false, ideaError = ideaLoadError)
                }
            }
        }
    }
}

private const val STUDY_TIME_SAVE_INTERVAL_MILLIS = 15_000L

private fun newIdeaId(): String =
    "${kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(36)}-${Random.nextLong().toULong().toString(36)}"
