package de.gartenflora

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import de.gartenflora.data.local.AppDatabase
import de.gartenflora.data.local.PlantObservationEntity
import de.gartenflora.data.local.PlantObservationDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PlantObservationDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.plantObservationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_and_getById_returns_entity() = runTest {
        val entity = buildEntity(scientificName = "Rosa canina", confidence = 0.95f)
        val insertedId = dao.insert(entity)

        val retrieved = dao.getById(insertedId)

        assertNotNull(retrieved)
        assertEquals("Rosa canina", retrieved?.scientificName)
        assertEquals(insertedId, retrieved?.id)
    }

    @Test
    fun getAll_returns_all_inserted_entities() = runTest {
        dao.insert(buildEntity(scientificName = "Betula pendula", confidence = 0.9f))
        dao.insert(buildEntity(scientificName = "Quercus robur", confidence = 0.85f))
        dao.insert(buildEntity(scientificName = "Fagus sylvatica", confidence = 0.8f))

        val all = dao.getAll()

        assertEquals(3, all.size)
    }

    @Test
    fun delete_removes_entity() = runTest {
        val id = dao.insert(buildEntity(scientificName = "Pinus sylvestris", confidence = 0.7f))
        val entity = dao.getById(id)!!

        dao.delete(entity)

        assertNull(dao.getById(id))
    }

    @Test
    fun deleteById_removes_entity() = runTest {
        val id = dao.insert(buildEntity(scientificName = "Abies alba", confidence = 0.6f))

        dao.deleteById(id)

        assertNull(dao.getById(id))
    }

    @Test
    fun observeAll_emits_updated_list_on_insert() = runTest {
        dao.observeAll().test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            dao.insert(buildEntity(scientificName = "Viola tricolor", confidence = 0.88f))
            val afterInsert = awaitItem()
            assertEquals(1, afterInsert.size)
            assertEquals("Viola tricolor", afterInsert[0].scientificName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchByName_returns_matching_entities() = runTest {
        dao.insert(buildEntity(scientificName = "Rosa canina", commonNameDe = "Hundsrose", confidence = 0.9f))
        dao.insert(buildEntity(scientificName = "Rosa rubiginosa", commonNameDe = "Weinrose", confidence = 0.8f))
        dao.insert(buildEntity(scientificName = "Betula pendula", commonNameDe = "Birke", confidence = 0.7f))

        dao.searchByName("Rosa").test {
            val results = awaitItem()
            assertEquals(2, results.size)
            assertTrue(results.all { it.scientificName.contains("Rosa") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchByName_matches_common_name() = runTest {
        dao.insert(buildEntity(scientificName = "Betula pendula", commonNameDe = "Sandbirke", confidence = 0.8f))
        dao.insert(buildEntity(scientificName = "Quercus robur", commonNameDe = "Stieleiche", confidence = 0.9f))

        dao.searchByName("birke").test {
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("Betula pendula", results[0].scientificName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchByName_case_insensitive() = runTest {
        dao.insert(buildEntity(scientificName = "Acer platanoides", confidence = 0.75f))

        dao.searchByName("ACER").test {
            val results = awaitItem()
            assertEquals(1, results.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeById_emits_null_when_deleted() = runTest {
        val id = dao.insert(buildEntity(scientificName = "Sorbus aucuparia", confidence = 0.6f))

        dao.observeById(id).test {
            assertNotNull(awaitItem())
            dao.deleteById(id)
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun buildEntity(
        scientificName: String,
        commonNameDe: String? = null,
        confidence: Float = 0.5f
    ) = PlantObservationEntity(
        scientificName = scientificName,
        commonNameDe = commonNameDe,
        confidence = confidence,
        photoPaths = "[]",
        createdAt = System.currentTimeMillis()
    )
}
