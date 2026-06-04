package de.gartenflora

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        // Navigate to settings tab
        composeRule.onNodeWithText("Einstellungen").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun settings_title_is_displayed() {
        composeRule.onNodeWithText("Einstellungen").assertIsDisplayed()
    }

    @Test
    fun gemini_toggle_hidden_when_key_not_present() {
        // When GEMINI_API_KEY is empty (test build), the toggle should not be visible
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            composeRule.onNodeWithTag("gemini_toggle").assertDoesNotExist()
        }
    }

    @Test
    fun project_selector_is_displayed() {
        // Project selector card should always be visible
        composeRule.onNodeWithText("Pl@ntNet-Projekt").assertIsDisplayed()
    }

    @Test
    fun daily_usage_hint_is_displayed() {
        composeRule.onNodeWithText("Tagesnutzung").assertIsDisplayed()
        composeRule.onNodeWithText("Pl@ntNet kostenlos: 500 Anfragen/Tag").assertIsDisplayed()
    }

    @Test
    fun gemini_toggle_visible_when_key_present() {
        // This test only runs if GEMINI_API_KEY is configured
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            composeRule.onNodeWithTag("gemini_toggle").assertIsDisplayed()
        }
    }
}
