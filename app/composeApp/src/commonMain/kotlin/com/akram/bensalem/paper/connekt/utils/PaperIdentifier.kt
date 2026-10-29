package com.akram.bensalem.paper.connekt.utils

enum class PaperIdentifierType(val displayName: String) {
    DOI("DOI"),
    ARXIV("arXiv"),
    ISBN("ISBN"),
    PMID("PMID"),
    URL("URL"),
    LOCAL_PDF("Local PDF"),
    ONLINE_PDF("Online PDF"),
    UNKNOWN("Unknown"),
    AUTO("Auto")
}

object PaperIdentifier {
    fun detectType(idRaw: String): PaperIdentifierType {
        val id = idRaw.trim()
        if (id.isEmpty()) return PaperIdentifierType.UNKNOWN

        // URL detection
        if (id.startsWith("http://", ignoreCase = true) || id.startsWith("https://", ignoreCase = true)) {
            if (id.lowercase().endsWith(".pdf") || id.lowercase().contains(".pdf?")) {
                return PaperIdentifierType.ONLINE_PDF
            }
            val doiFromUrl = Regex("doi.org/(10\\.[^#?\\s]+)").find(id)?.groupValues?.getOrNull(1)
            if (doiFromUrl != null) return PaperIdentifierType.DOI
            return PaperIdentifierType.URL
        }

        // Local PDF detection (heuristic: ends with .pdf and looks like a path or just a filename)
        if (id.lowercase().endsWith(".pdf")) {
            return PaperIdentifierType.LOCAL_PDF
        }

        return when {
            id.contains("/10.") || id.startsWith("10.") || id.startsWith("doi:", ignoreCase = true) -> PaperIdentifierType.DOI
            id.lowercase().startsWith("arxiv:") || id.matches(Regex("\\d{4}\\.\\d{4,5}(v\\d+)?")) -> PaperIdentifierType.ARXIV
            id.lowercase().startsWith("pmid:") || (id.matches(Regex("\\d+")) && id.length in 1..8) -> PaperIdentifierType.PMID
            id.replace("-", "").let { clean -> (clean.length == 10 || clean.length == 13) && clean.all { it.isDigit() || it.lowercaseChar() == 'x' } } -> PaperIdentifierType.ISBN
            else -> PaperIdentifierType.UNKNOWN
        }
    }

    fun isValid(idRaw: String, type: PaperIdentifierType = PaperIdentifierType.AUTO): Boolean {
        val detected = detectType(idRaw)
        return if (type == PaperIdentifierType.AUTO) {
            detected != PaperIdentifierType.UNKNOWN
        } else {
            detected == type
        }
    }
}
