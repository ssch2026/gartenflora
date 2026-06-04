package de.gartenflora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlantObservationEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantObservationDao(): PlantObservationDao
}
