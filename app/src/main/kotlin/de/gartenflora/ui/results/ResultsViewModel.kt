package de.gartenflora.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gartenflora.data.repository.NoPlantDetectedException
import de.gartenflora.data.repository.QuotaExceededException
import de.gartenflora.domain.model.PlantCandidate
import de.gartenflora.domain.model.PlantObservation
import de.gartenflora.domain.usecase.IdentifyPlantUseCase
import de.gartenflora.domain.usecase.SaveObservationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ResultsUiState {
    object Loading : ResultsUiState()
    data class Success(val candidates: List<PlantCandidate>) : ResultsUiState()
    object Empty : ResultsUiState()
    object QuotaExceeded : ResultsUiState()
    object NoNetwork : ResultsUiState()
    data class Error(val message: String) : ResultsUiState()
}

data class ConfirmState(
    val candidate: PlantCandidate? = null,
    val isSaving: Boolean = false,
    val savedId: Long? = null
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val identifyPlantUseCase: IdentifyPlantUseCase,
    private val saveObservationUseCase: SaveObservationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private val _confirmState = MutableStateFlow(ConfirmState())
    val confirmState: StateFlow<ConfirmState> = _confirmState.asStateFlow()

    private var currentImagePaths: List<String> = emptyList()
    private var currentOrgans: List<String> = emptyList()

    fun identify(imagePaths: String, organs: String, project: String) {
        val paths = imagePaths.split(",").filter { it.isNotBlank() }
        val organList = organs.split(",").filter { it.isNotBlank() }
        currentImagePaths = paths
        currentOrgans = organList

        viewModelScope.launch {
            _uiState.value = ResultsUiState.Loading
            identifyPlantUseCase(paths, organList, project).fold(
                onSuccess = { candidates ->
                    _uiState.value = if (candidates.isEmpty()) {
                        ResultsUiState.Empty
                    } else {
                        ResultsUiState.Success(candidates)
                    }
                },
                onFailure = { error ->
                    _uiState.value = when (error) {
                        is NoPlantDetectedException -> ResultsUiState.Empty
                        is QuotaExceededException -> ResultsUiState.QuotaExceeded
                        is java.net.UnknownHostException,
                        is java.net.ConnectException -> ResultsUiState.NoNetwork
                        else -> ResultsUiState.Error(error.message ?: "Unbekannter Fehler")
                    }
                }
            )
        }
    }

    fun selectCandidate(candidate: PlantCandidate) {
        _confirmState.update { it.copy(candidate = candidate) }
    }

    fun dismissConfirm() {
        _confirmState.update { it.copy(candidate = null) }
    }

    fun confirmAndSave(candidate: PlantCandidate) {
        viewModelScope.launch {
            _confirmState.update { it.copy(isSaving = true) }
            val observation = PlantObservation(
                scientificName = candidate.scientificName,
                commonNameDe = candidate.commonNameDe,
                family = candidate.family,
                genus = candidate.genus,
                confidence = candidate.score,
                gbifId = candidate.gbifId,
                photoPaths = currentImagePaths,
                createdAt = System.currentTimeMillis()
            )
            saveObservationUseCase(observation).fold(
                onSuccess = { id ->
                    _confirmState.update { it.copy(isSaving = false, savedId = id, candidate = null) }
                },
                onFailure = { error ->
                    _confirmState.update { it.copy(isSaving = false) }
                }
            )
        }
    }
}
