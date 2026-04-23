package com.example.russiantrainer.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.russiantrainer.MainActivity
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopBarMenusTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString("language", AppLanguage.EN.tag)
            .putString("theme", "blossom")
            .apply()
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
    }

    @Test
    fun languageMenu_showsAllSupportedLanguages_andSelectionIsSaved() {
        launchActivity()

        composeRule.onNodeWithText("English").performClick()
        composeRule.onNodeWithText("Русский").assertIsDisplayed()
        composeRule.onNodeWithText("Français").assertIsDisplayed()
        composeRule.onNodeWithText("Español").assertIsDisplayed()
        composeRule.onNodeWithText("Português").assertIsDisplayed()
        composeRule.onNodeWithText("العربية").assertIsDisplayed()
        composeRule.onNodeWithText("中文").assertIsDisplayed()
        composeRule.onNodeWithText("Français").performClick()

        val savedLanguage = context
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("language", null)

        composeRule.waitForIdle()
        assertEquals(AppLanguage.FR.tag, savedLanguage)
    }

    @Test
    fun settingsMenu_showsThemes_andSelectionIsSaved() {
        launchActivity()

        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Blossom", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Grove", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Midnight", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Midnight", substring = true).performClick()

        val savedTheme = context
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("theme", null)

        composeRule.waitForIdle()
        assertEquals("midnight", savedTheme)
    }

    private fun launchActivity() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForIdle()
    }
}
