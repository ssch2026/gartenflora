package de.gartenflora.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenZoneDao {

    @Query("SELECT * FROM garden_zones ORDER BY name ASC")
    fun observeAll(): Flow<List<GardenZoneEntity>>

    @Query("SELECT * FROM garden_zones WHERE id = :id")
    suspend fun getById(id: Long): GardenZoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zone: GardenZoneEntity): Long

    @Update
    suspend fun update(zone: GardenZoneEntity)

    @Query("DELETE FROM garden_zones WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM garden_cells WHERE zone_id = :zoneId")
    fun observePlantCount(zoneId: Long): Flow<Int>
}
