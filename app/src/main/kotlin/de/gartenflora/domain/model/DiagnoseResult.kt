package de.gartenflora.domain.model

data class DiagnoseResult(
    val isHealthy: Boolean,
    val isHealthyProbability: Float,
    val diseases: List<DiseaseItem>
)

data class DiseaseItem(
    val name: String,
    val probability: Float,
    val description: String?,
    val treatment: TreatmentInfo?,
    val similarImageUrls: List<String>,
    val classification: List<String>
)

data class TreatmentInfo(
    val biological: List<String>,
    val chemical: List<String>,
    val prevention: List<String>
)
