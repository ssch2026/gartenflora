package de.gartenflora.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantObservationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlantObservationEntity): Long

    @Update
    suspend fun update(entity: PlantObservationEntity)

    @Delete
    suspend fun delete(entity: PlantObservationEntity)

    @Query("DELETE FROM plant_observations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM plant_observations ORDER BY created_at DESC")
    fun observeAll(): Flow<List<PlantObservationEntity>>

    @Query("SELECT * FROM plant_observations ORDER BY created_at DESC")
    suspend fun getAll(): List<PlantObservationEntity>

    @Query("SELECT * FROM plant_observations WHERE id = :id")
    suspend fun getById(id: Long): PlantObservationEntity?

    @Query("SELECT * FROM plant_observations WHERE id = :id")
    fun observeById(id: Long): Flow<PlantObservationEntity?>

    @Query(
        """SELECT * FROM plant_observations
           WHERE lower(scientific_name) LIKE lower('%' || :query || '%')
           OR lower(custom_name) LIKE lower('%' || :query || '%')
           OR lower(common_name_de) LIKE lower('%' || :query || '%')
           ORDER BY created_at DESC"""
    )
    fun searchByName(query: String): Flow<List<PlantObservationEntity>>
}
