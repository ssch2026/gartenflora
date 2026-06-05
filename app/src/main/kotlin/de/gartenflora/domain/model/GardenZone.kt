package de.gartenflora.domain.model

data class GardenZone(
    val id: Long = 0,
    val name: String,
    val gridCols: Int = 10,
    val gridRows: Int = 8,
    val cellSizeCm: Int = 50,
    val backgroundPhotoPath: String? = null
)
