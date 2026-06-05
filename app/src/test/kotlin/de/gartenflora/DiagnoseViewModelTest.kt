package de.gartenflora

import app.cash.turbine.test
import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.domain.model.DiagnoseResult
import de.gartenflora.domain.model.DiseaseItem
import de.gartenflora.domain.model.PlantObservation
import de.gartenflora.domain.model.TreatmentInfo
import de.gartenflora.domain.usecase.GetObservationsUseCase
import de.gartenflora.ui.diagnose.DiagnoseViewModel
import io.mockk.coEvery
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

class DiagnoseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var plantRepository: PlantRepository
    private lateinit var getObservationsUseCase: GetObservationsUseCase
    private lateinit var viewModel: DiagnoseViewModel

    private val testObservation = PlantObservation(
        id = 1L,
        scientificName = "Rosa canina",
        commonNameDe = "Hundsrose",
        confidence = 0.95f,
        photoPaths = listOf("/path/photo1.jpg", "/path/photo2.jpg"),
        latitude = 48.1,
        longitude = 11.6
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        plantRepository = mockk()
        getObservationsUseCase = mockk()
        every { getObservationsUseCase.observeById(1L) } returns flowOf(testObservation)
        viewModel = DiagnoseViewModel(plantRepository, getObservationsUseCase)
        viewModel.init(1L)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is idle`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.result)
            assertNull(state.error)
            assertEquals(0, state.selectedPhotoIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectPhoto updates selectedPhotoIndex and clears result`() = runTest {
        // First set a result
        val diagnoseResult = buildHealthyResult()
        coEvery {
            plantRepository.diagnoseHealth("/path/photo1.jpg", 48.1, 11.6)
        } returns Result.success(diagnoseResult)
        viewModel.analyze()
        testDispatcher.scheduler.advanceUntilIdle()

        // Now select different photo
        viewModel.selectPhoto(1)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.selectedPhotoIndex)
            assertNull(state.result) // result cleared on photo change
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyze sets loading state then shows result`() = runTest {
        val diagnoseResult = buildHealthyResult()
        coEvery {
            plantRepository.diagnoseHealth("/path/photo1.jpg", 48.1, 11.6)
        } returns Result.success(diagnoseResult)

        viewModel.uiState.test {
            awaitItem() // initial idle

            viewModel.analyze()
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertNull(loadingState.result)

            testDispatcher.scheduler.advanceUntilIdle()
            val resultState = awaitItem()
            assertFalse(resultState.isLoading)
            assertNotNull(resultState.result)
            assertTrue(resultState.result!!.isHealthy)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyze with diseases shows disease list`() = runTest {
        val diseaseResult = DiagnoseResult(
            isHealthy = false,
            isHealthyProbability = 0.21f,
            diseases = listOf(
                DiseaseItem(
                    name = "Rust disease",
                    probability = 0.78f,
                    description = "Fungal infection",
                    treatment = TreatmentInfo(
                        biological = listOf("Remove infected leaves"),
                        chemical = listOf("Fungicide"),
                        prevention = listOf("Good air circulation")
                    ),
                    similarImageUrls = listOf("https://plant.id/img/rust.jpg"),
                    classification = listOf("Fungal", "Disease")
                )
            )
        )
        coEvery {
            plantRepository.diagnoseHealth(any(), any(), any())
        } returns Result.success(diseaseResult)

        viewModel.analyze()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.result!!.isHealthy)
            assertEquals(1, state.result!!.diseases.size)
            assertEquals("Rust disease", state.result!!.diseases[0].name)
            assertEquals(0.78f, state.result!!.diseases[0].probability)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyze failure sets error state`() = runTest {
        coEvery {
            plantRepository.diagnoseHealth(any(), any(), any())
        } returns Result.failure(Exception("Network error"))

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.analyze()
            awaitItem() // loading

            testDispatcher.scheduler.advanceUntilIdle()
            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNull(errorState.result)
            assertEquals("Network error", errorState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError resets error to null`() = runTest {
        coEvery { plantRepository.diagnoseHealth(any(), any(), any()) } returns
            Result.failure(Exception("Some error"))

        viewModel.analyze()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        viewModel.uiState.test {
            assertNull(awaitItem().error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyze uses selected photo index`() = runTest {
        viewModel.selectPhoto(1) // select second photo
        coEvery {
            plantRepository.diagnoseHealth("/path/photo2.jpg", 48.1, 11.6)
        } returns Result.success(buildHealthyResult())

        viewModel.analyze()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { plantRepository.diagnoseHealth("/path/photo2.jpg", 48.1, 11.6) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildHealthyResult() = DiagnoseResult(
        isHealthy = true,
        isHealthyProbability = 0.92f,
        diseases = emptyList()
    )
}
