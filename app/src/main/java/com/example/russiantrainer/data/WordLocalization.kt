package com.example.russiantrainer.data

internal object WordLocalization {
    private const val LIST_DELIMITER = " | "
    private val ONCE_SUFFIX_REGEX = Regex("\\s+once\\b", RegexOption.IGNORE_CASE)

    fun decodeList(value: String): List<String> {
        return value.split(LIST_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun encodeList(items: List<String>): String {
        return items.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(LIST_DELIMITER)
    }

    fun translationsForLanguage(word: WordEntity, languageTag: String): List<String> {
        val localized = when (languageTag.lowercase()) {
            "fr" -> decodeList(word.translationsFrData)
            "es" -> decodeList(word.translationsEsData)
            "pt" -> decodeList(word.translationsPtData)
            "ar" -> decodeList(word.translationsArData)
            "zh" -> decodeList(word.translationsZhData)
            else -> decodeList(word.translationsData)
        }
        return localized.ifEmpty {
            decodeList(word.translationsData).ifEmpty { listOf(word.english) }
        }
    }

    fun primaryTranslationForLanguage(word: WordEntity, languageTag: String): String {
        return translationsForLanguage(word, languageTag).firstOrNull() ?: word.english
    }

    fun normalizeEnglishText(value: String): String {
        return value.trim()
            .replace(ONCE_SUFFIX_REGEX, "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    fun fallbackTranscription(word: String): String {
        val transliteration = buildString {
            word.forEach { char ->
                append(
                    when (char.lowercaseChar()) {
                        'а' -> "a"
                        'б' -> "b"
                        'в' -> "v"
                        'г' -> "g"
                        'д' -> "d"
                        'е' -> "ye"
                        'ё' -> "yo"
                        'ж' -> "zh"
                        'з' -> "z"
                        'и' -> "i"
                        'й' -> "y"
                        'к' -> "k"
                        'л' -> "l"
                        'м' -> "m"
                        'н' -> "n"
                        'о' -> "o"
                        'п' -> "p"
                        'р' -> "r"
                        'с' -> "s"
                        'т' -> "t"
                        'у' -> "u"
                        'ф' -> "f"
                        'х' -> "kh"
                        'ц' -> "ts"
                        'ч' -> "ch"
                        'ш' -> "sh"
                        'щ' -> "shch"
                        'ъ' -> ""
                        'ы' -> "y"
                        'ь' -> ""
                        'э' -> "e"
                        'ю' -> "yu"
                        'я' -> "ya"
                        else -> char.toString()
                    }
                )
            }
        }
        return "/$transliteration/"
    }
}
