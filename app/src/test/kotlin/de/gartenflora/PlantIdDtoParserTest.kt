package de.gartenflora

import de.gartenflora.data.remote.dto.PlantIdHealthResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests that our Plant.id DTOs correctly parse the API response JSON.
 * Uses representative JSON samples from Plant.id v3 health_assessment docs.
 */
class PlantIdDtoParserTest {

    private lateinit var json: Json

    @Before
    fun setup() {
        json = Json { ignoreUnknownKeys = true; isLenient = true }
    }

    @Test
    fun `parse healthy plant response`() {
        val jsonString = """
        {
          "access_token": "abc123",
          "result": {
            "is_plant": { "probability": 0.99, "binary": true },
            "is_healthy": { "probability": 0.92, "binary": true },
            "disease": {
              "suggestions": []
            }
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<PlantIdHealthResponse>(jsonString)

        assertTrue(response.result.isPlant.binary)
        assertTrue(response.result.isHealthy.binary)
        assertEquals(0.92, response.result.isHealthy.probability, 0.001)
        assertTrue(response.result.disease?.suggestions?.isEmpty() ?: true)
    }

    @Test
    fun `parse unhealthy plant with disease suggestions`() {
        val jsonString = """
        {
          "access_token": "xyz789",
          "result": {
            "is_plant": { "probability": 0.98, "binary": true },
            "is_healthy": { "probability": 0.21, "binary": false },
            "disease": {
              "suggestions": [
                {
                  "id": "rust_001",
                  "name": "Rust disease",
                  "probability": 0.78,
                  "similar_images": [
                    { "url": "https://plant.id/img/rust1.jpg", "similarity": 0.85 }
                  ],
                  "details": {
                    "description": "Fungal infection causing orange-brown spots.",
                    "treatment": {
                      "biological": ["Apply copper-based fungicide", "Remove infected leaves"],
                      "chemical": ["Use systemic fungicide"],
                      "prevention": ["Ensure good air circulation", "Avoid wetting foliage"]
                    },
                    "classification": ["Fungal", "Disease"]
                  }
                },
                {
                  "id": "mildew_001",
                  "name": "Powdery mildew",
                  "probability": 0.15,
                  "similar_images": [],
                  "details": null
                }
              ]
            }
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<PlantIdHealthResponse>(jsonString)

        assertFalse(response.result.isHealthy.binary)
        assertEquals(0.21, response.result.isHealthy.probability, 0.001)

        val suggestions = response.result.disease!!.suggestions
        assertEquals(2, suggestions.size)

        val rust = suggestions[0]
        assertEquals("Rust disease", rust.name)
        assertEquals(0.78, rust.probability, 0.001)
        assertEquals(1, rust.similarImages.size)
        assertEquals("https://plant.id/img/rust1.jpg", rust.similarImages[0].url)
        assertEquals(0.85, rust.similarImages[0].similarity!!, 0.001)

        val details = rust.details!!
        assertEquals("Fungal infection causing orange-brown spots.", details.description)
        assertEquals(2, details.treatment!!.biological.size)
        assertEquals(1, details.treatment!!.chemical.size)
        assertEquals(2, details.treatment!!.prevention.size)
        assertEquals(listOf("Fungal", "Disease"), details.classification)

        val mildew = suggestions[1]
        assertEquals("Powdery mildew", mildew.name)
        assertNull(mildew.details)
        assertTrue(mildew.similarImages.isEmpty())
    }

    @Test
    fun `parse response with missing disease section`() {
        val jsonString = """
        {
          "result": {
            "is_plant": { "probability": 0.95, "binary": true },
            "is_healthy": { "probability": 0.88, "binary": true }
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<PlantIdHealthResponse>(jsonString)

        assertNull(response.result.disease)
        assertTrue(response.result.isHealthy.binary)
    }

    @Test
    fun `parse response with empty treatment lists`() {
        val jsonString = """
        {
          "result": {
            "is_plant": { "probability": 0.99, "binary": true },
            "is_healthy": { "probability": 0.10, "binary": false },
            "disease": {
              "suggestions": [
                {
                  "name": "Unknown deficiency",
                  "probability": 0.55,
                  "similar_images": [],
                  "details": {
                    "treatment": {
                      "biological": [],
                      "chemical": [],
                      "prevention": []
                    },
                    "classification": []
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<PlantIdHealthResponse>(jsonString)
        val suggestion = response.result.disease!!.suggestions[0]
        val treatment = suggestion.details!!.treatment!!

        assertTrue(treatment.biological.isEmpty())
        assertTrue(treatment.chemical.isEmpty())
        assertTrue(treatment.prevention.isEmpty())
        assertTrue(suggestion.details!!.classification.isEmpty())
    }

    @Test
    fun `parse response ignores unknown fields`() {
        val jsonString = """
        {
          "access_token": "tok",
          "unknownTopField": 42,
          "result": {
            "is_plant": { "probability": 0.99, "binary": true, "threshold": 0.5 },
            "is_healthy": { "probability": 0.9, "binary": true },
            "someNewField": "future_api_field"
          }
        }
        """.trimIndent()

        // Should not throw
        val response = json.decodeFromString<PlantIdHealthResponse>(jsonString)
        assertEquals(0.99, response.result.isPlant.probability, 0.001)
    }
}
