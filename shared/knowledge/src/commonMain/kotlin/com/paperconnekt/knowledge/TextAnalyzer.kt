package com.paperconnekt.knowledge

/** Deterministic, language-light tokenizer for lexical retrieval: lowercases, drops stop words and folds plurals. */
object TextAnalyzer {
    private val stopWords = setOf(
        "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can", "do", "does", "for", "from", "has", "have",
        "how", "if", "in", "into", "is", "it", "its", "of", "on", "or", "our", "so", "such", "than", "that", "the", "their",
        "then", "there", "these", "they", "this", "those", "to", "was", "we", "were", "what", "when", "where", "which",
        "while", "who", "why", "will", "with", "would", "you", "your", "also", "about", "paper", "et", "al",
    )

    fun terms(text: String): List<String> {
        val terms = ArrayList<String>()
        val token = StringBuilder()
        fun flush() {
            if (token.isEmpty()) return
            val word = token.toString()
            token.clear()
            if (word in stopWords) return
            if (word.length == 1 && !word[0].isDigit()) return
            terms += stem(word)
        }
        for (char in text) {
            if (char.isLetterOrDigit()) token.append(char.lowercaseChar()) else flush()
        }
        flush()
        return terms
    }

    /** Folds common English plural endings so "losses"/"loss" and "values"/"value" match; deliberately conservative. */
    internal fun stem(word: String): String = when {
        word.length <= 3 || word.any(Char::isDigit) -> word
        word.endsWith("ies") && word.length > 4 -> word.dropLast(3) + "y"
        word.endsWith("sses") -> word.dropLast(2)
        word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us") && !word.endsWith("is") -> word.dropLast(1)
        else -> word
    }

    fun normalizePhrase(text: String): String = text.lowercase().split(whitespace).filter(String::isNotEmpty).joinToString(" ")

    private val whitespace = Regex("\\s+")
}
