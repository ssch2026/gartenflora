package de.gartenflora

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.gartenflora.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    // ── v1 baseline ───────────────────────────────────────────────────────────

    @Test
    @Throws(IOException::class)
    fun createDatabase_version1_and_insert_observation() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """INSERT INTO plant_observations
                   (scientific_name, confidence, photo_paths, created_at)
                   VALUES ('Rosa canina', 0.95, '[]', ${System.currentTimeMillis()})"""
            )
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun database_v1_has_plant_observations_table() {
        val db = helper.createDatabase(testDbName, 1)
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='plant_observations'"
        )
        assertEquals(1, cursor.count)
        cursor.close()
        db.close()
    }

    // ── MIGRATION_1_2 ─────────────────────────────────────────────────────────

    @Test
    @Throws(IOException::class)
    fun migration_1_to_2_creates_garden_zones_table() {
        helper.createDatabase(testDbName, 1).close()

        val db = helper.runMigrationsAndValidate(
            testDbName, 2, true, AppDatabase.MIGRATION_1_2
        )

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='garden_zones'"
        )
        assertEquals(1, cursor.count)
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_2_creates_garden_cells_table() {
        helper.createDatabase(testDbName, 1).close()

        val db = helper.runMigrationsAndValidate(
            testDbName, 2, true, AppDatabase.MIGRATION_1_2
        )

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='garden_cells'"
        )
        assertEquals(1, cursor.count)
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_2_preserves_existing_observations() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """INSERT INTO plant_observations
                   (scientific_name, confidence, photo_paths, created_at)
                   VALUES ('Betula pendula', 0.9, '[]', 1000000)"""
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDbName, 2, true, AppDatabase.MIGRATION_1_2
        )

        val cursor = db.query(
            "SELECT scientific_name FROM plant_observations WHERE scientific_name='Betula pendula'"
        )
        assertEquals(1, cursor.count)
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_2_garden_zones_accepts_insert() {
        helper.createDatabase(testDbName, 1).close()

        val db = helper.runMigrationsAndValidate(
            testDbName, 2, true, AppDatabase.MIGRATION_1_2
        )

        db.execSQL(
            """INSERT INTO garden_zones (name, grid_cols, grid_rows, cell_size_cm)
               VALUES ('Hochbeet Nord', 10, 8, 50)"""
        )
        val cursor = db.query("SELECT name FROM garden_zones WHERE name='Hochbeet Nord'")
        assertEquals(1, cursor.count)
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_2_unique_constraint_prevents_duplicate_cell_position() {
        helper.createDatabase(testDbName, 1).close()

        val db = helper.runMigrationsAndValidate(
            testDbName, 2, true, AppDatabase.MIGRATION_1_2
        )

        db.execSQL(
            """INSERT INTO garden_zones (id, name, grid_cols, grid_rows, cell_size_cm)
               VALUES (1, 'Zone', 10, 8, 50)"""
        )
        db.execSQL(
            "INSERT INTO garden_cells (zone_id, row, col, observation_id) VALUES (1, 2, 3, 10)"
        )

        var threw = false
        try {
            db.execSQL(
                "INSERT INTO garden_cells (zone_id, row, col, observation_id) VALUES (1, 2, 3, 20)"
            )
        } catch (_: Exception) {
            threw = true
        }
        assertTrue("Unique constraint on (zone_id, row, col) must prevent duplicates", threw)
        db.close()
    }

    // ── Room builder integration ───────────────────────────────────────────────

    @Test
    fun room_builder_with_migration_opens_successfully() {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()

        assertTrue(db.isOpen)
        // Verify all three DAOs are accessible
        db.plantObservationDao()
        db.gardenZoneDao()
        db.gardenCellDao()
        db.close()
    }
}
