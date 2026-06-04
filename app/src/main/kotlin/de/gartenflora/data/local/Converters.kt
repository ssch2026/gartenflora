package de.gartenflora.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromPhotoPathsList(paths: List<String>): String {
        return json.encodeToString(paths)
    }

    @TypeConverter
    fun toPhotoPathsList(pathsJson: String): List<String> {
        return try {
            json.decodeFromString(pathsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
