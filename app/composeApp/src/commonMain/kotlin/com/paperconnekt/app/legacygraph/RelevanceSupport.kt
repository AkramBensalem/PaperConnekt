package com.paperconnekt.app.legacygraph

enum class RelevanceField(val label: String, val placeholder: String, val suggestions: List<String> = emptyList()) {
    DOMAIN("Domain", "e.g. computer vision, oncology"),
    CONTEXT("Context", "e.g. smart cities"),
    KEYWORDS("Keywords", "e.g. federated learning"),
    METHODS("Methods", "e.g. deep learning"),
    STUDY_TYPE("Study type", "e.g. empirical"),
    DATA("Population / data", "e.g. children, satellite images"),
    OUTCOME("Outcome", "e.g. accuracy, mortality"),
    LANGUAGE("Language", "e.g. English"),
    OTHER("Other", "Any other requirement"),
}

enum class RelevanceConnector(val label: String) { AND("AND"), OR("OR"), AND_NOT("AND NOT") }

data class RelevanceCriterion(
    val id: String,
    val field: RelevanceField = RelevanceField.KEYWORDS,
    val value: String = "",
    val connector: RelevanceConnector = RelevanceConnector.AND,
)

data class RelevanceProfile(
    val goal: String = "",
    val criteria: List<RelevanceCriterion> = emptyList(),
) {
    val activeCriteria get() = criteria.filter { it.value.isNotBlank() }
    val isEmpty get() = goal.isBlank() && activeCriteria.isEmpty()
    fun expression(): String = activeCriteria.mapIndexed { index, criterion ->
        val term = "${criterion.field.label}: \"${criterion.value.trim()}\""
        if (index == 0 || criterion.connector == RelevanceConnector.OR) term
        else if (criterion.connector == RelevanceConnector.AND_NOT) "NOT $term" else term
    }.joinToString(if (activeCriteria.any { it.connector == RelevanceConnector.OR }) " OR " else " AND ")
    fun fingerprint(): String = toPrompt().lowercase().replace(Regex("\\s+"), " ").trim()
    fun toPrompt(): String = buildString {
        goal.trim().takeIf { it.isNotEmpty() }?.let { append("Research goal: ").appendLine(it) }
        expression().takeIf { it.isNotEmpty() }?.let { appendLine(it) }
    }.trim()
}

data class RelevanceAiState(
    val isAvailable: Boolean = false,
    val profile: RelevanceProfile = RelevanceProfile(),
    val scores: Map<String, RelevanceScore> = emptyMap(),
    val isEvaluating: Boolean = false,
    val evaluated: Int = 0,
    val total: Int = 0,
    val failed: Int = 0,
    val error: String? = null,
) {
    fun pendingIds(paperIds: Collection<String>): List<String> = paperIds.distinct().filter { it !in scores }
}
