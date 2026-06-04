package de.gartenflora

import de.gartenflora.data.local.Converters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotoPathsConverterTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    @Test
    fun `fromPhotoPathsList serializes list to JSON string`() {
        val paths = listOf("/storage/path/photo1.jpg", "/storage/path/photo2.jpg")
        val result = converters.fromPhotoPathsList(paths)
        assertEquals("""["/storage/path/photo1.jpg","/storage/path/photo2.jpg"]""", result)
    }

    @Test
    fun `toPhotoPathsList deserializes JSON string to list`() {
        val json = """["/storage/path/photo1.jpg","/storage/path/photo2.jpg"]"""
        val result = converters.toPhotoPathsList(json)
        assertEquals(listOf("/storage/path/photo1.jpg", "/storage/path/photo2.jpg"), result)
    }

    @Test
    fun `fromPhotoPathsList with empty list produces empty JSON array`() {
        val result = converters.fromPhotoPathsList(emptyList())
        assertEquals("[]", result)
    }

    @Test
    fun `toPhotoPathsList with empty JSON array produces empty list`() {
        val result = converters.toPhotoPathsList("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toPhotoPathsList with invalid JSON returns empty list`() {
        val result = converters.toPhotoPathsList("not-valid-json")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toPhotoPathsList with null-like value returns empty list`() {
        val result = converters.toPhotoPathsList("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `round trip conversion preserves list`() {
        val original = listOf(
            "/data/user/0/de.gartenflora/files/Pictures/PLANT_20240101_123456_001.jpg",
            "/data/user/0/de.gartenflora/files/Pictures/PLANT_20240101_123456_002.jpg",
            "/data/user/0/de.gartenflora/files/Pictures/PLANT_20240101_123456_003.jpg"
        )
        val json = converters.fromPhotoPathsList(original)
        val restored = converters.toPhotoPathsList(json)
        assertEquals(original, restored)
    }

    @Test
    fun `single path round trip`() {
        val paths = listOf("/sdcard/DCIM/Plant.jpg")
        val json = converters.fromPhotoPathsList(paths)
        val restored = converters.toPhotoPathsList(json)
        assertEquals(paths, restored)
    }
}
