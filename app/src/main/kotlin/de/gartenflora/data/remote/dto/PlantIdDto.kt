package de.gartenflora.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Request ──────────────────────────────────────────────────────────────────

@Serializable
data class PlantIdHealthRequest(
    /** Base64-encoded JPEG images (max 3). */
    val images: List<String>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("similar_images") val similarImages: Boolean = true,
    /** "all" returns both healthy + disease suggestions. */
    val health: String = "all"
)

// ── Response ─────────────────────────────────────────────────────────────────

@Serializable
data class PlantIdHealthResponse(
    @SerialName("access_token") val accessToken: String = "",
    val result: PlantIdHealthResult
)

@Serializable
data class PlantIdHealthResult(
    @SerialName("is_plant") val isPlant: PlantIdProbability,
    @SerialName("is_healthy") val isHealthy: PlantIdProbability,
    val disease: PlantIdDiseaseSection? = null
)

@Serializable
data class PlantIdProbability(
    val probability: Double,
    val binary: Boolean
)

@Serializable
data class PlantIdDiseaseSection(
    val suggestions: List<PlantIdDiseaseSuggestion> = emptyList()
)

@Serializable
data class PlantIdDiseaseSuggestion(
    val id: String = "",
    val name: String,
    val probability: Double,
    @SerialName("similar_images") val similarImages: List<PlantIdSimilarImage> = emptyList(),
    val details: PlantIdDiseaseDetails? = null
)

@Serializable
data class PlantIdSimilarImage(
    val url: String,
    @SerialName("url_small") val urlSmall: String? = null,
    val similarity: Double? = null
)

@Serializable
data class PlantIdDiseaseDetails(
    val description: String? = null,
    val url: String? = null,
    val treatment: PlantIdTreatment? = null,
    val classification: List<String> = emptyList()
)

@Serializable
data class PlantIdTreatment(
    val biological: List<String> = emptyList(),
    val chemical: List<String> = emptyList(),
    val prevention: List<String> = emptyList()
)
