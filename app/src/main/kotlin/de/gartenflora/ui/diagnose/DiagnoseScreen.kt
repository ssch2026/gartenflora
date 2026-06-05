package de.gartenflora.ui.diagnose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import de.gartenflora.R
import de.gartenflora.domain.model.DiagnoseResult
import de.gartenflora.domain.model.DiseaseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnoseScreen(
    observationId: Long,
    onNavigateUp: () -> Unit,
    viewModel: DiagnoseViewModel = hiltViewModel()
) {
    LaunchedEffect(observationId) { viewModel.init(observationId) }

    val uiState by viewModel.uiState.collectAsState()
    val observation by viewModel.observation.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnose_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plant name header
            Text(
                text = observation.customName ?: observation.commonNameDe ?: observation.scientificName,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = observation.scientificName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Photo selector
            if (observation.photoPaths.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.diagnose_select_photo),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(observation.photoPaths) { index, path ->
                        val selected = index == uiState.selectedPhotoIndex
                        AsyncImage(
                            model = path,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.selectPhoto(index) }
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.diagnose_no_photos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Analyze button
            Button(
                onClick = { viewModel.analyze() },
                enabled = !uiState.isLoading && observation.photoPaths.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.diagnose_analyzing))
                } else {
                    Icon(Icons.Filled.BugReport, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.diagnose_analyze))
                }
            }

            // Results
            uiState.result?.let { result ->
                DiagnoseResultContent(result)
            }
        }
    }
}

@Composable
private fun DiagnoseResultContent(result: DiagnoseResult) {
    // Health status card
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (result.isHealthy)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (result.isHealthy) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (result.isHealthy)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = if (result.isHealthy) "Pflanze gesund" else "Probleme erkannt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (result.isHealthy)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Wahrscheinlichkeit: ${(result.isHealthyProbability * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.isHealthy)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    if (result.diseases.isNotEmpty()) {
        Text(
            text = "Erkannte Probleme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        result.diseases
            .filter { it.probability > 0.05f }
            .take(5)
            .forEach { disease -> DiseaseCard(disease) }
    }
}

@Composable
private fun DiseaseCard(disease: DiseaseItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = disease.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(disease.probability * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { disease.probability },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    disease.probability > 0.7f -> MaterialTheme.colorScheme.error
                    disease.probability > 0.4f -> Color(0xFFF57C00)
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            if (disease.classification.isNotEmpty()) {
                Text(
                    text = disease.classification.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            disease.description?.let { desc ->
                if (desc.isNotBlank()) {
                    HorizontalDivider()
                    Text(text = desc, style = MaterialTheme.typography.bodySmall)
                }
            }

            disease.treatment?.let { treatment ->
                val hasTreatment = treatment.biological.isNotEmpty() ||
                        treatment.prevention.isNotEmpty() ||
                        treatment.chemical.isNotEmpty()
                if (hasTreatment) {
                    HorizontalDivider()
                    if (treatment.prevention.isNotEmpty()) {
                        TreatmentSection("Vorbeugung", treatment.prevention)
                    }
                    if (treatment.biological.isNotEmpty()) {
                        TreatmentSection("Biologische Behandlung", treatment.biological)
                    }
                    if (treatment.chemical.isNotEmpty()) {
                        TreatmentSection("Chemische Behandlung", treatment.chemical)
                    }
                }
            }

            if (disease.similarImageUrls.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Ähnliche Fälle",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val urls = disease.similarImageUrls.take(4)
                    itemsIndexed(urls) { _, url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreatmentSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        items.take(3).forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
