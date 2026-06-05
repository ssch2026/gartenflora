package de.gartenflora.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "garden_zones")
data class GardenZoneEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "grid_cols")
    val gridCols: Int = 10,

    @ColumnInfo(name = "grid_rows")
    val gridRows: Int = 8,

    @ColumnInfo(name = "cell_size_cm")
    val cellSizeCm: Int = 50,

    @ColumnInfo(name = "background_photo_path")
    val backgroundPhotoPath: String? = null
)
