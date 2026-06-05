package de.gartenflora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlantObservationEntity::class,
        GardenZoneEntity::class,
        GardenCellEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantObservationDao(): PlantObservationDao
    abstract fun gardenZoneDao(): GardenZoneDao
    abstract fun gardenCellDao(): GardenCellDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS garden_zones (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        grid_cols INTEGER NOT NULL DEFAULT 10,
                        grid_rows INTEGER NOT NULL DEFAULT 8,
                        cell_size_cm INTEGER NOT NULL DEFAULT 50,
                        background_photo_path TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS garden_cells (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        zone_id INTEGER NOT NULL,
                        row INTEGER NOT NULL,
                        col INTEGER NOT NULL,
                        observation_id INTEGER NOT NULL,
                        FOREIGN KEY (zone_id) REFERENCES garden_zones(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_garden_cells_zone_id_row_col " +
                    "ON garden_cells (zone_id, row, col)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_garden_cells_zone_id " +
                    "ON garden_cells (zone_id)"
                )
            }
        }
    }
}
