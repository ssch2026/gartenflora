package de.gartenflora.ui.gardenplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gartenflora.data.repository.GardenRepository
import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.domain.model.GardenCell
import de.gartenflora.domain.model.GardenZone
import de.gartenflora.domain.model.PlantObservation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GridUiState(
    val zone: GardenZone? = null,
    val pickerQuery: String = "",
    // null = no sheet open; Pair(row, col) = cell tapped
    val selectedCell: Pair<Int, Int>? = null,
    // true = empty cell tapped (picker), false = occupied cell tapped (detail)
    val showPicker: Boolean = false,
    val showCellDetail: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GridViewModel @Inject constructor(
    private val gardenRepository: GardenRepository,
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val zoneIdFlow = MutableStateFlow(0L)

    val zone: StateFlow<GardenZone?> = zoneIdFlow
        .flatMapLatest { id ->
            if (id == 0L) flowOf(null)
            else gardenRepository.observeZones().flatMapLatest { zones ->
                flowOf(zones.find { it.id == id })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cells: StateFlow<List<GardenCell>> = zoneIdFlow
        .flatMapLatest { id ->
            if (id == 0L) flowOf(emptyList())
            else gardenRepository.observeCells(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allObservations: StateFlow<List<PlantObservation>> =
        plantRepository.observeAllObservations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(GridUiState())
    val uiState: StateFlow<GridUiState> = _uiState.asStateFlow()

    /** Filtered observations for the plant picker. */
    val filteredObservations: StateFlow<List<PlantObservation>> = combine(
        allObservations,
        _uiState
    ) { observations, state ->
        val q = state.pickerQuery.trim().lowercase()
        if (q.isEmpty()) observations
        else observations.filter { obs ->
            obs.scientificName.lowercase().contains(q) ||
            obs.commonNameDe?.lowercase()?.contains(q) == true ||
            obs.customName?.lowercase()?.contains(q) == true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun init(zoneId: Long) {
        zoneIdFlow.value = zoneId
    }

    // ── Cell interactions ─────────────────────────────────────────────────────

    fun onCellTap(row: Int, col: Int) {
        val cell = cells.value.find { it.row == row && it.col == col }
        _uiState.update {
            it.copy(
                selectedCell = row to col,
                showPicker = cell == null,
                showCellDetail = cell != null,
                pickerQuery = ""
            )
        }
    }

    fun dismissSheets() {
        _uiState.update { it.copy(selectedCell = null, showPicker = false, showCellDetail = false) }
    }

    // ── Picker ────────────────────────────────────────────────────────────────

    fun onPickerQueryChange(q: String) {
        _uiState.update { it.copy(pickerQuery = q) }
    }

    fun placeObservation(observation: PlantObservation) {
        val (row, col) = _uiState.value.selectedCell ?: return
        val zoneId = zoneIdFlow.value
        viewModelScope.launch {
            gardenRepository.placeCell(zoneId, row, col, observation.id)
            dismissSheets()
        }
    }

    // ── Cell detail ───────────────────────────────────────────────────────────

    fun removeCell(row: Int, col: Int) {
        val zoneId = zoneIdFlow.value
        viewModelScope.launch {
            gardenRepository.removeCell(zoneId, row, col)
            dismissSheets()
        }
    }

    /** Observation currently shown in the detail sheet. */
    fun observationAt(row: Int, col: Int): PlantObservation? {
        val obsId = cells.value.find { it.row == row && it.col == col }?.observationId
        return allObservations.value.find { it.id == obsId }
    }
}
