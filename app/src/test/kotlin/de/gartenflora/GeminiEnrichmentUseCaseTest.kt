package de.gartenflora

import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.domain.usecase.EnrichWithGeminiUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiEnrichmentUseCaseTest {

    private lateinit var repository: PlantRepository
    private lateinit var useCase: EnrichWithGeminiUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = EnrichWithGeminiUseCase(repository)
    }

    @Test
    fun `invoke returns care notes on success`() = runTest {
        val scientificName = "Rosa canina"
        val careNotes = """
            Licht: Volle Sonne bis Halbschatten, mindestens 6 Stunden direkte Sonne täglich.
            Wasser: Mäßig, Staunässe vermeiden, einmal wöchentlich gründlich wässern.
            Boden: Durchlässiger, lehmiger Boden, pH 6.0-7.0.
            Winterhärte: Zone 7b, winterhart ohne Schutz, verträgt bis -15°C.
        """.trimIndent()

        coEvery { repository.generateCareNotes(scientificName) } returns
            Result.success(careNotes)

        val result = useCase(scientificName)

        assertTrue(result.isSuccess)
        assertEquals(careNotes, result.getOrNull())
        coVerify(exactly = 1) { repository.generateCareNotes(scientificName) }
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        val scientificName = "Quercus robur"
        val exception = Exception("Gemini API nicht erreichbar")

        coEvery { repository.generateCareNotes(scientificName) } returns
            Result.failure(exception)

        val result = useCase(scientificName)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke with special characters in name succeeds`() = runTest {
        val scientificName = "Acer platanoides 'Crimson King'"
        val careNotes = "Licht: Volle Sonne.\nWasser: Regelmäßig."

        coEvery { repository.generateCareNotes(scientificName) } returns
            Result.success(careNotes)

        val result = useCase(scientificName)

        assertTrue(result.isSuccess)
        coVerify { repository.generateCareNotes(scientificName) }
    }

    @Test
    fun `invoke delegates to repository without transformation`() = runTest {
        val scientificName = "Taraxacum officinale"
        val notes = "Licht: Volle Sonne. Wasser: Wenig. Boden: Beliebig. Winterhärte: -30°C."

        coEvery { repository.generateCareNotes(scientificName) } returns
            Result.success(notes)

        val result = useCase(scientificName)

        assertEquals(notes, result.getOrNull())
    }
}
