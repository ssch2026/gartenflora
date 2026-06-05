package de.gartenflora

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import de.gartenflora.data.local.AppDatabase
import de.gartenflora.data.local.GardenCellDao
import de.gartenflora.data.local.GardenCellEntity
import de.gartenflora.data.local.GardenZoneDao
import de.gartenflora.data.local.GardenZoneEntity
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
class GardenDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var zoneDao: GardenZoneDao
    private lateinit var cellDao: GardenCellDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        zoneDao = database.gardenZoneDao()
        cellDao = database.gardenCellDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ── Zone CRUD ─────────────────────────────────────────────────────────────

    @Test
    fun insert_zone_and_getById_returns_entity() = runTest {
        val id = zoneDao.insert(buildZone("Hochbeet Nord"))

        val zone = zoneDao.getById(id)

        assertNotNull(zone)
        assertEquals("Hochbeet Nord", zone!!.name)
        assertEquals(id, zone.id)
    }

    @Test
    fun observeAll_zones_returns_sorted_by_name() = runTest {
        zoneDao.insert(buildZone("Terrasse"))
        zoneDao.insert(buildZone("Hochbeet"))
        zoneDao.insert(buildZone("Südwand"))

        zoneDao.observeAll().test {
            val zones = awaitItem()
            assertEquals(3, zones.size)
            assertEquals("Hochbeet", zones[0].name)
            assertEquals("Südwand", zones[1].name)
            assertEquals("Terrasse", zones[2].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun update_zone_persists_changes() = runTest {
        val id = zoneDao.insert(buildZone("Old Name", cols = 10))
        val zone = zoneDao.getById(id)!!

        zoneDao.update(zone.copy(name = "New Name", gridCols = 15))

        val updated = zoneDao.getById(id)
        assertEquals("New Name", updated!!.name)
        assertEquals(15, updated.gridCols)
    }

    @Test
    fun deleteById_zone_removes_entity() = runTest {
        val id = zoneDao.insert(buildZone("To Delete"))

        zoneDao.deleteById(id)

        assertNull(zoneDao.getById(id))
    }

    @Test
    fun getById_returns_null_for_missing_zone() = runTest {
        assertNull(zoneDao.getById(9999L))
    }

    // ── Cell CRUD ─────────────────────────────────────────────────────────────

    @Test
    fun insert_cell_and_observeByZone_returns_entity() = runTest {
        val zoneId = zoneDao.insert(buildZone("Zone A"))
        cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 0, col = 0, observationId = 100L))

        cellDao.observeByZone(zoneId).test {
            val cells = awaitItem()
            assertEquals(1, cells.size)
            assertEquals(0, cells[0].row)
            assertEquals(0, cells[0].col)
            assertEquals(100L, cells[0].observationId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insert_cell_with_same_position_replaces_existing() = runTest {
        val zoneId = zoneDao.insert(buildZone("Zone B"))
        cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 1, col = 2, observationId = 10L))
        // Same position, different observation
        cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 1, col = 2, observationId = 20L))

        cellDao.observeByZone(zoneId).test {
            val cells = awaitItem()
            assertEquals(1, cells.size) // still only one cell
            assertEquals(20L, cells[0].observationId) // replaced
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByPosition_removes_correct_cell() = runTest {
        val zoneId = zoneDao.insert(buildZone("Zone C"))
        cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 0, col = 0, observationId = 1L))
        cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 0, col = 1, observationId = 2L))
        cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 1, col = 0, observationId = 3L))

        cellDao.deleteByPosition(zoneId, 0, 1)

        cellDao.observeByZone(zoneId).test {
            val cells = awaitItem()
            assertEquals(2, cells.size)
            assertTrue(cells.none { it.row == 0 && it.col == 1 })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cells_cascade_delete_when_zone_deleted() = runTest {
        val zoneId = zoneDao.insert(buildZone("Zone D"))
        cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 0, col = 0, observationId = 1L))
        cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 1, col = 1, observationId = 2L))

        // Delete the zone — cells should cascade delete
        zoneDao.deleteById(zoneId)

        cellDao.observeByZone(zoneId).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observePlantCount_reflects_cell_count() = runTest {
        val zoneId = zoneDao.insert(buildZone("Zone E"))

        zoneDao.observePlantCount(zoneId).test {
            assertEquals(0, awaitItem()) // initially empty

            cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 0, col = 0, observationId = 1L))
            assertEquals(1, awaitItem())

            cellDao.insert(GardenCellEntity(zoneId = zoneId, row = 0, col = 1, observationId = 2L))
            assertEquals(2, awaitItem())

            cellDao.deleteByPosition(zoneId, 0, 0)
            assertEquals(1, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cells_in_different_zones_are_independent() = runTest {
        val zoneA = zoneDao.insert(buildZone("Zone A"))
        val zoneB = zoneDao.insert(buildZone("Zone B"))

        cellDao.insert(GardenCellEntity(zoneId = zoneA, row = 0, col = 0, observationId = 1L))
        cellDao.insert(GardenCellEntity(zoneId = zoneB, row = 0, col = 0, observationId = 2L))

        cellDao.observeByZone(zoneA).test {
            val cells = awaitItem()
            assertEquals(1, cells.size)
            assertEquals(1L, cells[0].observationId)
            cancelAndIgnoreRemainingEvents()
        }

        cellDao.observeByZone(zoneB).test {
            val cells = awaitItem()
            assertEquals(1, cells.size)
            assertEquals(2L, cells[0].observationId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildZone(
        name: String,
        cols: Int = 10,
        rows: Int = 8,
        cellSizeCm: Int = 50
    ) = GardenZoneEntity(name = name, gridCols = cols, gridRows = rows, cellSizeCm = cellSizeCm)
}
