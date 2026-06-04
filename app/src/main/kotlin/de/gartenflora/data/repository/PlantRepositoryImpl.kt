package de.gartenflora.data.repository

import de.gartenflora.data.local.PlantObservationDao
import de.gartenflora.data.local.PlantObservationEntity
import de.gartenflora.data.remote.GeminiApiService
import de.gartenflora.data.remote.PlantNetApiService
import de.gartenflora.data.remote.dto.GeminiRequest
import de.gartenflora.data.remote.dto.GeminiRequestContent
import de.gartenflora.data.remote.dto.GeminiRequestPart
import de.gartenflora.domain.model.PlantCandidate
import de.gartenflora.domain.model.PlantObservation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class PlantRepositoryImpl @Inject constructor(
    private val dao: PlantObservationDao,
    private val plantNetApi: PlantNetApiService,
    private val geminiApi: GeminiApiService,
    @Named("plantnet_api_key") private val plantNetApiKey: String,
    @Named("gemini_api_key") private val geminiApiKey: String
) : PlantRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeAllObservations(): Flow<List<PlantObservation>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeObservation(id: Long): Flow<PlantObservation?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun searchObservations(query: String): Flow<List<PlantObservation>> =
        dao.searchByName(query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getObservation(id: Long): PlantObservation? =
        dao.getById(id)?.toDomain()

    override suspend fun saveObservation(observation: PlantObservation): Result<Long> =
        runCatching {
            dao.insert(observation.toEntity())
        }

    override suspend fun updateObservation(observation: PlantObservation): Result<Unit> =
        runCatching {
            dao.update(observation.toEntity())
        }

    override suspend fun deleteObservation(id: Long): Result<Unit> =
        runCatching {
            dao.deleteById(id)
        }

    override suspend fun identifyPlant(
        imagePaths: List<String>,
        organs: List<String>,
        project: String
    ): Result<List<PlantCandidate>> = runCatching {
        val imageParts = imagePaths.map { path ->
            val file = File(path)
            val requestBody = file.asRequestBody("image/jpeg".toMediaType())
            MultipartBody.Part.createFormData("images", file.name, requestBody)
        }

        val organFormParts = organs.map { organ ->
            MultipartBody.Part.createFormData("organs", organ)
        }

        val response = plantNetApi.identify(
            project = project,
            apiKey = plantNetApiKey,
            images = imageParts,
            organs = organFormParts
        )

        if (response.results.isEmpty()) {
            throw NoPlantDetectedException("Keine Pflanze erkannt")
        }

        response.results.map { result ->
            PlantCandidate(
                scientificName = result.species.scientificNameWithoutAuthor,
                commonNameDe = result.species.commonNames.firstOrNull(),
                family = result.species.family?.scientificNameWithoutAuthor,
                genus = result.species.genus?.scientificNameWithoutAuthor,
                gbifId = result.gbif?.id,
                score = result.score.toFloat()
            )
        }
    }.mapFailure()

    override suspend fun getProjects(): Result<List<Pair<String, String>>> = runCatching {
        plantNetApi.getProjects(apiKey = plantNetApiKey)
            .map { project -> project.id to project.displayName() }
    }

    override suspend fun generateCareNotes(scientificName: String): Result<String> = runCatching {
        val prompt = buildString {
            appendLine("Gib mir kompakte Pflegehinweise auf Deutsch für die Pflanze '$scientificName'.")
            appendLine("Format: Licht | Wasser | Boden | Winterhärte (Zone 7b, Südwest-Deutschland)")
            appendLine("Jeweils 1-2 Sätze. Beginne direkt mit den Hinweisen, ohne Einleitung.")
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiRequestContent(
                    parts = listOf(GeminiRequestPart(text = prompt))
                )
            )
        )

        val response = geminiApi.generateContent(
            model = "gemini-2.5-flash",
            apiKey = geminiApiKey,
            request = request
        )

        response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Keine Pflegehinweise erhalten")
    }

    private fun PlantObservationEntity.toDomain(): PlantObservation {
        val paths: List<String> = try {
            json.decodeFromString(photoPaths)
        } catch (e: Exception) {
            emptyList()
        }
        return PlantObservation(
            id = id,
            customName = customName,
            scientificName = scientificName,
            commonNameDe = commonNameDe,
            family = family,
            genus = genus,
            confidence = confidence,
            gbifId = gbifId,
            photoPaths = paths,
            latitude = latitude,
            longitude = longitude,
            gardenSpot = gardenSpot,
            careNotes = careNotes,
            userNotes = userNotes,
            createdAt = createdAt
        )
    }

    private fun PlantObservation.toEntity(): PlantObservationEntity {
        return PlantObservationEntity(
            id = id,
            customName = customName,
            scientificName = scientificName,
            commonNameDe = commonNameDe,
            family = family,
            genus = genus,
            confidence = confidence,
            gbifId = gbifId,
            photoPaths = json.encodeToString(photoPaths),
            latitude = latitude,
            longitude = longitude,
            gardenSpot = gardenSpot,
            careNotes = careNotes,
            userNotes = userNotes,
            createdAt = createdAt
        )
    }

    private fun <T> Result<T>.mapFailure(): Result<T> = this.recoverCatching { throwable ->
        when {
            throwable is HttpException && throwable.code() == 429 ->
                throw QuotaExceededException("Tageskontingent erschöpft (429)")
            throwable is NoPlantDetectedException -> throw throwable
            else -> throw throwable
        }
    }
}

class NoPlantDetectedException(message: String) : Exception(message)
class QuotaExceededException(message: String) : Exception(message)
