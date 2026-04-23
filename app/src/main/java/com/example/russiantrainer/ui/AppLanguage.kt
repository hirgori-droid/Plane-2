package com.example.russiantrainer.ui

enum class AppLanguage(val tag: String) {
    RU("ru"),
    EN("en"),
    FR("fr"),
    ES("es"),
    PT("pt"),
    AR("ar"),
    ZH("zh");

    val nativeLabel: String
        get() = when (this) {
            RU -> "Русский"
            EN -> "English"
            FR -> "Français"
            ES -> "Español"
            PT -> "Português"
            AR -> "العربية"
            ZH -> "中文"
        }

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.tag == tag } ?: RU
        }
    }
}
