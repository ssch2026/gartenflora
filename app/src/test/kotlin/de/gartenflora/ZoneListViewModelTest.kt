package de.gartenflora

import app.cash.turbine.test
import de.gartenflora.data.repository.GardenRepository
import de.gartenflora.domain.model.GardenZone
import de.gartenflora.ui.gardenplan.ZoneListViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZoneListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var gardenRepository: GardenRepository
    private lateinit var viewModel: ZoneListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        gardenRepository = mockk(relaxed = true)
        every { gardenRepository.observeZones() } returns flowOf(emptyList())
        viewModel = ZoneListViewModel(gardenRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has no dialog open`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreateDialog)
            assertNull(state.editZone)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openCreateDialog sets showCreateDialog true with blank defaults`() = runTest {
        viewModel.openCreateDialog()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showCreateDialog)
            assertNull(state.editZone)
            assertEquals("", state.dialogName)
            assertEquals("10", state.dialogCols)
            assertEquals("8", state.dialogRows)
            assertEquals("50", state.dialogCellSize)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openEditDialog pre-fills dialog with zone values`() = runTest {
        val zone = GardenZone(id = 1L, name = "Hochbeet Nord", gridCols = 12, gridRows = 6, cellSizeCm = 40)

        viewModel.openEditDialog(zone)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showCreateDialog)
            assertNotNull(state.editZone)
            assertEquals("Hochbeet Nord", state.dialogName)
            assertEquals("12", state.dialogCols)
            assertEquals("6", state.dialogRows)
            assertEquals("40", state.dialogCellSize)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissDialog closes dialog and clears edit zone`() = runTest {
        viewModel.openCreateDialog()
        viewModel.dismissDialog()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreateDialog)
            assertNull(state.editZone)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDialogNameChange updates dialog name`() = runTest {
        viewModel.onDialogNameChange("Terrasse")

        viewModel.uiState.test {
            assertEquals("Terrasse", awaitItem().dialogName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirmDialog with blank name does nothing`() = runTest {
        viewModel.openCreateDialog()
        viewModel.onDialogNameChange("   ")
        viewModel.confirmDialog()

        // Dialog should remain open
        viewModel.uiState.test {
            assertTrue(awaitItem().showCreateDialog)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { gardenRepository.saveZone(any()) }
    }

    @Test
    fun `confirmDialog with valid name creates zone and closes dialog`() = runTest {
        viewModel.openCreateDialog()
        viewModel.onDialogNameChange("Hochbeet Nord")
        viewModel.onDialogColsChange("8")
        viewModel.onDialogRowsChange("5")
        viewModel.onDialogCellSizeChange("60")

        viewModel.confirmDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            gardenRepository.saveZone(
                match {
                    it.name == "Hochbeet Nord" &&
                    it.gridCols == 8 &&
                    it.gridRows == 5 &&
                    it.cellSizeCm == 60
                }
            )
        }
        viewModel.uiState.test {
            assertFalse(awaitItem().showCreateDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirmDialog in edit mode updates zone`() = runTest {
        val existing = GardenZone(id = 5L, name = "Alt", gridCols = 10, gridRows = 8, cellSizeCm = 50)
        viewModel.openEditDialog(existing)
        viewModel.onDialogNameChange("Neu")

        viewModel.confirmDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            gardenRepository.updateZone(match { it.id == 5L && it.name == "Neu" })
        }
        coVerify(exactly = 0) { gardenRepository.saveZone(any()) }
    }

    @Test
    fun `confirmDialog clamps cols and rows to valid range`() = runTest {
        viewModel.openCreateDialog()
        viewModel.onDialogNameChange("Test")
        viewModel.onDialogColsChange("999") // over max
        viewModel.onDialogRowsChange("1")   // under min

        viewModel.confirmDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            gardenRepository.saveZone(
                match { it.gridCols == 30 && it.gridRows == 2 }
            )
        }
    }

    @Test
    fun `deleteZone calls repository deleteZone`() = runTest {
        val zone = GardenZone(id = 3L, name = "Südwand", gridCols = 10, gridRows = 4, cellSizeCm = 50)

        viewModel.deleteZone(zone)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { gardenRepository.deleteZone(3L) }
    }

    @Test
    fun `zones flow reflects repository`() = runTest {
        val zones = listOf(
            GardenZone(id = 1L, name = "A", gridCols = 5, gridRows = 5, cellSizeCm = 50),
            GardenZone(id = 2L, name = "B", gridCols = 8, gridRows = 3, cellSizeCm = 30)
        )
        every { gardenRepository.observeZones() } returns flowOf(zones)
        viewModel = ZoneListViewModel(gardenRepository) // re-create with new stub

        viewModel.zones.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("A", result[0].name)
            assertEquals("B", result[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
