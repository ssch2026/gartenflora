package de.gartenflora.ui.gardenplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.gartenflora.R
import de.gartenflora.domain.model.GardenZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneListScreen(
    onNavigateToGrid: (Long) -> Unit,
    viewModel: ZoneListViewModel = hiltViewModel()
) {
    val zones by viewModel.zones.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.gardenplan_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openCreateDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.gardenplan_add_zone))
            }
        }
    ) { padding ->
        if (zones.isEmpty()) {
            EmptyZonesContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(zones, key = { it.id }) { zone ->
                    ZoneCard(
                        zone = zone,
                        onClick = { onNavigateToGrid(zone.id) },
                        onEdit = { viewModel.openEditDialog(zone) },
                        onDelete = { viewModel.deleteZone(zone) }
                    )
                }
            }
        }
    }

    // Create / Edit dialog
    if (uiState.showCreateDialog) {
        ZoneDialog(
            uiState = uiState,
            onNameChange = viewModel::onDialogNameChange,
            onColsChange = viewModel::onDialogColsChange,
            onRowsChange = viewModel::onDialogRowsChange,
            onCellSizeChange = viewModel::onDialogCellSizeChange,
            onConfirm = viewModel::confirmDialog,
            onDismiss = viewModel::dismissDialog
        )
    }
}

@Composable
private fun ZoneCard(
    zone: GardenZone,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(text = zone.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${zone.gridCols} × ${zone.gridRows} ${stringResource(R.string.gardenplan_cells)} " +
                           "(${zone.cellSizeCm} cm)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyZonesContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.GridView,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.gardenplan_empty),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.gardenplan_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ZoneDialog(
    uiState: ZoneListUiState,
    onNameChange: (String) -> Unit,
    onColsChange: (String) -> Unit,
    onRowsChange: (String) -> Unit,
    onCellSizeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = uiState.editZone != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) stringResource(R.string.gardenplan_edit_zone)
                 else stringResource(R.string.gardenplan_new_zone))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.dialogName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.gardenplan_zone_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.dialogCols,
                        onValueChange = onColsChange,
                        label = { Text(stringResource(R.string.gardenplan_cols)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.dialogRows,
                        onValueChange = onRowsChange,
                        label = { Text(stringResource(R.string.gardenplan_rows)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = uiState.dialogCellSize,
                    onValueChange = onCellSizeChange,
                    label = { Text(stringResource(R.string.gardenplan_cell_size_cm)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = uiState.dialogName.isNotBlank()
            ) {
                Text(if (isEdit) stringResource(R.string.gardenplan_save)
                     else stringResource(R.string.gardenplan_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
