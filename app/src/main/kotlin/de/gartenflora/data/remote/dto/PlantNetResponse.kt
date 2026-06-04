package de.gartenflora.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlantNetResponse(
    @SerialName("results") val results: List<PlantNetResult> = emptyList(),
    @SerialName("remainingIdentificationRequests") val remainingRequests: Int? = null,
    @SerialName("version") val version: String? = null,
    @SerialName("query") val query: PlantNetQuery? = null
)

@Serializable
data class PlantNetResult(
    @SerialName("score") val score: Double,
    @SerialName("species") val species: PlantNetSpecies,
    @SerialName("gbif") val gbif: PlantNetGbif? = null
)

@Serializable
data class PlantNetSpecies(
    @SerialName("scientificNameWithoutAuthor") val scientificNameWithoutAuthor: String,
    @SerialName("scientificNameAuthorship") val scientificNameAuthorship: String? = null,
    @SerialName("genus") val genus: PlantNetTaxon? = null,
    @SerialName("family") val family: PlantNetTaxon? = null,
    @SerialName("commonNames") val commonNames: List<String> = emptyList()
)

@Serializable
data class PlantNetTaxon(
    @SerialName("scientificNameWithoutAuthor") val scientificNameWithoutAuthor: String
)

@Serializable
data class PlantNetGbif(
    @SerialName("id") val id: String? = null
)

@Serializable
data class PlantNetQuery(
    @SerialName("project") val project: String? = null,
    @SerialName("images") val images: List<String> = emptyList(),
    @SerialName("organs") val organs: List<String> = emptyList()
)

@Serializable
data class PlantNetProject(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("nameByLang") val nameByLang: Map<String, String>? = null,
    @SerialName("description") val description: String? = null
) {
    fun displayName(): String =
        nameByLang?.get("de") ?: nameByLang?.get("en") ?: name ?: id
}

@Serializable
data class GeminiResponse(
    @SerialName("candidates") val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    @SerialName("content") val content: GeminiContent
)

@Serializable
data class GeminiContent(
    @SerialName("parts") val parts: List<GeminiPart> = emptyList()
)

@Serializable
data class GeminiPart(
    @SerialName("text") val text: String = ""
)
