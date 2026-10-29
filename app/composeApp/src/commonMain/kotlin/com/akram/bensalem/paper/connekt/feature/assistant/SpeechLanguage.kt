package com.akram.bensalem.paper.connekt.feature.assistant

private val latinLanguageMarkers = mapOf(
    "en-US" to setOf("the", "and", "of", "to", "in", "is", "that", "for", "with", "on", "are", "was", "this", "from", "by"),
    "fr-FR" to setOf("le", "la", "les", "des", "du", "une", "et", "est", "que", "dans", "pour", "avec", "ce", "cette", "qui", "pas", "sur", "sont", "nous", "vous", "leur", "aux", "mais", "comme", "bonjour", "merci"),
    "es-ES" to setOf("el", "los", "las", "y", "que", "en", "una", "es", "por", "para", "con", "como", "pero", "del", "al", "se", "son", "esta", "este", "también", "hola", "gracias"),
    "de-DE" to setOf("der", "die", "das", "und", "ist", "im", "zu", "mit", "von", "für", "ein", "eine", "nicht", "auf", "werden", "sind", "auch", "hallo", "danke"),
    "it-IT" to setOf("il", "lo", "gli", "e", "che", "di", "del", "una", "per", "con", "sono", "non", "come", "questo", "questa", "ciao", "grazie"),
    "pt-BR" to setOf("o", "os", "as", "e", "que", "do", "da", "em", "um", "uma", "para", "com", "não", "por", "como", "olá", "obrigado"),
    "nl-NL" to setOf("de", "het", "een", "en", "van", "is", "in", "voor", "met", "dat", "niet", "op", "zijn", "hallo", "bedankt"),
    "ro-RO" to setOf("și", "în", "de", "la", "un", "o", "este", "cu", "pentru", "care", "nu", "salut", "mulțumesc"),
    "pl-PL" to setOf("i", "w", "z", "na", "że", "jest", "się", "dla", "jako", "ale", "nie", "to", "są", "przez", "oraz", "cześć", "dziękuję"),
    "tr-TR" to setOf("ve", "bir", "bu", "için", "ile", "olan", "olarak", "değil", "çok", "daha", "var", "merhaba", "teşekkür"),
    "sv-SE" to setOf("och", "att", "det", "som", "en", "är", "på", "för", "med", "till", "av", "inte", "har", "från", "hej", "tack"),
    "da-DK" to setOf("og", "at", "det", "en", "er", "på", "for", "med", "til", "af", "ikke", "har", "hej", "tak"),
    "nb-NO" to setOf("og", "at", "det", "en", "er", "på", "for", "med", "til", "av", "ikke", "har", "fra", "hei", "takk"),
    "fi-FI" to setOf("ja", "on", "että", "se", "ei", "kanssa", "tämä", "myös", "mutta", "tai", "kun", "hei", "kiitos"),
    "cs-CZ" to setOf("a", "že", "se", "v", "na", "je", "pro", "jako", "s", "z", "do", "jsou", "není", "také", "ahoj", "děkuji"),
)

/**
 * Picks a Google TTS language from the whole document, rather than each short speech chunk.
 * Script detection handles non-Latin writing systems; common words distinguish Latin-script languages.
 */
internal fun detectSpeechLanguage(text: String): String {
    val letters = text.filter(Char::isLetter)
    if (letters.isEmpty()) return "en-US"

    val scriptCounts = letters.groupingBy(::speechScript).eachCount()
    val kana = scriptCounts["kana"] ?: 0
    if (kana > 0) return "ja-JP"
    if ((scriptCounts["hangul"] ?: 0) > 0) return "ko-KR"

    val nonLatinLanguages = listOf(
        "cyrillic" to "ru-RU",
        "arabic" to "ar-XA",
        "devanagari" to "hi-IN",
        "bengali" to "bn-IN",
        "gurmukhi" to "pa-IN",
        "gujarati" to "gu-IN",
        "tamil" to "ta-IN",
        "telugu" to "te-IN",
        "kannada" to "kn-IN",
        "malayalam" to "ml-IN",
        "thai" to "th-TH",
        "greek" to "el-GR",
        "hebrew" to "he-IL",
        "han" to "zh-CN",
    )
    val latinCount = scriptCounts["latin"] ?: 0
    nonLatinLanguages.firstOrNull { (script, _) ->
        (scriptCounts[script] ?: 0) > 0 &&
            (scriptCounts[script] ?: 0) >= latinCount * 0.25
    }?.let { (script, languageCode) ->
        return if (script == "cyrillic" && letters.any { it in "іїєґІЇЄҐ" }) "uk-UA" else languageCode
    }

    val words = Regex("\\p{L}+").findAll(text.lowercase()).map { it.value }.toList()
    val scores = latinLanguageMarkers.mapValues { (_, markers) -> words.count { it in markers } }
    val accentBoosts = mapOf(
        "fr-FR" to letters.count { it in "éèêëàùûçœÉÈÊËÀÙÛÇŒ" } * 2,
        "es-ES" to letters.count { it in "ñÑ¿¡" } * 2,
        "de-DE" to letters.count { it in "äöüßÄÖÜẞ" } * 2,
        "pt-BR" to letters.count { it in "ãõÃÕ" } * 2,
        "ro-RO" to letters.count { it in "ăâîșțĂÂÎȘȚ" } * 2,
        "pl-PL" to letters.count { it in "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ" } * 2,
        "tr-TR" to letters.count { it in "ğışĞİŞ" } * 2,
        "sv-SE" to letters.count { it in "åäöÅÄÖ" } * 2,
        "da-DK" to letters.count { it in "æøåÆØÅ" } * 2,
        "nb-NO" to letters.count { it in "æøåÆØÅ" } * 2,
        "fi-FI" to letters.count { it in "äöÄÖ" } * 2,
        "cs-CZ" to letters.count { it in "čďěňřšťůžČĎĚŇŘŠŤŮŽ" } * 2,
    )
    val ranked = scores.map { (language, score) ->
        language to score + (accentBoosts[language] ?: 0)
    }.sortedByDescending { it.second }
    val best = ranked.first()
    val secondScore = ranked.getOrNull(1)?.second ?: 0
    return if (best.first != "en-US" && best.second >= 2 && best.second > secondScore) {
        best.first
    } else {
        "en-US"
    }
}

private fun speechScript(char: Char): String = when (char.code) {
    in 0x0041..0x024F, in 0x1E00..0x1EFF -> "latin"
    in 0x0370..0x03FF, in 0x1F00..0x1FFF -> "greek"
    in 0x0400..0x052F -> "cyrillic"
    in 0x0590..0x05FF -> "hebrew"
    in 0x0600..0x06FF, in 0x0750..0x077F, in 0x08A0..0x08FF -> "arabic"
    in 0x0900..0x097F -> "devanagari"
    in 0x0980..0x09FF -> "bengali"
    in 0x0A00..0x0A7F -> "gurmukhi"
    in 0x0A80..0x0AFF -> "gujarati"
    in 0x0B80..0x0BFF -> "tamil"
    in 0x0C00..0x0C7F -> "telugu"
    in 0x0C80..0x0CFF -> "kannada"
    in 0x0D00..0x0D7F -> "malayalam"
    in 0x0E00..0x0E7F -> "thai"
    in 0x3040..0x30FF, in 0x31F0..0x31FF -> "kana"
    in 0xAC00..0xD7AF -> "hangul"
    in 0x3400..0x4DBF, in 0x4E00..0x9FFF -> "han"
    else -> "other"
}
