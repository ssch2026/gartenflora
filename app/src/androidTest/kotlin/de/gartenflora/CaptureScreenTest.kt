package de.gartenflora

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
class CaptureScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        // App starts on CaptureScreen by default
        composeRule.waitForIdle()
    }

    @Test
    fun identify_button_disabled_with_no_images() {
        // The camera permission dialog may appear first — handle it
        // Identify button should be disabled when no photos are captured
        composeRule
            .onNodeWithText("Identifizieren")
            .assertIsNotEnabled()
    }

    @Test
    fun capture_screen_shows_title() {
        composeRule.onNodeWithText("Pflanze fotografieren").assertExists()
    }

    @Test
    fun capture_hint_shown_when_no_photos() {
        // Hint text should be visible when no photos captured yet
        composeRule.onNodeWithText("Mindestens 1 Foto für Identifikation erforderlich")
            .assertExists()
    }
}
