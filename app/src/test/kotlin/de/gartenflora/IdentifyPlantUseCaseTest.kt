package de.gartenflora

import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.domain.model.PlantCandidate
import de.gartenflora.domain.usecase.IdentifyPlantUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IdentifyPlantUseCaseTest {

    private lateinit var repository: PlantRepository
    private lateinit var useCase: IdentifyPlantUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = IdentifyPlantUseCase(repository)
    }

    @Test
    fun `invoke with valid inputs delegates to repository`() = runTest {
        val imagePaths = listOf("/path/to/image.jpg")
        val organs = listOf("leaf")
        val candidates = listOf(
            PlantCandidate(
                scientificName = "Rosa canina",
                commonNameDe = "Hundsrose",
                family = "Rosaceae",
                genus = "Rosa",
                gbifId = "3004014",
                score = 0.95f
            )
        )

        coEvery { repository.identifyPlant(imagePaths, organs, "all") } returns
            Result.success(candidates)

        val result = useCase(imagePaths, organs, "all")

        assertTrue(result.isSuccess)
        assertEquals(candidates, result.getOrNull())
        coVerify(exactly = 1) { repository.identifyPlant(imagePaths, organs, "all") }
    }

    @Test
    fun `invoke with empty image paths returns failure`() = runTest {
        val result = useCase(emptyList(), emptyList(), "all")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.identifyPlant(any(), any(), any()) }
    }

    @Test
    fun `invoke with mismatched organs and images returns failure`() = runTest {
        val imagePaths = listOf("/path/to/image1.jpg", "/path/to/image2.jpg")
        val organs = listOf("leaf") // only one organ for two images

        val result = useCase(imagePaths, organs, "all")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.identifyPlant(any(), any(), any()) }
    }

    @Test
    fun `invoke maps DTO to domain model correctly`() = runTest {
        val imagePaths = listOf("/path/image.jpg")
        val organs = listOf("flower")
        val expectedCandidate = PlantCandidate(
            scientificName = "Viola tricolor",
            commonNameDe = "Wildes Stiefmütterchen",
            family = "Violaceae",
            genus = "Viola",
            gbifId = "5666040",
            score = 0.87f
        )

        coEvery { repository.identifyPlant(imagePaths, organs, "weurope") } returns
            Result.success(listOf(expectedCandidate))

        val result = useCase(imagePaths, organs, "weurope")

        assertTrue(result.isSuccess)
        val candidates = result.getOrNull()!!
        assertEquals(1, candidates.size)
        assertEquals("Viola tricolor", candidates[0].scientificName)
        assertEquals("Wildes Stiefmütterchen", candidates[0].commonNameDe)
        assertEquals("Violaceae", candidates[0].family)
        assertEquals(0.87f, candidates[0].score)
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val imagePaths = listOf("/path/image.jpg")
        val organs = listOf("auto")
        val exception = Exception("Netzwerkfehler")

        coEvery { repository.identifyPlant(imagePaths, organs, "all") } returns
            Result.failure(exception)

        val result = useCase(imagePaths, organs, "all")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke with multiple images and organs succeeds`() = runTest {
        val imagePaths = listOf("/path/img1.jpg", "/path/img2.jpg", "/path/img3.jpg")
        val organs = listOf("leaf", "flower", "fruit")

        coEvery { repository.identifyPlant(imagePaths, organs, "all") } returns
            Result.success(emptyList())

        val result = useCase(imagePaths, organs, "all")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.identifyPlant(imagePaths, organs, "all") }
    }
}
