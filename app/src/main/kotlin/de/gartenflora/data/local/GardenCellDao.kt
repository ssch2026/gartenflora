package de.gartenflora.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenCellDao {

    @Query("SELECT * FROM garden_cells WHERE zone_id = :zoneId")
    fun observeByZone(zoneId: Long): Flow<List<GardenCellEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cell: GardenCellEntity): Long

    @Query("DELETE FROM garden_cells WHERE zone_id = :zoneId AND row = :row AND col = :col")
    suspend fun deleteByPosition(zoneId: Long, row: Int, col: Int)

    @Query("DELETE FROM garden_cells WHERE zone_id = :zoneId")
    suspend fun deleteAllInZone(zoneId: Long)
}
