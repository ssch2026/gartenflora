package de.gartenflora.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    @SerialName("contents") val contents: List<GeminiRequestContent>
)

@Serializable
data class GeminiRequestContent(
    @SerialName("parts") val parts: List<GeminiRequestPart>
)

@Serializable
data class GeminiRequestPart(
    @SerialName("text") val text: String
)
