package de.gartenflora.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.gartenflora.BuildConfig
import de.gartenflora.data.repository.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoadingProjects: Boolean = false,
    val projects: List<Pair<String, String>> = emptyList(),
    val selectedProjectId: String = "all",
    val isGeminiEnabled: Boolean = false,
    val geminiAvailable: Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank(),
    val projectsError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gartenflora_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            selectedProjectId = prefs.getString("selected_project", "all") ?: "all",
            isGeminiEnabled = prefs.getBoolean("gemini_enabled", false)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProjects = true, projectsError = null) }
            plantRepository.getProjects().fold(
                onSuccess = { projects ->
                    val allProject = "all" to "Alle Pflanzen (Standard)"
                    val combined = listOf(allProject) + projects
                    _uiState.update {
                        it.copy(isLoadingProjects = false, projects = combined)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingProjects = false,
                            projectsError = error.message
                        )
                    }
                }
            )
        }
    }

    fun retryLoadProjects() {
        loadProjects()
    }

    fun selectProject(projectId: String) {
        prefs.edit().putString("selected_project", projectId).apply()
        _uiState.update { it.copy(selectedProjectId = projectId) }
    }

    fun setGeminiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("gemini_enabled", enabled).apply()
        _uiState.update { it.copy(isGeminiEnabled = enabled) }
    }
}
