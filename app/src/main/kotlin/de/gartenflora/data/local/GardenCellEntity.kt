package de.gartenflora.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "garden_cells",
    indices = [Index(value = ["zone_id", "row", "col"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = GardenZoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["zone_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GardenCellEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "zone_id", index = true)
    val zoneId: Long,

    @ColumnInfo(name = "row")
    val row: Int,

    @ColumnInfo(name = "col")
    val col: Int,

    @ColumnInfo(name = "observation_id")
    val observationId: Long
)
