package de.gartenflora.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import de.gartenflora.data.firebase.FirestoreService
import de.gartenflora.data.local.PlantObservationDao
import de.gartenflora.data.local.PlantObservationEntity
import de.gartenflora.data.remote.GeminiApiService
import de.gartenflora.data.remote.PlantIdApiService
import de.gartenflora.data.remote.PlantNetApiService
import de.gartenflora.data.remote.dto.GeminiRequest
import de.gartenflora.data.remote.dto.GeminiRequestContent
import de.gartenflora.data.remote.dto.GeminiRequestPart
import de.gartenflora.data.remote.dto.PlantIdHealthRequest
import de.gartenflora.di.ApplicationScope
import de.gartenflora.domain.model.DiagnoseResult
import de.gartenflora.domain.model.DiseaseItem
import de.gartenflora.domain.model.PlantCandidate
import de.gartenflora.domain.model.PlantObservation
import de.gartenflora.domain.model.TreatmentInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class PlantRepositoryImpl @Inject constructor(
    private val dao: PlantObservationDao,
    private val plantNetApi: PlantNetApiService,
    private val geminiApi: GeminiApiService,
    private val plantIdApi: PlantIdApiService,
    private val firestoreService: FirestoreService,
    @Named("plantnet_api_key") private val plantNetApiKey: String,
    @Named("gemini_api_key") private val geminiApiKey: String,
    @ApplicationScope private val appScope: CoroutineScope
) : PlantRepository {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Local observation CRUD ────────────────────────────────────────────────

    override fun observeAllObservations(): Flow<List<PlantObservation>> =
        dao.observeAll().map { it.map(PlantObservationEntity::toDomain) }

    override fun observeObservation(id: Long): Flow<PlantObservation?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun searchObservations(query: String): Flow<List<PlantObservation>> =
        dao.searchByName(query).map { it.map(PlantObservationEntity::toDomain) }

    override suspend fun getObservation(id: Long): PlantObservation? =
        dao.getById(id)?.toDomain()

    override suspend fun saveObservation(observation: PlantObservation): Result<Long> =
        runCatching {
            val id = dao.insert(observation.toEntity())
            syncToFirebase(observation.copy(id = id))
            id
        }

    override suspend fun updateObservation(observation: PlantObservation): Result<Unit> =
        runCatching {
            dao.update(observation.toEntity())
            syncToFirebase(observation)
        }

    override suspend fun deleteObservation(id: Long): Result<Unit> =
        runCatching {
            dao.deleteById(id)
            appScope.launch {
                runCatching {
                    firestoreService.ensureSignedIn()
                    firestoreService.deleteObservation(id)
                }
            }
        }

    /** Fire-and-forget Firebase sync — never blocks the caller. */
    private fun syncToFirebase(observation: PlantObservation) {
        appScope.launch {
            runCatching {
                firestoreService.ensureSignedIn()
                firestoreService.syncObservation(observation)
            }
        }
    }

    // ── Plant identification (Pl@ntNet) ───────────────────────────────────────

    override suspend fun identifyPlant(
        imagePaths: List<String>,
        organs: List<String>,
        project: String
    ): Result<List<PlantCandidate>> = runCatching {
        val imageParts = imagePaths.map { path ->
            val file = File(path)
            MultipartBody.Part.createFormData(
                "images", file.name, file.asRequestBody("image/jpeg".toMediaType())
            )
        }
        val organParts = organs.map { organ ->
            MultipartBody.Part.createFormData("organs", organ)
        }
        val response = plantNetApi.identify(
            project = project,
            apiKey = plantNetApiKey,
            images = imageParts,
            organs = organParts
        )
        if (response.results.isEmpty()) throw NoPlantDetectedException("Keine Pflanze erkannt")
        response.results.map { result ->
            PlantCandidate(
                scientificName = result.species.scientificNameWithoutAuthor,
                commonNameDe   = result.species.commonNames.firstOrNull(),
                family         = result.species.family?.scientificNameWithoutAuthor,
                genus          = result.species.genus?.scientificNameWithoutAuthor,
                gbifId         = result.gbif?.id,
                score          = result.score.toFloat()
            )
        }
    }.mapFailure()

    override suspend fun getProjects(): Result<List<Pair<String, String>>> = runCatching {
        plantNetApi.getProjects(apiKey = plantNetApiKey)
            .map { project -> project.id to project.displayName() }
    }

    // ── Care notes (Gemini) ───────────────────────────────────────────────────

    override suspend fun generateCareNotes(scientificName: String): Result<String> = runCatching {
        val prompt = buildString {
            appendLine("Gib mir kompakte Pflegehinweise auf Deutsch für '$scientificName'.")
            appendLine("Format: Licht | Wasser | Boden | Winterhärte (Zone 7b, Südwest-Deutschland)")
            appendLine("Jeweils 1–2 Sätze. Beginne direkt ohne Einleitung.")
        }
        val request = GeminiRequest(
            contents = listOf(
                GeminiRequestContent(parts = listOf(GeminiRequestPart(text = prompt)))
            )
        )
        val response = geminiApi.generateContent(
            model = "gemini-2.5-flash", apiKey = geminiApiKey, request = request
        )
        response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Keine Pflegehinweise erhalten")
    }

    // ── Health diagnosis (Plant.id) ───────────────────────────────────────────

    override suspend fun diagnoseHealth(
        imagePath: String,
        latitude: Double?,
        longitude: Double?
    ): Result<DiagnoseResult> = runCatching {
        val base64 = encodeImageToBase64(imagePath)
        val request = PlantIdHealthRequest(
            images    = listOf(base64),
            latitude  = latitude,
            longitude = longitude
        )
        val response = plantIdApi.healthAssessment(request)
        val result   = response.result

        DiagnoseResult(
            isHealthy            = result.isHealthy.binary,
            isHealthyProbability = result.isHealthy.probability.toFloat(),
            diseases = result.disease?.suggestions?.map { s ->
                DiseaseItem(
                    name            = s.name,
                    probability     = s.probability.toFloat(),
                    description     = s.details?.description,
                    treatment       = s.details?.treatment?.let { t ->
                        TreatmentInfo(
                            biological = t.biological,
                            chemical   = t.chemical,
                            prevention = t.prevention
                        )
                    },
                    similarImageUrls = s.similarImages.map { it.url },
                    classification   = s.details?.classification ?: emptyList()
                )
            } ?: emptyList()
        )
    }.mapFailure()

    // ── Image helpers ─────────────────────────────────────────────────────────

    private fun encodeImageToBase64(path: String): String {
        val bitmap = BitmapFactory.decodeFile(path)
            ?: throw IllegalArgumentException("Cannot decode image: $path")
        val out = ByteArrayOutputStream()
        // Compress to max ~800px wide to keep payload small
        val scaled = if (bitmap.width > 800) {
            val ratio = 800f / bitmap.width
            Bitmap.createScaledBitmap(
                bitmap, 800, (bitmap.height * ratio).toInt(), true
            )
        } else bitmap
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // ── Entity ↔ domain mapping ───────────────────────────────────────────────

    private fun PlantObservationEntity.toDomain(): PlantObservation {
        val paths: List<String> = try { json.decodeFromString(photoPaths) } catch (_: Exception) { emptyList() }
        return PlantObservation(
            id             = id,
            customName     = customName,
            scientificName = scientificName,
            commonNameDe   = commonNameDe,
            family         = family,
            genus          = genus,
            confidence     = confidence,
            gbifId         = gbifId,
            photoPaths     = paths,
            latitude       = latitude,
            longitude      = longitude,
            gardenSpot     = gardenSpot,
            careNotes      = careNotes,
            userNotes      = userNotes,
            createdAt      = createdAt
        )
    }

    private fun PlantObservation.toEntity() = PlantObservationEntity(
        id             = id,
        customName     = customName,
        scientificName = scientificName,
        commonNameDe   = commonNameDe,
        family         = family,
        genus          = genus,
        confidence     = confidence,
        gbifId         = gbifId,
        photoPaths     = json.encodeToString(photoPaths),
        latitude       = latitude,
        longitude      = longitude,
        gardenSpot     = gardenSpot,
        careNotes      = careNotes,
        userNotes      = userNotes,
        createdAt      = createdAt
    )

    // ── Error mapping ─────────────────────────────────────────────────────────

    private fun <T> Result<T>.mapFailure(): Result<T> = recoverCatching { t ->
        when {
            t is HttpException && t.code() == 429 ->
                throw QuotaExceededException("Tageskontingent erschöpft (429)")
            t is NoPlantDetectedException -> throw t
            else -> throw t
        }
    }
}

class NoPlantDetectedException(message: String) : Exception(message)
class QuotaExceededException(message: String) : Exception(message)
