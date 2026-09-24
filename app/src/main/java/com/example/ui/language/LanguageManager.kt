package com.example.ui.language

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val displayName: String) {
    HINDI("hi", "हिन्दी"),
    ENGLISH("en", "English")
}

object LanguageManager {
    var currentLanguage by mutableStateOf(AppLanguage.HINDI)

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
    }

    fun isHindi(): Boolean = currentLanguage == AppLanguage.HINDI

    // Bilingual string helper
    fun text(en: String, hi: String): String {
        return if (isHindi()) hi else en
    }
}
