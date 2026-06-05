package de.gartenflora.ui.detail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.gartenflora.BuildConfig
import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.domain.model.PlantObservation
import de.gartenflora.domain.usecase.DeleteObservationUseCase
import de.gartenflora.domain.usecase.EnrichWithGeminiUseCase
import de.gartenflora.domain.usecase.GetObservationsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val editCustomName: String = "",
    val editUserNotes: String = "",
    val editGardenSpot: String = "",
    val isFetchingLocation: Boolean = false,
    val isGeneratingCareNotes: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false
) {
    val geminiAvailable: Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()
    val plantIdAvailable: Boolean = BuildConfig.PLANTID_API_KEY.isNotBlank()
}

private val EMPTY_OBSERVATION = PlantObservation(scientificName = "", confidence = 0f)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getObservationsUseCase: GetObservationsUseCase,
    private val deleteObservationUseCase: DeleteObservationUseCase,
    private val enrichWithGeminiUseCase: EnrichWithGeminiUseCase,
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // ID driven via MutableStateFlow so observation is always initialized (never lateinit).
    private val _observationId = MutableStateFlow(0L)
    private var observationId: Long = 0L

    val observation: StateFlow<PlantObservation> = _observationId
        .flatMapLatest { id ->
            if (id == 0L) flowOf(EMPTY_OBSERVATION)
            else getObservationsUseCase.observeById(id).filterNotNull()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EMPTY_OBSERVATION
        )

    fun init(id: Long) {
        observationId = id
        _observationId.value = id
        _uiState.update { it.copy(isLoading = false) }
    }

    fun enterEditMode(obs: PlantObservation) {
        _uiState.update {
            it.copy(
                isEditMode = true,
                editCustomName = obs.customName ?: "",
                editUserNotes = obs.userNotes ?: "",
                editGardenSpot = obs.gardenSpot ?: ""
            )
        }
    }

    fun exitEditMode() {
        _uiState.update { it.copy(isEditMode = false) }
    }

    fun onCustomNameChange(value: String) {
        _uiState.update { it.copy(editCustomName = value) }
    }

    fun onUserNotesChange(value: String) {
        _uiState.update { it.copy(editUserNotes = value) }
    }

    fun onGardenSpotChange(value: String) {
        _uiState.update { it.copy(editGardenSpot = value) }
    }

    fun saveEdits(obs: PlantObservation) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = obs.copy(
                customName = state.editCustomName.takeIf { it.isNotBlank() },
                userNotes = state.editUserNotes.takeIf { it.isNotBlank() },
                gardenSpot = state.editGardenSpot.takeIf { it.isNotBlank() }
            )
            plantRepository.updateObservation(updated)
            _uiState.update { it.copy(isSaving = false, isEditMode = false) }
        }
    }

    fun captureLocation() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            _uiState.update { it.copy(error = "Standortberechtigung fehlt") }
            return
        }

        _uiState.update { it.copy(isFetchingLocation = true) }
        viewModelScope.launch {
            try {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val provider = when {
                    hasFineLocation -> LocationManager.GPS_PROVIDER
                    else -> LocationManager.NETWORK_PROVIDER
                }
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    val obs = getObservationsUseCase.observeById(observationId).stateIn(
                        viewModelScope, SharingStarted.Eagerly, null
                    ).value
                    obs?.let {
                        plantRepository.updateObservation(
                            it.copy(latitude = location.latitude, longitude = location.longitude)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isFetchingLocation = false) }
            }
        }
    }

    fun generateCareNotes(scientificName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingCareNotes = true) }
            enrichWithGeminiUseCase(scientificName).fold(
                onSuccess = { careNotes ->
                    val obs = getObservationsUseCase.observeById(observationId).stateIn(
                        viewModelScope, SharingStarted.Eagerly, null
                    ).value
                    obs?.let { plantRepository.updateObservation(it.copy(careNotes = careNotes)) }
                    _uiState.update { it.copy(isGeneratingCareNotes = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isGeneratingCareNotes = false, error = error.message) }
                }
            )
        }
    }

    fun deleteObservation() {
        viewModelScope.launch {
            deleteObservationUseCase(observationId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
