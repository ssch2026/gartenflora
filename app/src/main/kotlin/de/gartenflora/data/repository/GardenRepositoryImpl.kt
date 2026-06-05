package de.gartenflora.data.repository

import de.gartenflora.data.local.GardenCellDao
import de.gartenflora.data.local.GardenCellEntity
import de.gartenflora.data.local.GardenZoneDao
import de.gartenflora.data.local.GardenZoneEntity
import de.gartenflora.domain.model.GardenCell
import de.gartenflora.domain.model.GardenZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GardenRepositoryImpl @Inject constructor(
    private val zoneDao: GardenZoneDao,
    private val cellDao: GardenCellDao
) : GardenRepository {

    override fun observeZones(): Flow<List<GardenZone>> =
        zoneDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCells(zoneId: Long): Flow<List<GardenCell>> =
        cellDao.observeByZone(zoneId).map { list -> list.map { it.toDomain() } }

    override fun observePlantCount(zoneId: Long): Flow<Int> =
        zoneDao.observePlantCount(zoneId)

    override suspend fun getZone(id: Long): GardenZone? =
        zoneDao.getById(id)?.toDomain()

    override suspend fun saveZone(zone: GardenZone): Long =
        zoneDao.insert(zone.toEntity())

    override suspend fun updateZone(zone: GardenZone) =
        zoneDao.update(zone.toEntity())

    override suspend fun deleteZone(id: Long) =
        zoneDao.deleteById(id)

    override suspend fun placeCell(zoneId: Long, row: Int, col: Int, observationId: Long) {
        cellDao.insert(
            GardenCellEntity(zoneId = zoneId, row = row, col = col, observationId = observationId)
        )
    }

    override suspend fun removeCell(zoneId: Long, row: Int, col: Int) =
        cellDao.deleteByPosition(zoneId, row, col)

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun GardenZoneEntity.toDomain() = GardenZone(
        id = id, name = name, gridCols = gridCols,
        gridRows = gridRows, cellSizeCm = cellSizeCm,
        backgroundPhotoPath = backgroundPhotoPath
    )

    private fun GardenZone.toEntity() = GardenZoneEntity(
        id = id, name = name, gridCols = gridCols,
        gridRows = gridRows, cellSizeCm = cellSizeCm,
        backgroundPhotoPath = backgroundPhotoPath
    )

    private fun GardenCellEntity.toDomain() = GardenCell(
        id = id, zoneId = zoneId, row = row, col = col, observationId = observationId
    )
}
