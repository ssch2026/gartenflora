package de.gartenflora.domain.model

data class GardenCell(
    val id: Long = 0,
    val zoneId: Long,
    val row: Int,
    val col: Int,
    val observationId: Long
)
