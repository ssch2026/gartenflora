package de.gartenflora

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.gartenflora.data.local.AppDatabase
import de.gartenflora.data.local.PlantObservationEntity
import de.gartenflora.di.DatabaseModule
import de.gartenflora.di.NetworkModule
import de.gartenflora.di.RepositoryModule
import de.gartenflora.ui.garden.MeinGartenScreen
import de.gartenflora.ui.theme.GartenFloraTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MeinGartenScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun emptyState_shows_correct_message() {
        // Navigate to MeinGarten tab
        composeRule.onNodeWithText("Mein Garten").performClick()
        composeRule.waitForIdle()

        // Verify empty state is shown
        composeRule.onNodeWithTag("empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("Noch keine Pflanzen gespeichert").assertIsDisplayed()
    }

    @Test
    fun list_shows_saved_items() {
        // Insert a test observation
        runBlocking {
            database.plantObservationDao().insert(
                PlantObservationEntity(
                    scientificName = "Rosa canina",
                    commonNameDe = "Hundsrose",
                    confidence = 0.95f,
                    photoPaths = "[]",
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        composeRule.onNodeWithText("Mein Garten").performClick()
        composeRule.waitForIdle()

        // Grid should be visible with the plant
        composeRule.onNodeWithTag("observations_grid").assertIsDisplayed()
        composeRule.onNodeWithText("Hundsrose").assertIsDisplayed()
    }

    @Test
    fun search_bar_is_displayed() {
        composeRule.onNodeWithText("Mein Garten").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_bar").assertIsDisplayed()
    }
}
