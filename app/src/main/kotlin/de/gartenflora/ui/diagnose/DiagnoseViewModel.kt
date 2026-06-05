package de.gartenflora.ui.diagnose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gartenflora.BuildConfig
import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.domain.model.DiagnoseResult
import de.gartenflora.domain.model.PlantObservation
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

data class DiagnoseUiState(
    val isLoading: Boolean = false,
    val result: DiagnoseResult? = null,
    val error: String? = null,
    val selectedPhotoIndex: Int = 0
) {
    val plantIdAvailable: Boolean = BuildConfig.PLANTID_API_KEY.isNotBlank()
}

private val EMPTY_OBSERVATION = PlantObservation(scientificName = "", confidence = 0f)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiagnoseViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val getObservationsUseCase: GetObservationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnoseUiState())
    val uiState: StateFlow<DiagnoseUiState> = _uiState.asStateFlow()

    // ID driven via MutableStateFlow — observation is always initialized, never lateinit.
    private val _observationId = MutableStateFlow(0L)

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

    fun init(observationId: Long) {
        _observationId.value = observationId
    }

    fun selectPhoto(index: Int) {
        _uiState.update { it.copy(selectedPhotoIndex = index, result = null) }
    }

    fun analyze() {
        val obs = observation.value
        if (obs.photoPaths.isEmpty()) {
            _uiState.update { it.copy(error = "Keine Fotos vorhanden") }
            return
        }
        val path = obs.photoPaths.getOrElse(_uiState.value.selectedPhotoIndex) {
            obs.photoPaths.first()
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, result = null) }
            plantRepository.diagnoseHealth(
                imagePath = path,
                latitude = obs.latitude,
                longitude = obs.longitude
            ).fold(
                onSuccess = { result ->
                    _uiState.update { it.copy(isLoading = false, result = result) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
