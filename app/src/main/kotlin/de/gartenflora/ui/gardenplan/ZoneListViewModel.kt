package de.gartenflora.ui.gardenplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gartenflora.data.repository.GardenRepository
import de.gartenflora.domain.model.GardenZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ZoneListUiState(
    val showCreateDialog: Boolean = false,
    val editZone: GardenZone? = null,       // non-null = editing existing zone
    val dialogName: String = "",
    val dialogCols: String = "10",
    val dialogRows: String = "8",
    val dialogCellSize: String = "50"
)

@HiltViewModel
class ZoneListViewModel @Inject constructor(
    private val gardenRepository: GardenRepository
) : ViewModel() {

    val zones: StateFlow<List<GardenZone>> = gardenRepository.observeZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ZoneListUiState())
    val uiState: StateFlow<ZoneListUiState> = _uiState.asStateFlow()

    // ── Dialog ────────────────────────────────────────────────────────────────

    fun openCreateDialog() {
        _uiState.update {
            it.copy(
                showCreateDialog = true,
                editZone = null,
                dialogName = "",
                dialogCols = "10",
                dialogRows = "8",
                dialogCellSize = "50"
            )
        }
    }

    fun openEditDialog(zone: GardenZone) {
        _uiState.update {
            it.copy(
                showCreateDialog = true,
                editZone = zone,
                dialogName = zone.name,
                dialogCols = zone.gridCols.toString(),
                dialogRows = zone.gridRows.toString(),
                dialogCellSize = zone.cellSizeCm.toString()
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showCreateDialog = false, editZone = null) }
    }

    fun onDialogNameChange(v: String)     { _uiState.update { it.copy(dialogName = v) } }
    fun onDialogColsChange(v: String)     { _uiState.update { it.copy(dialogCols = v) } }
    fun onDialogRowsChange(v: String)     { _uiState.update { it.copy(dialogRows = v) } }
    fun onDialogCellSizeChange(v: String) { _uiState.update { it.copy(dialogCellSize = v) } }

    fun confirmDialog() {
        val state = _uiState.value
        val name = state.dialogName.trim().ifBlank { return }
        val cols = state.dialogCols.toIntOrNull()?.coerceIn(2, 30) ?: 10
        val rows = state.dialogRows.toIntOrNull()?.coerceIn(2, 30) ?: 8
        val cellSize = state.dialogCellSize.toIntOrNull()?.coerceIn(10, 200) ?: 50

        viewModelScope.launch {
            val existing = state.editZone
            if (existing != null) {
                gardenRepository.updateZone(
                    existing.copy(name = name, gridCols = cols, gridRows = rows, cellSizeCm = cellSize)
                )
            } else {
                gardenRepository.saveZone(
                    GardenZone(name = name, gridCols = cols, gridRows = rows, cellSizeCm = cellSize)
                )
            }
            _uiState.update { it.copy(showCreateDialog = false, editZone = null) }
        }
    }

    // ── Zone actions ──────────────────────────────────────────────────────────

    fun deleteZone(zone: GardenZone) {
        viewModelScope.launch { gardenRepository.deleteZone(zone.id) }
    }
}
