package de.gartenflora.ui.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gartenflora.domain.model.PlantObservation
import de.gartenflora.domain.usecase.DeleteObservationUseCase
import de.gartenflora.domain.usecase.GetObservationsUseCase
import de.gartenflora.domain.usecase.SaveObservationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GartenUiState(
    val isLoading: Boolean = true,
    val observations: List<PlantObservation> = emptyList(),
    val searchQuery: String = "",
    val deletedObservation: PlantObservation? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MeinGartenViewModel @Inject constructor(
    private val getObservationsUseCase: GetObservationsUseCase,
    private val deleteObservationUseCase: DeleteObservationUseCase,
    private val saveObservationUseCase: SaveObservationUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _deletedObservation = MutableStateFlow<PlantObservation?>(null)
    val deletedObservation: StateFlow<PlantObservation?> = _deletedObservation.asStateFlow()

    val observations: StateFlow<List<PlantObservation>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                getObservationsUseCase()
            } else {
                getObservationsUseCase.search(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun deleteObservation(observation: PlantObservation) {
        viewModelScope.launch {
            _deletedObservation.value = observation
            deleteObservationUseCase(observation.id)
        }
    }

    fun undoDelete() {
        val observation = _deletedObservation.value ?: return
        viewModelScope.launch {
            saveObservationUseCase(observation.copy(id = 0))
            _deletedObservation.value = null
        }
    }

    fun clearDeletedObservation() {
        _deletedObservation.value = null
    }
}
