package de.gartenflora

import app.cash.turbine.test
import de.gartenflora.data.local.GardenCellDao
import de.gartenflora.data.local.GardenCellEntity
import de.gartenflora.data.local.GardenZoneDao
import de.gartenflora.data.local.GardenZoneEntity
import de.gartenflora.data.repository.GardenRepositoryImpl
import de.gartenflora.domain.model.GardenZone
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GardenRepositoryTest {

    private lateinit var zoneDao: GardenZoneDao
    private lateinit var cellDao: GardenCellDao
    private lateinit var repository: GardenRepositoryImpl

    @Before
    fun setup() {
        zoneDao = mockk(relaxed = true)
        cellDao = mockk(relaxed = true)
        repository = GardenRepositoryImpl(zoneDao, cellDao)
    }

    // ── Zones ─────────────────────────────────────────────────────────────────

    @Test
    fun `observeZones maps entities to domain models`() = runTest {
        val entities = listOf(
            buildZoneEntity(id = 1L, name = "Hochbeet", cols = 10, rows = 8),
            buildZoneEntity(id = 2L, name = "Terrasse", cols = 6, rows = 4)
        )
        every { zoneDao.observeAll() } returns flowOf(entities)

        repository.observeZones().test {
            val zones = awaitItem()
            assertEquals(2, zones.size)
            assertEquals("Hochbeet", zones[0].name)
            assertEquals(10, zones[0].gridCols)
            assertEquals(8, zones[0].gridRows)
            assertEquals("Terrasse", zones[1].name)
            awaitComplete()
        }
    }

    @Test
    fun `getZone returns domain model when found`() = runTest {
        coEvery { zoneDao.getById(1L) } returns buildZoneEntity(id = 1L, name = "Mein Beet")

        val zone = repository.getZone(1L)

        assertNotNull(zone)
        assertEquals("Mein Beet", zone!!.name)
        assertEquals(1L, zone.id)
    }

    @Test
    fun `getZone returns null when not found`() = runTest {
        coEvery { zoneDao.getById(99L) } returns null

        assertNull(repository.getZone(99L))
    }

    @Test
    fun `saveZone inserts entity and returns id`() = runTest {
        coEvery { zoneDao.insert(any()) } returns 5L
        val zone = GardenZone(name = "Neue Zone", gridCols = 10, gridRows = 8, cellSizeCm = 50)

        val id = repository.saveZone(zone)

        assertEquals(5L, id)
        coVerify(exactly = 1) { zoneDao.insert(match { it.name == "Neue Zone" }) }
    }

    @Test
    fun `updateZone calls dao update`() = runTest {
        val zone = GardenZone(id = 3L, name = "Updated", gridCols = 12, gridRows = 6, cellSizeCm = 40)

        repository.updateZone(zone)

        coVerify(exactly = 1) {
            zoneDao.update(match {
                it.id == 3L && it.name == "Updated" && it.gridCols == 12
            })
        }
    }

    @Test
    fun `deleteZone calls dao deleteById`() = runTest {
        repository.deleteZone(7L)

        coVerify(exactly = 1) { zoneDao.deleteById(7L) }
    }

    // ── Cells ─────────────────────────────────────────────────────────────────

    @Test
    fun `observeCells maps entities to domain models`() = runTest {
        val entities = listOf(
            GardenCellEntity(id = 1L, zoneId = 10L, row = 0, col = 2, observationId = 100L),
            GardenCellEntity(id = 2L, zoneId = 10L, row = 3, col = 5, observationId = 200L)
        )
        every { cellDao.observeByZone(10L) } returns flowOf(entities)

        repository.observeCells(10L).test {
            val cells = awaitItem()
            assertEquals(2, cells.size)
            assertEquals(0, cells[0].row)
            assertEquals(2, cells[0].col)
            assertEquals(100L, cells[0].observationId)
            assertEquals(3, cells[1].row)
            assertEquals(5, cells[1].col)
            awaitComplete()
        }
    }

    @Test
    fun `placeCell inserts entity with correct position`() = runTest {
        repository.placeCell(zoneId = 10L, row = 2, col = 4, observationId = 99L)

        coVerify(exactly = 1) {
            cellDao.insert(match {
                it.zoneId == 10L && it.row == 2 && it.col == 4 && it.observationId == 99L
            })
        }
    }

    @Test
    fun `removeCell calls dao deleteByPosition`() = runTest {
        repository.removeCell(zoneId = 10L, row = 2, col = 4)

        coVerify(exactly = 1) { cellDao.deleteByPosition(10L, 2, 4) }
    }

    @Test
    fun `observeCells emits empty list for zone with no cells`() = runTest {
        every { cellDao.observeByZone(99L) } returns flowOf(emptyList())

        repository.observeCells(99L).test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // ── Mapping round-trip ────────────────────────────────────────────────────

    @Test
    fun `saveZone preserves all fields`() = runTest {
        val zone = GardenZone(
            id = 0L,
            name = "Südwand",
            gridCols = 15,
            gridRows = 3,
            cellSizeCm = 25,
            backgroundPhotoPath = "/photos/bg.jpg"
        )
        coEvery { zoneDao.insert(any()) } returns 1L

        repository.saveZone(zone)

        coVerify {
            zoneDao.insert(match {
                it.name == "Südwand" &&
                it.gridCols == 15 &&
                it.gridRows == 3 &&
                it.cellSizeCm == 25 &&
                it.backgroundPhotoPath == "/photos/bg.jpg"
            })
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildZoneEntity(
        id: Long = 0L,
        name: String = "Zone",
        cols: Int = 10,
        rows: Int = 8,
        cellSizeCm: Int = 50
    ) = GardenZoneEntity(id = id, name = name, gridCols = cols, gridRows = rows, cellSizeCm = cellSizeCm)
}
