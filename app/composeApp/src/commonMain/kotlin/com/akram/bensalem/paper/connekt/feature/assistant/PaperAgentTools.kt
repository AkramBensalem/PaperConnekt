package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentParameterType.*
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentTool
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentToolArguments
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentToolParameter
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroAnnotation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


val agentColors = linkedMapOf(
    "yellow" to "#ffd400", "red" to "#ff6666", "green" to "#5fb236", "blue" to "#2ea8e5",
    "purple" to "#a28ae5", "magenta" to "#e56eee", "orange" to "#f19837", "gray" to "#aaaaaa"
)

fun agentColor(value: String?, fallback: String): String {
    val name = value?.trim()?.lowercase().orEmpty()
    if (name.isEmpty()) return fallback
    agentColors[name]?.let { return it }
    if (name == "grey") return agentColors.getValue("gray")
    if (name == "pink") return agentColors.getValue("magenta")
    val hex = if (name.startsWith("#")) name else "#$name"
    require(hex.matches(Regex("#[0-9a-f]{6}"))) { "Unknown color '$value'. Use one of ${agentColors.keys.joinToString()} or #rrggbb." }
    return hex
}

private fun colorName(hex: String) = agentColors.entries.firstOrNull { it.value.equals(hex, true) }?.key ?: hex

private val markdownImage = Regex("""!\[[^\]]*]\([^)]*\)""")
private val markdownHeading = Regex("""^#{1,4}\s+(.+)$""", RegexOption.MULTILINE)

internal fun agentMarkdown(page: String) = page.replace(markdownImage, "[figure]").trim()

private fun excerpt(text: String, max: Int) = text.replace(Regex("\\s+"), " ").trim().let { if (it.length > max) it.take(max - 1) + "…" else it }

internal fun ZoteroAnnotation.agentSummary(): String = buildString {
    val position = position
    append("- key=").append(key).append(" page=").append(position.pageIndex + 1).append(" type=")
    append(if (isIdea) "idea" else if (type == "image") "area" else type)
    append(" color=").append(colorName(color))
    if (isAiGenerated) append(" (by AI)")
    if (text.isNotBlank()) append(" text=\"").append(excerpt(text, 90)).append('"')
    if (comment.isNotBlank()) append(" note=\"").append(excerpt(comment, 140)).append('"')
}

/** Everything the agent should know about the open paper. The PDF itself is never sent; only its Markdown version. */
fun paperAgentSystemPrompt(
    state: PdfWorkspaceState, maxMarkdownChars: Int = 80_000,
    includePageContext: Boolean = true, includePaperContext: Boolean = true,
    ideas: List<Idea>? = null,
    ideaBoardError: String? = null
): String = buildString {
    val item = state.target?.item?.data
    val pages = state.markdown.pages
    appendLine("You are PaperConnekt's research assistant, embedded next to a PDF reader. You help the user understand, critique and annotate the open paper.")
    appendLine()
    appendLine("## Paper")
    appendLine("Title: ${state.localTitle ?: item?.title ?: state.target?.attachment?.data?.title ?: "Untitled"}")
    if (state.localTitle != null) {
        appendLine("This is a local PDF. Zotero annotation writes are unavailable; use the Idea Board for saved research notes.")
    }
    item?.creators?.mapNotNull { it.name ?: listOfNotNull(it.firstName, it.lastName).joinToString(" ").ifBlank { null } }
        ?.takeIf { it.isNotEmpty() }?.let { appendLine("Authors: ${it.take(12).joinToString()}${if (it.size > 12) ", et al." else ""}") }
    item?.date?.takeIf { it.isNotBlank() }?.let { appendLine("Date: $it") }
    item?.publicationTitle?.takeIf { it.isNotBlank() }?.let { appendLine("Venue: $it") }
    appendLine("PDF pages: ${state.pageCount}")
    if (includePageContext) {
        appendLine()
        appendLine("## Reader state right now")
        append(paperAgentLiveContext(state))
    }
    if (ideas != null || ideaBoardError != null) {
        appendLine()
        appendLine("## Idea Board (separate Markdown files for this paper)")
        appendLine(if (ideas != null) ideaBoardContext(ideas) else "Idea Board unavailable: $ideaBoardError")
        appendLine("Use read_idea to read a note's complete Markdown, create_idea to create a file, and update_idea to fill or edit an existing file. These tools save directly to the Idea Board.")
    }
    appendLine()
    when {
        includePaperContext -> {
            appendLine("## Paper content (Markdown version produced by OCR)")
            when {
                pages.isEmpty() && state.markdown.missingApiKey -> appendLine("Not available: the Mistral API key is missing. Tell the user to add it in Settings so you can read the paper. You can still list and manage annotations.")
                pages.isEmpty() -> appendLine("Not available yet${state.markdown.error?.let { " ($it)" } ?: ""}. Call read_pages to load it.")
                else -> {
                    if (!state.markdown.hasPageBoundaries) appendLine("This Markdown has no page boundaries, so page numbers below are approximate.")
                    val cleaned = pages.map(::agentMarkdown)
                    if (cleaned.sumOf { it.length } <= maxMarkdownChars) {
                        cleaned.forEachIndexed { index, page -> appendLine("<page number=\"${index + 1}\">").appendLine(page).appendLine("</page>") }
                    } else {
                        appendLine("The paper is long, so only an outline and the pages around the reader are included. Use read_pages or search_paper for anything else.")
                        appendLine("### Outline")
                        cleaned.forEachIndexed { index, page ->
                            markdownHeading.findAll(page).take(8).forEach { appendLine("- p${index + 1}: ${excerpt(it.groupValues[1], 100)}") }
                        }
                        val current = state.pageIndex.coerceIn(0, cleaned.lastIndex)
                        (maxOf(0, current - 1)..minOf(cleaned.lastIndex, current + 1)).forEach { index ->
                            appendLine("<page number=\"${index + 1}\">").appendLine(cleaned[index].take(maxMarkdownChars / 4)).appendLine("</page>")
                        }
                    }
                }
            }
        }
        includePageContext -> {
            appendLine("## Current page content (Markdown version produced by OCR)")
            val currentPage = pages.getOrNull(state.pageIndex)
            if (currentPage != null) appendLine("<page number=\"${state.pageIndex + 1}\">\n${agentMarkdown(currentPage)}\n</page>")
            else appendLine("Not available yet${state.markdown.error?.let { " ($it)" } ?: ""}. Call read_pages to load it.")
        }
        else -> appendLine("## Paper content\nThe page and whole-paper content were excluded for this question. Use read_pages or search_paper only if the user asks for paper details.")
    }
    appendLine()
    appendLine(
        """
        ## How to work
        - Ground every answer in the supplied Markdown content (or read_pages/search_paper when context was excluded). Cite pages like "(p. 3)". Never invent content that is not in the paper; say so when something is missing.
        - "This page", "here" or "the current page" refer to the reader's current page. "This", "the selection" or "the selected text" refer to the selected text or selected annotation.
        - You can change the PDF exactly like the user can: highlight_text, underline via style, highlight_keywords, add_note, add_text, draw_shape, draw_freehand, update_annotation, delete_annotation, go_to_page.
        - To anchor anything on text, pass a short, distinctive verbatim phrase (4–20 words) from the Markdown on that page. Long passages are fine for highlights, but copy them exactly. Math and tables may not match; anchor on nearby words instead.
        - Notes are comments attached to the paper. Page ideas (add_note with as_idea=true) are annotations placed on the PDF, NOT Idea Board Markdown files. For requests about the Idea Board or its Markdown notes/files, use list_ideas/read_idea/create_idea/update_idea instead. Create ideas only when the user asks for ideas or brainstorming.
        - Before filling or editing an existing Idea Board note, read it by ID and preserve content the user did not ask to change. Pass the complete updated Markdown body to update_idea. Never claim a note was saved unless the tool succeeded.
        - Keep annotation comments concise and useful. Prefer one well-placed annotation over many noisy ones, unless the user asks for many.
        - Only delete or edit annotations the user asked about. Never delete the user's own annotations unless explicitly told to.
        - Format replies as clean Markdown: a short `###` heading only when the answer has sections, **bold** key terms, bullet or numbered lists for enumerations, tables for comparisons. Write math in LaTeX: `${'$'}...${'$'}` inline and `${'$'}${'$'}...${'$'}${'$'}` on its own lines for display equations.
        - Reply in Markdown, briefly. After changing the PDF, summarize what you did in one or two sentences.
        """.trimIndent()
    )
}

internal fun ideaBoardContext(ideas: List<Idea>): String =
    if (ideas.isEmpty()) "Idea Board: no Markdown notes yet."
    else ideas.joinToString("\n") { "id=${it.id} title=\"${excerpt(it.title, 160)}\" preview=\"${excerpt(it.description, 240)}\"" }

fun paperAgentLiveContext(state: PdfWorkspaceState): String = buildString {
    appendLine("Current page: ${state.pageIndex + 1} of ${state.readerPageCount} (${if (state.viewMode == ReaderViewMode.Markdown) "Markdown view" else "PDF view"})")
    state.textSelection?.text?.takeIf { it.isNotBlank() }?.let { appendLine("Selected text on page ${state.pageIndex + 1}: \"${excerpt(it, 1_500)}\"") }
    state.annotations.find { it.key == state.selectedKey }?.let { appendLine("Selected annotation:\n${it.agentSummary()}") }
    val annotations = state.annotations
    if (annotations.isEmpty()) appendLine("Annotations: none yet.")
    else {
        appendLine("Annotations (${annotations.size}${if (annotations.size > 60) ", showing those nearest the current page" else ""}):")
        annotations.sortedBy { kotlin.math.abs(it.position.pageIndex - state.pageIndex) }.take(60)
            .sortedWith(compareBy({ it.position.pageIndex }, { it.key })).forEach { appendLine(it.agentSummary()) }
    }
}

/**
 * The agent's hands. Every tool runs on [ui] because it reads and mutates the workspace's Compose state. Page numbers are
 * 1-based and free positions are fractions of the page (0,0 = top-left, 1,1 = bottom-right) so the model never needs points.
 */
class PaperAgentToolbox(
    private val workspace: PdfWorkspaceViewModel,
    private val ui: CoroutineContext = Dispatchers.Main,
    private val ideaBoard: AssistantViewModel? = null,
    private val ideaPaperId: String? = ideaBoard?.state?.selectedPaper?.id,
    private val onAction: (PaperAgentAction) -> Unit = {}
) {
    private val state get() = workspace.state

    val tools: List<AgentTool> get() = listOf(
        tool("get_reader_context", "Get the reader's current page, selected text, selected annotation and the annotation list. Call it again after the user may have moved.") { paperAgentLiveContext(state) },
        tool("read_pages", "Read the Markdown content of one or more pages.", int("start_page", "First page (1-based)."), int("end_page", "Last page, inclusive. Defaults to start_page.", false)) { args ->
            val pages = markdownPages() ?: return@tool missingMarkdown()
            val start = args.requireInt("start_page").coerceIn(1, pages.size)
            val end = (args.int("end_page") ?: start).coerceIn(start, minOf(pages.size, start + 5))
            (start..end).joinToString("\n") { "<page number=\"$it\">\n${agentMarkdown(pages[it - 1])}\n</page>" }
        },
        tool("search_paper", "Find where words or a phrase appear in the paper's Markdown. Returns pages with short snippets.", string("query", "Words or phrase to find.")) { args ->
            val pages = markdownPages() ?: return@tool missingMarkdown()
            val query = args.requireString("query").trim()
            val results = pages.flatMapIndexed { index, page ->
                Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(page).take(3).map { match ->
                    "p${index + 1}: …${excerpt(page.substring(maxOf(0, match.range.first - 80), minOf(page.length, match.range.last + 80)), 200)}…"
                }.toList()
            }
            if (results.isEmpty()) "No matches for \"$query\"." else results.take(25).joinToString("\n")
        },
        tool("list_annotations", "List existing annotations with their keys.", int("page", "Only this page (1-based).", false)) { args ->
            val page = args.int("page")
            state.annotations.filter { page == null || it.position.pageIndex == page - 1 }
                .joinToString("\n") { it.agentSummary() }.ifEmpty { "No annotations${page?.let { " on page $it" } ?: ""}." }
        },
        tool("go_to_page", "Scroll the reader to a page so the user can see it.", int("page", "Page number (1-based).")) { args ->
            val page = args.requireInt("page")
            require(page in 1..state.readerPageCount) { "Page must be between 1 and ${state.readerPageCount}." }
            workspace.goToPage(page - 1)
            "Showing page $page."
        },
        tool("select_annotation", "Scroll to an annotation and select it so the user sees it.", string("key", "Annotation key.")) { args ->
            val key = args.requireString("key")
            requireNotNull(state.annotations.find { it.key == key }) { "No annotation with key $key." }
            workspace.revealAnnotation(key)
            "Selected $key."
        },
        tool("highlight_text", "Highlight or underline a verbatim passage on a page, optionally with a comment.",
            int("page", "Page number (1-based)."), string("text", "Exact passage to mark, copied from the paper."),
            color(), string("comment", "Optional note attached to the highlight.", false),
            AgentToolParameter("style", "highlight (default) or underline.", STRING, false, listOf("highlight", "underline"))
        ) { args ->
            val style = args.string("style")?.lowercase()?.takeIf { it == "underline" } ?: "highlight"
            val match = locate(args.requireInt("page"), args.requireString("text"))
            val draft = AnnotationDraft(style, match.pageIndex, match.selection.rects.map(geometry(match.pageIndex)::pdfRect),
                text = match.selection.text, comment = args.string("comment").orEmpty(), color = agentColor(args.string("color"), state.annotationColor))
            created(draft, "${style.replaceFirstChar(Char::uppercase)} “${excerpt(match.selection.text, 40)}”")
        },
        tool("highlight_keywords", "Highlight every occurrence of keywords or key terms across the paper (or chosen pages).",
            AgentToolParameter("keywords", "Words or short phrases to highlight.", STRING_LIST),
            AgentToolParameter("pages", "Only these pages (1-based). Defaults to all pages.", NUMBER_LIST, false),
            color(), AgentToolParameter("style", "highlight (default) or underline.", STRING, false, listOf("highlight", "underline")),
            AgentToolParameter("whole_word", "Match whole words only. Default true.", BOOLEAN, false),
            int("max_per_keyword", "Maximum occurrences per keyword. Default 20.", false)
        ) { args ->
            val keywords = args.strings("keywords").map(String::trim).filter(String::isNotEmpty).distinct()
            require(keywords.isNotEmpty()) { "Give at least one keyword." }
            val style = args.string("style")?.lowercase()?.takeIf { it == "underline" } ?: "highlight"
            val color = agentColor(args.string("color"), state.annotationColor)
            val pages = args.floats("pages").map { it.toInt() - 1 }.filter { it in 0 until state.pageCount }.ifEmpty { (0 until state.pageCount).toList() }
            val limit = (args.int("max_per_keyword") ?: 20).coerceIn(1, 100)
            val wholeWord = args.boolean("whole_word") ?: true
            val existing = state.annotations.filter { it.type == style }.map { it.position.pageIndex to normalizeForSearch(it.text) }.toSet()
            val counts = mutableMapOf<String, Int>()
            val drafts = mutableListOf<AnnotationDraft>()
            for (index in pages) {
                val page = workspace.pageText(index) ?: continue
                for (keyword in keywords) {
                    val left = limit - counts.getOrElse(keyword) { 0 }
                    if (left <= 0) continue
                    page.find(keyword, wholeWord, left).filter { (index to normalizeForSearch(it.selection.text)) !in existing }.forEach { match ->
                        drafts += AnnotationDraft(style, index, match.selection.rects.map(page.geometry::pdfRect), text = match.selection.text, color = color)
                        counts[keyword] = counts.getOrElse(keyword) { 0 } + 1
                    }
                }
                if (drafts.size >= 300) break
            }
            if (drafts.isEmpty()) return@tool "None of the keywords were found in the PDF text layer${if (wholeWord) " as whole words" else ""}."
            val keys = workspace.agentCreateAnnotations(drafts.take(300), reveal = false)
            onAction(PaperAgentAction("${keys.size} keyword ${style}s", drafts.first().pageIndex, keys.first()))
            "Created ${keys.size} ${style}s: " + keywords.joinToString { "\"$it\" ×${counts[it] ?: 0}" }
        },
        tool("add_note", "Add a sticky note (or a research idea) on a page, next to a passage or at a position.",
            int("page", "Page number (1-based)."), string("note", "The note text (Markdown allowed)."),
            string("anchor_text", "Verbatim phrase the note refers to; the note is placed at its top-end.", false),
            number("x", "Horizontal position 0–1 when there is no anchor."), number("y", "Vertical position 0–1 when there is no anchor."),
            color(), AgentToolParameter("as_idea", "Save as a research idea instead of a note.", BOOLEAN, false)
        ) { args ->
            val note = args.requireString("note").trim().also { require(it.isNotEmpty()) { "The note is empty." } }
            val (pageIndex, rect) = place(args, 22f, 22f) { anchor, g ->
                Rect(anchor.right + 2f, anchor.top - 11f, anchor.right + 24f, anchor.top + 11f).clampTo(g)
            }
            val idea = args.boolean("as_idea") == true
            created(AnnotationDraft("note", pageIndex, listOf(geometry(pageIndex).pdfRect(rect)), comment = note,
                color = agentColor(args.string("color"), state.annotationColor), isIdea = idea), "${if (idea) "Idea" else "Note"}: ${excerpt(note, 40)}")
        },
        tool("add_text", "Write visible text directly on the page (a text box), below a passage or at a position.",
            int("page", "Page number (1-based)."), string("text", "Text to write."),
            string("anchor_text", "Verbatim phrase to write below.", false),
            number("x", "Left edge 0–1 when there is no anchor."), number("y", "Top edge 0–1 when there is no anchor."),
            number("font_size", "Font size in points (8–32). Default is the reader's text size."), color()
        ) { args ->
            val text = args.requireString("text").trim().also { require(it.isNotEmpty()) { "The text is empty." } }
            val fontSize = (args.float("font_size") ?: state.textSize).coerceIn(6f, 48f)
            val (pageIndex, origin) = place(args, 1f, 1f) { anchor, _ -> Rect(anchor.left, anchor.bottom + 3f, anchor.left + 1f, anchor.bottom + 4f) }
            val g = geometry(pageIndex)
            val box = textBoxRect(origin.left.coerceIn(0f, g.displayWidth - fontSize * 2f), origin.top.coerceIn(0f, g.displayHeight - fontSize * 1.5f), text, fontSize, g.displayWidth)
            created(AnnotationDraft("text", pageIndex, listOf(g.pdfRect(box)), comment = text, fontSize = fontSize,
                color = agentColor(args.string("color"), "#ff6666")), "Text: ${excerpt(text, 40)}")
        },
        tool("draw_shape", "Draw on the page around/next to a passage or at a position: area (rectangle selection), box, ellipse, underline, arrow (pointing at it), bracket (in the margin), checkmark or cross.",
            int("page", "Page number (1-based)."),
            AgentToolParameter("shape", "Shape to draw.", STRING, true, listOf("area", "box", "ellipse", "underline", "arrow", "bracket", "checkmark", "cross")),
            string("anchor_text", "Verbatim phrase to draw around or next to.", false),
            number("x", "Left 0–1 when there is no anchor."), number("y", "Top 0–1 when there is no anchor."),
            number("width", "Width 0–1 when there is no anchor."), number("height", "Height 0–1 when there is no anchor."),
            color(), number("line_width", "Stroke width in points (0.5–12). Default is the reader's pen width."),
            string("comment", "Optional comment attached to the drawing.", false)
        ) { args ->
            val shape = args.requireString("shape").lowercase()
            val (pageIndex, bounds) = place(args, 0f, 0f) { anchor, _ -> anchor }
            val g = geometry(pageIndex)
            val color = agentColor(args.string("color"), state.annotationColor)
            val comment = args.string("comment").orEmpty()
            if (shape == "area") {
                val area = (if (args.string("anchor_text").isNullOrBlank()) bounds else bounds.inflate(2f)).clampTo(g)
                return@tool created(AnnotationDraft("image", pageIndex, listOf(g.pdfRect(area)), comment = comment, color = color), "Area on page ${pageIndex + 1}")
            }
            val strokes = shapeStrokes(shape, bounds).map { stroke -> stroke.map { it.clampTo(g) } }
            ink(pageIndex, strokes, color, args.float("line_width"), comment, "${shape.replaceFirstChar(Char::uppercase)} on page ${pageIndex + 1}")
        },
        tool("draw_freehand", "Draw a freehand pen stroke through points given as fractions of the page.",
            int("page", "Page number (1-based)."),
            AgentToolParameter("points", "Flat list x1,y1,x2,y2,… of at least two points, each 0–1 (0,0 = top-left).", NUMBER_LIST),
            color(), number("line_width", "Stroke width in points (0.5–12).")
        ) { args ->
            val pageIndex = page(args)
            val g = geometry(pageIndex)
            val values = args.floats("points")
            require(values.size >= 4 && values.size % 2 == 0) { "Give an even number of coordinates for at least two points." }
            val points = values.chunked(2).map { Offset(it[0].coerceIn(0f, 1f) * g.displayWidth, it[1].coerceIn(0f, 1f) * g.displayHeight) }
            ink(pageIndex, listOf(points), agentColor(args.string("color"), state.annotationColor), args.float("line_width"), "", "Drawing on page ${pageIndex + 1}")
        },
        tool("update_annotation", "Change the note/comment or color of an existing annotation.",
            string("key", "Annotation key."), string("note", "New note text. Omit to keep it.", false), color()
        ) { args ->
            val key = args.requireString("key")
            val color = args.string("color")?.let { agentColor(it, "") }
            workspace.agentUpdateAnnotation(key, args.string("note"), color)
            state.annotations.find { it.key == key }?.let { onAction(PaperAgentAction("Updated ${it.type}", it.position.pageIndex, key)) }
            "Updated $key."
        },
        tool("delete_annotation", "Delete an annotation. Only when the user explicitly asks.", string("key", "Annotation key.")) { args ->
            val key = args.requireString("key")
            val page = state.annotations.find { it.key == key }?.position?.pageIndex ?: 0
            workspace.agentDeleteAnnotation(key)
            onAction(PaperAgentAction("Deleted annotation", page))
            "Deleted $key."
        }
    ) + if (ideaBoard != null && ideaPaperId != null) ideaTools() else emptyList()

    private fun ideaTools(): List<AgentTool> = listOf(
        tool("list_ideas", "List the Idea Board's Markdown notes for this paper, with IDs, titles and previews. These are not PDF annotations.") {
            ideaBoardContext(boardIdeas())
        },
        tool("read_idea", "Read the complete Markdown body of an Idea Board note before editing it.",
            string("id", "Idea Board note ID from list_ideas.")
        ) { args ->
            val id = args.requireString("id")
            val idea = requireNotNull(boardIdeas().find { it.id == id }) { "No Idea Board note with ID $id." }
            "id=${idea.id}\nTitle: ${idea.title}\n\n${idea.description}"
        },
        tool("create_idea", "Create and save a new Markdown file in the Idea Board, not a sticky note on the PDF.",
            string("title", "Note title, without the .md extension."),
            string("content", "Complete Markdown body to save. May be empty for a new blank note.")
        ) { args ->
            val idea = requireNotNull(ideaBoard).createIdeaNote(requireNotNull(ideaPaperId),
                args.requireString("title"),
                requireNotNull(args.values["content"] as? String) { "Content must be a Markdown string." })
            ideaSaved(idea, "Created")
        },
        tool("update_idea", "Fill or edit an existing Idea Board Markdown file. Read it first; content replaces the complete body. Omitted fields are preserved.",
            string("id", "Idea Board note ID from list_ideas/read_idea."),
            string("content", "Complete updated Markdown body. May be empty to clear the body.", false),
            string("title", "New title, without .md. Omit to keep the existing title.", false)
        ) { args ->
            require(args.values.containsKey("content") || args.string("title") != null) { "Give content or title to update." }
            val content = if (args.values.containsKey("content"))
                requireNotNull(args.values["content"] as? String) { "Content must be a Markdown string." } else null
            val idea = requireNotNull(ideaBoard).updateIdeaNote(requireNotNull(ideaPaperId),
                args.requireString("id"), args.string("title"), content)
            ideaSaved(idea, "Updated")
        }
    )

    private suspend fun boardIdeas() = requireNotNull(ideaBoard).awaitIdeaNotes(requireNotNull(ideaPaperId))

    private fun ideaSaved(idea: Idea, verb: String): String {
        onAction(PaperAgentAction("$verb ${idea.title}.md", state.pageIndex, ideaId = idea.id))
        return "$verb Idea Board Markdown note id=${idea.id} title=\"${idea.title}\". Saved successfully."
    }

    private fun tool(name: String, description: String, vararg parameters: AgentToolParameter, run: suspend (AgentToolArguments) -> String) =
        AgentTool(name, description, parameters.toList()) { args ->
            try {
                withContext(ui) { run(args) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                "Error: ${exception.message ?: exception::class.simpleName}"
            }
        }

    private fun string(name: String, description: String, required: Boolean = true) = AgentToolParameter(name, description, STRING, required)
    private fun int(name: String, description: String, required: Boolean = true) = AgentToolParameter(name, description, INTEGER, required)
    private fun number(name: String, description: String) = AgentToolParameter(name, description, NUMBER, false)
    private fun color() = AgentToolParameter("color", "Color name (${agentColors.keys.joinToString()}) or #rrggbb. Defaults to the reader's current color.", STRING, false)

    private suspend fun markdownPages(): List<String>? = workspace.awaitMarkdown().pages.takeIf { it.isNotEmpty() }

    private fun missingMarkdown() = if (state.markdown.missingApiKey) "The Markdown version is unavailable: add a Mistral API key in Settings."
    else "The Markdown version is unavailable${state.markdown.error?.let { ": $it" } ?: "."}"

    private fun page(args: AgentToolArguments): Int {
        val page = args.requireInt("page")
        require(page in 1..state.pageCount) { "Page must be between 1 and ${state.pageCount}." }
        return page - 1
    }

    private fun geometry(pageIndex: Int) = state.pageGeometries[pageIndex] ?: error("Page ${pageIndex + 1} is not loaded.")

    /** Finds a passage on the requested page, then nearby pages, since OCR page numbers can drift by one. */
    private suspend fun locate(pageNumber: Int, text: String): PdfTextMatch {
        require(pageNumber in 1..state.pageCount) { "Page must be between 1 and ${state.pageCount}." }
        val order = listOf(0, 1, -1, 2, -2).map { pageNumber - 1 + it }.filter { it in 0 until state.pageCount } +
            (0 until state.pageCount).filter { kotlin.math.abs(it - pageNumber + 1) > 2 }
        for (index in order) workspace.pageText(index)?.find(text, limit = 1)?.firstOrNull()?.let { return it }
        val hasText = workspace.pageText(pageNumber - 1)?.glyphs?.isNotEmpty() == true
        error(if (hasText) "Could not find \"${excerpt(text, 80)}\" in the PDF text. Use a shorter verbatim phrase without math or table content."
        else "Page $pageNumber has no selectable text layer, so text cannot be anchored there. Use x/y positions instead.")
    }

    /** Resolves either an anchor phrase or fractional x/y(/width/height) to a display-space rect. */
    private suspend fun place(args: AgentToolArguments, defaultWidth: Float, defaultHeight: Float, fromAnchor: (Rect, PdfGeometry) -> Rect): Pair<Int, Rect> {
        args.string("anchor_text")?.takeIf { it.isNotBlank() }?.let { anchor ->
            val match = locate(args.requireInt("page"), anchor)
            val bounds = match.selection.rects.reduce { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }
            return match.pageIndex to fromAnchor(bounds, geometry(match.pageIndex))
        }
        val pageIndex = page(args)
        workspace.pageText(pageIndex)
        val g = geometry(pageIndex)
        val x = args.float("x")
        val y = args.float("y")
        require(x != null && y != null) { "Give anchor_text, or x and y as fractions of the page." }
        val left = x.coerceIn(0f, 1f) * g.displayWidth
        val top = y.coerceIn(0f, 1f) * g.displayHeight
        val width = args.float("width")?.let { it.coerceIn(0.005f, 1f) * g.displayWidth } ?: defaultWidth
        val height = args.float("height")?.let { it.coerceIn(0.005f, 1f) * g.displayHeight } ?: defaultHeight
        require(defaultWidth > 0f || args.float("width") != null && args.float("height") != null) { "Give width and height as fractions of the page." }
        return pageIndex to Rect(left, top, left + width, top + height).clampTo(g)
    }

    private suspend fun ink(pageIndex: Int, strokes: List<List<Offset>>, color: String, width: Float?, comment: String, label: String): String {
        val g = geometry(pageIndex)
        val paths = strokes.filter { it.size >= 2 }.map { stroke -> stroke.map(g::toPdf).flatMap { listOf(it.x, it.y) } }
        require(paths.isNotEmpty()) { "Nothing to draw." }
        val points = paths.flatMap { it.chunked(2) }
        val left = points.minOf { it[0] }
        val bottom = points.minOf { it[1] }
        val rect = listOf(left, bottom, maxOf(points.maxOf { it[0] }, left + 0.01f), maxOf(points.maxOf { it[1] }, bottom + 0.01f))
        return created(AnnotationDraft("ink", pageIndex, listOf(rect), comment = comment, color = color, paths = paths,
            width = (width ?: state.inkWidth).coerceIn(0.5f, 12f)), label)
    }

    private suspend fun created(draft: AnnotationDraft, label: String): String {
        val key = workspace.agentCreateAnnotations(listOf(draft)).first()
        onAction(PaperAgentAction(label, draft.pageIndex, key))
        return "Created ${if (draft.isIdea) "idea" else draft.type} $key on page ${draft.pageIndex + 1}."
    }
}

private fun Rect.clampTo(g: PdfGeometry): Rect {
    val w = minOf(width, g.displayWidth)
    val h = minOf(height, g.displayHeight)
    val l = left.coerceIn(0f, g.displayWidth - w)
    val t = top.coerceIn(0f, g.displayHeight - h)
    return Rect(l, t, l + w, t + h)
}

private fun Offset.clampTo(g: PdfGeometry) = Offset(x.coerceIn(0f, g.displayWidth), y.coerceIn(0f, g.displayHeight))

/** Pen strokes for each shape, in display space, around or next to [bounds]. */
internal fun shapeStrokes(shape: String, bounds: Rect): List<List<Offset>> {
    val b = bounds
    return when (shape) {
        "box" -> b.inflate(3f).let { listOf(listOf(it.topLeft, it.topRight, it.bottomRight, it.bottomLeft, it.topLeft)) }
        "ellipse" -> b.inflate(5f).let { r -> listOf((0..48).map { i ->
            val a = 2 * PI * i / 48
            Offset(r.center.x + r.width / 2f * cos(a).toFloat(), r.center.y + r.height / 2f * sin(a).toFloat())
        }) }
        "underline" -> listOf(listOf(Offset(b.left, b.bottom + 2f), Offset(b.right, b.bottom + 2f)))
        "arrow" -> {
            val tip = Offset(b.left - 3f, b.top + minOf(b.height, 12f) / 2f)
            listOf(listOf(Offset(tip.x - 36f, tip.y), tip), listOf(Offset(tip.x - 7f, tip.y - 5f), tip, Offset(tip.x - 7f, tip.y + 5f)))
        }
        "bracket" -> { val x = b.left - 8f; listOf(listOf(Offset(x + 4f, b.top), Offset(x, b.top), Offset(x, b.bottom), Offset(x + 4f, b.bottom))) }
        "checkmark" -> { val x = b.left - 18f; val y = b.top + minOf(b.height, 12f) / 2f
            listOf(listOf(Offset(x, y), Offset(x + 4f, y + 5f), Offset(x + 12f, y - 6f))) }
        "cross" -> { val x = b.left - 16f; val y = b.top + minOf(b.height, 12f) / 2f
            listOf(listOf(Offset(x - 5f, y - 5f), Offset(x + 5f, y + 5f)), listOf(Offset(x + 5f, y - 5f), Offset(x - 5f, y + 5f))) }
        else -> throw IllegalArgumentException("Unknown shape '$shape'.")
    }
}
