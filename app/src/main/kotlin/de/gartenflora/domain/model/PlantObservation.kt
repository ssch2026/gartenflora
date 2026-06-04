package de.gartenflora.domain.model

data class PlantObservation(
    val id: Long = 0,
    val customName: String? = null,
    val scientificName: String,
    val commonNameDe: String? = null,
    val family: String? = null,
    val genus: String? = null,
    val confidence: Float,
    val gbifId: String? = null,
    val photoPaths: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gardenSpot: String? = null,
    val careNotes: String? = null,
    val userNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlantCandidate(
    val scientificName: String,
    val commonNameDe: String?,
    val family: String?,
    val genus: String?,
    val gbifId: String?,
    val score: Float
)
