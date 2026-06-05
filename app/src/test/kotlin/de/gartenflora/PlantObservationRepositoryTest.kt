package de.gartenflora

import app.cash.turbine.test
import de.gartenflora.data.firebase.FirestoreService
import de.gartenflora.data.local.PlantObservationDao
import de.gartenflora.data.local.PlantObservationEntity
import de.gartenflora.data.remote.GeminiApiService
import de.gartenflora.data.remote.PlantIdApiService
import de.gartenflora.data.remote.PlantNetApiService
import de.gartenflora.data.repository.PlantRepositoryImpl
import de.gartenflora.domain.model.PlantObservation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlantObservationRepositoryTest {

    private lateinit var dao: PlantObservationDao
    private lateinit var plantNetApi: PlantNetApiService
    private lateinit var geminiApi: GeminiApiService
    private lateinit var plantIdApi: PlantIdApiService
    private lateinit var firestoreService: FirestoreService
    private lateinit var repository: PlantRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true }
    private val testScope = CoroutineScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        plantNetApi = mockk()
        geminiApi = mockk()
        plantIdApi = mockk()
        firestoreService = mockk(relaxed = true) // relaxed: Firebase is fire-and-forget
        repository = PlantRepositoryImpl(
            dao = dao,
            plantNetApi = plantNetApi,
            geminiApi = geminiApi,
            plantIdApi = plantIdApi,
            firestoreService = firestoreService,
            plantNetApiKey = "test_key",
            geminiApiKey = "test_gemini_key",
            appScope = testScope
        )
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test
    fun `saveObservation inserts entity into dao and returns generated id`() = runTest {
        val observation = PlantObservation(scientificName = "Betula pendula", confidence = 0.9f)
        coEvery { dao.insert(any()) } returns 42L

        val result = repository.saveObservation(observation)

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())
        coVerify(exactly = 1) { dao.insert(any()) }
    }

    @Test
    fun `deleteObservation calls dao deleteById`() = runTest {
        val result = repository.deleteObservation(1L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dao.deleteById(1L) }
    }

    @Test
    fun `updateObservation calls dao update`() = runTest {
        val observation = PlantObservation(id = 5L, scientificName = "Rosa canina", confidence = 0.8f)

        val result = repository.updateObservation(observation)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dao.update(any()) }
    }

    @Test
    fun `observeAllObservations maps entities to domain models`() = runTest {
        val entity = buildEntity(id = 1L, scientificName = "Acer platanoides", confidence = 0.85f)
        every { dao.observeAll() } returns flowOf(listOf(entity))

        repository.observeAllObservations().test {
            val observations = awaitItem()
            assertEquals(1, observations.size)
            assertEquals("Acer platanoides", observations[0].scientificName)
            assertEquals(0.85f, observations[0].confidence)
            assertEquals(1L, observations[0].id)
            awaitComplete()
        }
    }

    @Test
    fun `getObservation by id returns domain model`() = runTest {
        val entity = buildEntity(id = 5L, scientificName = "Fagus sylvatica", confidence = 0.99f)
        coEvery { dao.getById(5L) } returns entity

        val result = repository.getObservation(5L)

        assertNotNull(result)
        assertEquals("Fagus sylvatica", result?.scientificName)
        assertEquals(5L, result?.id)
    }

    @Test
    fun `getObservation returns null when not found`() = runTest {
        coEvery { dao.getById(99L) } returns null

        assertNull(repository.getObservation(99L))
    }

    @Test
    fun `observeAllObservations deserializes photo paths correctly`() = runTest {
        val paths = listOf("/path/photo1.jpg", "/path/photo2.jpg")
        val entity = buildEntity(
            scientificName = "Rosa canina",
            confidence = 0.7f,
            photoPaths = json.encodeToString(paths)
        )
        every { dao.observeAll() } returns flowOf(listOf(entity))

        repository.observeAllObservations().test {
            assertEquals(paths, awaitItem()[0].photoPaths)
            awaitComplete()
        }
    }

    @Test
    fun `saveObservation succeeds even when Firebase is unavailable`() = runTest {
        coEvery { dao.insert(any()) } returns 1L
        coEvery { firestoreService.ensureSignedIn() } throws Exception("No network")

        val result = repository.saveObservation(
            PlantObservation(scientificName = "Rosa canina", confidence = 0.9f)
        )

        // Local write must succeed regardless of Firebase
        assertTrue(result.isSuccess)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildEntity(
        id: Long = 0L,
        scientificName: String = "Test species",
        commonNameDe: String? = null,
        confidence: Float = 0.5f,
        photoPaths: String = "[]"
    ) = PlantObservationEntity(
        id = id,
        scientificName = scientificName,
        commonNameDe = commonNameDe,
        confidence = confidence,
        photoPaths = photoPaths,
        createdAt = System.currentTimeMillis()
    )
}
