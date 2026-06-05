package de.gartenflora.data.repository

import de.gartenflora.domain.model.DiagnoseResult
import de.gartenflora.domain.model.PlantCandidate
import de.gartenflora.domain.model.PlantObservation
import kotlinx.coroutines.flow.Flow

interface PlantRepository {

    fun observeAllObservations(): Flow<List<PlantObservation>>

    fun observeObservation(id: Long): Flow<PlantObservation?>

    fun searchObservations(query: String): Flow<List<PlantObservation>>

    suspend fun getObservation(id: Long): PlantObservation?

    suspend fun saveObservation(observation: PlantObservation): Result<Long>

    suspend fun updateObservation(observation: PlantObservation): Result<Unit>

    suspend fun deleteObservation(id: Long): Result<Unit>

    suspend fun identifyPlant(
        imagePaths: List<String>,
        organs: List<String>,
        project: String = "all"
    ): Result<List<PlantCandidate>>

    suspend fun getProjects(): Result<List<Pair<String, String>>>

    suspend fun generateCareNotes(scientificName: String): Result<String>

    /**
     * Send one photo (by local path) to Plant.id health assessment.
     * Returns a structured [DiagnoseResult] with disease suggestions + treatment info.
     */
    suspend fun diagnoseHealth(
        imagePath: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<DiagnoseResult>
}
