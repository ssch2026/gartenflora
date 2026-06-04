package de.gartenflora.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_observations")
data class PlantObservationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "custom_name")
    val customName: String? = null,

    @ColumnInfo(name = "scientific_name")
    val scientificName: String,

    @ColumnInfo(name = "common_name_de")
    val commonNameDe: String? = null,

    @ColumnInfo(name = "family")
    val family: String? = null,

    @ColumnInfo(name = "genus")
    val genus: String? = null,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "gbif_id")
    val gbifId: String? = null,

    @ColumnInfo(name = "photo_paths")
    val photoPaths: String = "[]",

    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,

    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,

    @ColumnInfo(name = "garden_spot")
    val gardenSpot: String? = null,

    @ColumnInfo(name = "care_notes")
    val careNotes: String? = null,

    @ColumnInfo(name = "user_notes")
    val userNotes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
