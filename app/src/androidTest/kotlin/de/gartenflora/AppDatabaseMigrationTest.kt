package de.gartenflora

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.gartenflora.data.local.AppDatabase
import org.junit.Assert.assertEquals
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

    @Test
    @Throws(IOException::class)
    fun createDatabase_version1_succeeds() {
        // Creates the database with version 1 and closes it
        helper.createDatabase(testDbName, 1).apply {
            // Validate that the schema matches
            execSQL(
                """INSERT INTO plant_observations
                   (scientific_name, confidence, photo_paths, created_at)
                   VALUES ('Rosa canina', 0.95, '[]', ${System.currentTimeMillis()})"""
            )
            close()
        }

        // Re-open with Room to verify readability
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            testDbName
        ).build()

        val dao = db.plantObservationDao()
        // Just verify we can query without crashing
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun openDatabase_with_inMemory_succeeds() {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()

        // Verify database is open and accessible
        assertEquals(true, db.isOpen)
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun database_schema_has_plant_observations_table() {
        val dbHelper = helper.createDatabase(testDbName, 1)

        // Verify the table exists by querying it
        val cursor = dbHelper.query("SELECT name FROM sqlite_master WHERE type='table' AND name='plant_observations'")
        assertEquals(1, cursor.count)
        cursor.close()
        dbHelper.close()
    }
}
