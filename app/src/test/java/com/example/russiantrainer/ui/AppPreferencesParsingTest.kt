package com.example.russiantrainer.ui

import com.example.russiantrainer.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class AppPreferencesParsingTest {
    @Test
    fun appLanguage_fromTag_returnsExpectedLanguage() {
        assertEquals(AppLanguage.FR, AppLanguage.fromTag("fr"))
        assertEquals(AppLanguage.AR, AppLanguage.fromTag("ar"))
        assertEquals(AppLanguage.ZH, AppLanguage.fromTag("zh"))
    }

    @Test
    fun appLanguage_fromTag_fallsBackToRussian() {
        assertEquals(AppLanguage.RU, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.RU, AppLanguage.fromTag("de"))
    }

    @Test
    fun appTheme_fromKey_returnsExpectedTheme() {
        assertEquals(AppTheme.GROVE, AppTheme.fromKey("grove"))
        assertEquals(AppTheme.MIDNIGHT, AppTheme.fromKey("midnight"))
    }

    @Test
    fun appTheme_fromKey_fallsBackToBlossom() {
        assertEquals(AppTheme.BLOSSOM, AppTheme.fromKey(null))
        assertEquals(AppTheme.BLOSSOM, AppTheme.fromKey("unknown"))
    }
}
