package com.akram.bensalem.paper.connekt.shared.serverapi

/**
 * HTTP contract between the web (wasm) client and the PaperConnekt server.
 *
 * Every route is served by the same store/agent implementations that the desktop app runs locally,
 * so the browser only ever talks to these endpoints for persistence and AI work.
 */
object ServerRoutes {
    const val API = "/api"
    const val HEALTH = "$API/health"
    const val CLIENT_CONFIG = "$API/config"

    const val SETTINGS_API_KEYS = "$API/settings/api-keys"
    const val SETTINGS_SYNC_STATE = "$API/settings/sync-state"
    const val SETTINGS_SYNC_STATE_KEYS = "$API/settings/sync-state/keys"
    const val SETTINGS_RELEVANCE = "$API/settings/relevance"
    const val SETTINGS_RELEVANCE_PROFILE = "$API/settings/relevance/profile"
    const val SETTINGS_RELEVANCE_SCORES = "$API/settings/relevance/scores"
    const val SETTINGS_GRAPH_DISPLAY = "$API/settings/graph-display"

    const val ASSISTANT_IDEAS = "$API/assistant/ideas"
    const val ASSISTANT_HISTORY = "$API/assistant/research-history"
    const val ASSISTANT_CHATS = "$API/assistant/agent-chats"
    const val ASSISTANT_MARKDOWN = "$API/assistant/markdown"
    const val ASSISTANT_MARKDOWN_DOCUMENT = "$API/assistant/markdown/document"
    const val ASSISTANT_MARKDOWN_FIGURE = "$API/assistant/markdown/figure"
    const val ASSISTANT_MARKDOWN_LAYOUT = "$API/assistant/markdown/layout"
    const val ASSISTANT_MARKDOWN_PENDING = "$API/assistant/markdown/pending"
    const val ASSISTANT_MARKDOWN_READING = "$API/assistant/markdown/reading"

    const val GRAPH_SESSIONS = "$API/graph/sessions"
    const val GRAPH_RECENT_QUERIES = "$API/graph/recent-queries"

    const val PDF_DOCUMENTS = "$API/pdf/documents"
    const val ZOTERO_FILE = "$API/zotero/file"
    fun pdfDocument(id: String) = "$PDF_DOCUMENTS/$id"
    fun pdfPage(id: String, pageIndex: Int) = "$PDF_DOCUMENTS/$id/pages/$pageIndex"

    /** WebSocket endpoint running the research agent; see [AgentWireMessage]. */
    const val AGENT = "$API/agent"
}

/** Query parameter names shared by client and server. */
object ServerParams {
    const val KEY = "key"
    const val ID = "id"
    const val PREFIX = "prefix"
    const val QUERY = "query"
    const val SCALE = "scale"
    const val FINGERPRINT = "fingerprint"
}
