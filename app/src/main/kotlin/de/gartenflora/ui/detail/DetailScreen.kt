package de.gartenflora.ui.detail

import android.Manifest
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.gartenflora.R
import de.gartenflora.domain.model.PlantObservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DetailScreen(
    observationId: Long,
    onNavigateUp: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    LaunchedEffect(observationId) {
        viewModel.init(observationId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val observation by viewModel.observation.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateUp()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(
                            onClick = { viewModel.saveEdits(observation) },
                            enabled = !uiState.isSaving
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                        }
                    } else {
                        IconButton(onClick = { viewModel.enterEditMode(observation) }) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Photo pager
                if (observation.photoPaths.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { observation.photoPaths.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) { page ->
                        AsyncImage(
                            model = observation.photoPaths[page],
                            contentDescription = stringResource(R.string.photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Names section
                    if (uiState.isEditMode) {
                        OutlinedTextField(
                            value = uiState.editCustomName,
                            onValueChange = { viewModel.onCustomNameChange(it) },
                            label = { Text(stringResource(R.string.detail_custom_name)) },
                            placeholder = { Text(stringResource(R.string.detail_custom_name_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        observation.customName?.let { name ->
                            Text(text = name, style = MaterialTheme.typography.headlineSmall)
                        }
                    }

                    Text(
                        text = observation.scientificName,
                        style = MaterialTheme.typography.titleLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary
                    )

                    observation.commonNameDe?.let { name ->
                        LabeledText(label = stringResource(R.string.detail_common_name), value = name)
                    }

                    observation.family?.let { family ->
                        LabeledText(label = stringResource(R.string.detail_family), value = family)
                    }

                    // Confidence
                    Column {
                        Text(
                            text = stringResource(R.string.detail_confidence),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = { observation.confidence },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(observation.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // Date
                    LabeledText(
                        label = stringResource(R.string.detail_date),
                        value = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
                            .format(Date(observation.createdAt))
                    )

                    // Garden spot
                    if (uiState.isEditMode) {
                        OutlinedTextField(
                            value = uiState.editGardenSpot,
                            onValueChange = { viewModel.onGardenSpotChange(it) },
                            label = { Text(stringResource(R.string.detail_garden_spot)) },
                            placeholder = { Text(stringResource(R.string.detail_garden_spot_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        observation.gardenSpot?.let { spot ->
                            LabeledText(label = stringResource(R.string.detail_garden_spot), value = spot)
                        }
                    }

                    // Location
                    if (observation.latitude != null && observation.longitude != null) {
                        LabeledText(
                            label = stringResource(R.string.detail_location),
                            value = "%.6f, %.6f".format(observation.latitude, observation.longitude)
                        )
                    } else {
                        Button(
                            onClick = {
                                if (locationPermission.status.isGranted) {
                                    viewModel.captureLocation()
                                } else {
                                    locationPermission.launchPermissionRequest()
                                }
                            },
                            enabled = !uiState.isFetchingLocation
                        ) {
                            if (uiState.isFetchingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Filled.LocationOn, contentDescription = null)
                                Text(stringResource(R.string.detail_capture_location))
                            }
                        }
                    }

                    // Care notes
                    if (observation.careNotes != null) {
                        Card {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.detail_care_notes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = observation.careNotes!!, style = MaterialTheme.typography.bodyMedium)
                                if (uiState.geminiAvailable) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.detail_gemini_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (uiState.geminiAvailable) {
                        Button(
                            onClick = { viewModel.generateCareNotes(observation.scientificName) },
                            enabled = !uiState.isGeneratingCareNotes
                        ) {
                            if (uiState.isGeneratingCareNotes) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                Text(stringResource(R.string.detail_generate_care_notes))
                            }
                        }
                        Text(
                            text = stringResource(R.string.detail_gemini_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // User notes
                    if (uiState.isEditMode) {
                        OutlinedTextField(
                            value = uiState.editUserNotes,
                            onValueChange = { viewModel.onUserNotesChange(it) },
                            label = { Text(stringResource(R.string.detail_user_notes)) },
                            placeholder = { Text(stringResource(R.string.detail_notes_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    } else {
                        observation.userNotes?.let { notes ->
                            LabeledText(label = stringResource(R.string.detail_user_notes), value = notes)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteObservation()
                    }
                ) {
                    Text(
                        stringResource(R.string.detail_delete_yes),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun LabeledText(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
