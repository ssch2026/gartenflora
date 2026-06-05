package de.gartenflora.data.repository

import de.gartenflora.domain.model.GardenCell
import de.gartenflora.domain.model.GardenZone
import kotlinx.coroutines.flow.Flow

interface GardenRepository {

    fun observeZones(): Flow<List<GardenZone>>

    fun observeCells(zoneId: Long): Flow<List<GardenCell>>

    fun observePlantCount(zoneId: Long): Flow<Int>

    suspend fun getZone(id: Long): GardenZone?

    suspend fun saveZone(zone: GardenZone): Long

    suspend fun updateZone(zone: GardenZone)

    suspend fun deleteZone(id: Long)

    suspend fun placeCell(zoneId: Long, row: Int, col: Int, observationId: Long)

    suspend fun removeCell(zoneId: Long, row: Int, col: Int)
}
