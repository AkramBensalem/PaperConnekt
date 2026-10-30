package com.paperconnekt.secrets

enum class SecretKey {
    OPENAI,
    MISTRAL,
    ZOTERO,
    OPENALEX,
    /** Bearer token MCP clients present to the local PaperConnekt MCP server; generated on this device. */
    MCP_TOKEN,
}
