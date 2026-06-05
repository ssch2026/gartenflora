package de.gartenflora.ui.gardenplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import de.gartenflora.R
import de.gartenflora.domain.model.GardenCell
import de.gartenflora.domain.model.GardenZone
import de.gartenflora.domain.model.PlantObservation

private val CELL_SIZE = 72.dp
private val CELL_SPACING = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    zoneId: Long,
    onNavigateUp: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: GridViewModel = hiltViewModel()
) {
    LaunchedEffect(zoneId) { viewModel.init(zoneId) }

    val zone by viewModel.zone.collectAsState()
    val cells by viewModel.cells.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val filteredObservations by viewModel.filteredObservations.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(zone?.name ?: stringResource(R.string.gardenplan_grid_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        zone?.let { z ->
            // Scrollable in both directions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
            ) {
                GardenGrid(
                    zone = z,
                    cells = cells,
                    allObservations = viewModel.allObservations.collectAsState().value,
                    onCellTap = { row, col -> viewModel.onCellTap(row, col) }
                )
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.loading))
        }
    }

    // ── Plant picker sheet (empty cell tapped) ────────────────────────────────
    if (uiState.showPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSheets() },
            sheetState = sheetState
        ) {
            PlantPickerSheetContent(
                query = uiState.pickerQuery,
                observations = filteredObservations,
                onQueryChange = viewModel::onPickerQueryChange,
                onSelect = viewModel::placeObservation,
                onDismiss = viewModel::dismissSheets
            )
        }
    }

    // ── Cell detail sheet (occupied cell tapped) ──────────────────────────────
    if (uiState.showCellDetail) {
        val (row, col) = uiState.selectedCell ?: (0 to 0)
        val obs = viewModel.observationAt(row, col)
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSheets() },
            sheetState = sheetState
        ) {
            CellDetailSheetContent(
                observation = obs,
                onRemove = { viewModel.removeCell(row, col) },
                onNavigateToDetail = { obs?.let { onNavigateToDetail(it.id) } },
                onDismiss = viewModel::dismissSheets
            )
        }
    }
}

// ── Grid ──────────────────────────────────────────────────────────────────────

@Composable
private fun GardenGrid(
    zone: GardenZone,
    cells: List<GardenCell>,
    allObservations: List<PlantObservation>,
    onCellTap: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
        repeat(zone.gridRows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_SPACING)) {
                repeat(zone.gridCols) { col ->
                    val cell = cells.find { it.row == row && it.col == col }
                    val observation = cell?.let { c ->
                        allObservations.find { it.id == c.observationId }
                    }
                    GridCell(
                        observation = observation,
                        onClick = { onCellTap(row, col) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GridCell(
    observation: PlantObservation?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)

    if (observation != null) {
        // Occupied cell — show photo + plant name overlay
        Box(
            modifier = Modifier
                .size(CELL_SIZE)
                .clip(shape)
                .clickable(onClick = onClick)
        ) {
            val photo = observation.photoPaths.firstOrNull()
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.LocalFlorist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            // Name overlay at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                    .padding(horizontal = 2.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = observation.customName ?: observation.commonNameDe
                        ?: observation.scientificName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        color = androidx.compose.ui.graphics.Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Empty cell — dashed border + "+"
        Box(
            modifier = Modifier
                .size(CELL_SIZE)
                .clip(shape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = shape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Plant picker sheet ────────────────────────────────────────────────────────

@Composable
private fun PlantPickerSheetContent(
    query: String,
    observations: List<PlantObservation>,
    onQueryChange: (String) -> Unit,
    onSelect: (PlantObservation) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.gardenplan_pick_plant),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.gardenplan_search_plant)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (observations.isEmpty()) {
            Text(
                text = stringResource(R.string.gardenplan_no_plants),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(observations) { obs ->
                    PlantPickerItem(observation = obs, onClick = { onSelect(obs) })
                }
            }
        }
    }
}

@Composable
private fun PlantPickerItem(
    observation: PlantObservation,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val photo = observation.photoPaths.firstOrNull()
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocalFlorist, contentDescription = null)
                }
            }
            Column {
                Text(
                    text = observation.customName ?: observation.commonNameDe
                        ?: observation.scientificName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = observation.scientificName,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Cell detail sheet ─────────────────────────────────────────────────────────

@Composable
private fun CellDetailSheetContent(
    observation: PlantObservation?,
    onRemove: () -> Unit,
    onNavigateToDetail: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.gardenplan_cell_detail),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
        }

        if (observation != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val photo = observation.photoPaths.firstOrNull()
                if (photo != null) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Column {
                    Text(
                        text = observation.customName ?: observation.commonNameDe
                            ?: observation.scientificName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = observation.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    observation.gardenSpot?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToDetail,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.gardenplan_view_details))
                }
                Button(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.gardenplan_remove_plant))
                }
            }
        } else {
            Text(
                text = stringResource(R.string.error_generic),
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.width(0.dp)) // bottom padding spacer
    }
}
