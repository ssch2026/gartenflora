package de.gartenflora

import de.gartenflora.data.remote.dto.PlantNetResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlantNetResponseParserTest {

    private lateinit var json: Json

    @Before
    fun setup() {
        json = Json { ignoreUnknownKeys = true }
    }

    @Test
    fun `parse valid JSON response returns correct results`() {
        val jsonString = """
        {
          "results": [
            {
              "score": 0.95,
              "species": {
                "scientificNameWithoutAuthor": "Rosa canina",
                "scientificNameAuthorship": "L.",
                "genus": {
                  "scientificNameWithoutAuthor": "Rosa"
                },
                "family": {
                  "scientificNameWithoutAuthor": "Rosaceae"
                },
                "commonNames": ["Hundsrose", "Wilde Rose"]
              },
              "gbif": {
                "id": "3004014"
              }
            },
            {
              "score": 0.75,
              "species": {
                "scientificNameWithoutAuthor": "Rosa rubiginosa",
                "commonNames": ["Weinrose"]
              }
            }
          ],
          "remainingIdentificationRequests": 490,
          "version": "2.0"
        }
        """.trimIndent()

        val response = json.decodeFromString<PlantNetResponse>(jsonString)

        assertEquals(2, response.results.size)
        assertEquals("Rosa canina", response.results[0].species.scientificNameWithoutAuthor)
        assertEquals(0.95, response.results[0].score, 0.001)
        assertEquals("Hundsrose", response.results[0].species.commonNames.first())
        assertEquals("Rosa", response.results[0].species.genus?.scientificNameWithoutAuthor)
        assertEquals("Rosaceae", response.results[0].species.family?.scientificNameWithoutAuthor)
        assertEquals("3004014", response.results[0].gbif?.id)
        assertEquals(490, response.remainingRequests)
    }

    @Test
    fun `parse empty results returns empty list`() {
        val jsonString = """
        {
          "results": [],
          "remainingIdentificationRequests": 500
        }
        """.trimIndent()

        val response = json.decodeFromString<PlantNetResponse>(jsonString)

        assertTrue(response.results.isEmpty())
    }

    @Test
    fun `parse response with missing optional fields succeeds`() {
        val jsonString = """
        {
          "results": [
            {
              "score": 0.5,
              "species": {
                "scientificNameWithoutAuthor": "Quercus robur",
                "commonNames": []
              }
            }
          ]
        }
        """.trimIndent()

        val response = json.decodeFromString<PlantNetResponse>(jsonString)

        assertEquals(1, response.results.size)
        assertEquals("Quercus robur", response.results[0].species.scientificNameWithoutAuthor)
        assertTrue(response.results[0].species.commonNames.isEmpty())
        assertEquals(null, response.results[0].gbif)
        assertEquals(null, response.results[0].species.family)
        assertEquals(null, response.results[0].species.genus)
    }

    @Test
    fun `parse response ignores unknown fields`() {
        val jsonString = """
        {
          "results": [
            {
              "score": 0.8,
              "species": {
                "scientificNameWithoutAuthor": "Betula pendula",
                "commonNames": ["Birke"],
                "unknownField": "someValue"
              },
              "unknownTopLevel": 42
            }
          ],
          "someNewApiField": "test"
        }
        """.trimIndent()

        val response = json.decodeFromString<PlantNetResponse>(jsonString)
        assertEquals(1, response.results.size)
        assertEquals("Betula pendula", response.results[0].species.scientificNameWithoutAuthor)
    }

    @Test
    fun `parse response without results field returns empty list`() {
        val jsonString = """
        {
          "remainingIdentificationRequests": 499,
          "version": "2.0"
        }
        """.trimIndent()

        val response = json.decodeFromString<PlantNetResponse>(jsonString)
        assertTrue(response.results.isEmpty())
    }
}
