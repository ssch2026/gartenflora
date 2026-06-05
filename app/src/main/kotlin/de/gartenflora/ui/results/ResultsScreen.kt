package de.gartenflora.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.gartenflora.R
import de.gartenflora.domain.model.PlantCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    imagePaths: String,
    organs: String,
    project: String,
    onNavigateToGarden: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val confirmState by viewModel.confirmState.collectAsState()
    val bottomSheetState = rememberModalBottomSheetState()

    LaunchedEffect(imagePaths, organs, project) {
        viewModel.identify(imagePaths, organs, project)
    }

    LaunchedEffect(confirmState.savedId) {
        if (confirmState.savedId != null) {
            onNavigateToGarden()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.results_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ResultsUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.results_loading))
                    }
                }

                is ResultsUiState.Empty -> {
                    EmptyResultsContent()
                }

                is ResultsUiState.QuotaExceeded -> {
                    ErrorContent(message = stringResource(R.string.results_error_quota))
                }

                is ResultsUiState.NoNetwork -> {
                    ErrorContent(message = stringResource(R.string.results_error_network))
                }

                is ResultsUiState.Error -> {
                    ErrorContent(message = state.message)
                }

                is ResultsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.candidates) { candidate ->
                            CandidateCard(
                                candidate = candidate,
                                onClick = { viewModel.selectCandidate(candidate) }
                            )
                        }
                    }
                }
            }
        }

        // Confirm Bottom Sheet
        if (confirmState.candidate != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissConfirm() },
                sheetState = bottomSheetState
            ) {
                ConfirmBottomSheetContent(
                    candidate = confirmState.candidate!!,
                    isSaving = confirmState.isSaving,
                    onConfirm = { viewModel.confirmAndSave(confirmState.candidate!!) },
                    onCancel = { viewModel.dismissConfirm() }
                )
            }
        }
    }
}

@Composable
fun CandidateCard(
    candidate: PlantCandidate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = candidate.scientificName,
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic
            )
            candidate.commonNameDe?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            candidate.family?.let { family ->
                Text(
                    text = "${stringResource(R.string.results_family)}: $family",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${stringResource(R.string.results_confidence)}:",
                    style = MaterialTheme.typography.labelMedium
                )
                LinearProgressIndicator(
                    progress = { candidate.score },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(candidate.score * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun ConfirmBottomSheetContent(
    candidate: PlantCandidate,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.results_scientific_name),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = candidate.scientificName,
            style = MaterialTheme.typography.titleLarge,
            fontStyle = FontStyle.Italic
        )
        candidate.commonNameDe?.let { name ->
            Text(
                text = stringResource(R.string.results_common_name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
        }
        candidate.family?.let { family ->
            Text(
                text = "${stringResource(R.string.results_family)}: $family",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                Text(stringResource(R.string.results_cancel))
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    Text(stringResource(R.string.results_confirm))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun EmptyResultsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.results_empty),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.results_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Wifi,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}
