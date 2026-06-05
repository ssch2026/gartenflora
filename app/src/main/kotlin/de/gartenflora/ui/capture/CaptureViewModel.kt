package de.gartenflora.ui.capture

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class OrganType(val apiValue: String, val displayKey: String) {
    LEAF("leaf", "capture_organ_leaf"),
    FLOWER("flower", "capture_organ_flower"),
    FRUIT("fruit", "capture_organ_fruit"),
    BARK("bark", "capture_organ_bark"),
    AUTO("auto", "capture_organ_auto")
}

data class CapturedPhoto(
    val uri: String,        // content:// URI — Google Photos picks this up automatically
    val organ: OrganType = OrganType.AUTO
)

data class CaptureUiState(
    val capturedPhotos: List<CapturedPhoto> = emptyList(),
    val isCapturing: Boolean = false,
    val error: String? = null,
    val selectedProject: String = "all"
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    val canIdentify: Boolean
        get() = _uiState.value.capturedPhotos.isNotEmpty()

    fun capturePhoto(imageCapture: ImageCapture) {
        if (_uiState.value.capturedPhotos.size >= 5) return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val filename = "PLANT_${timestamp}.jpg"

        // Save into Pictures/GartenFlora/ — Google Photos backs this up automatically
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GartenFlora")
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        _uiState.update { it.copy(isCapturing = true, error = null) }

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri?.toString() ?: return
                    _uiState.update { state ->
                        state.copy(
                            capturedPhotos = state.capturedPhotos + CapturedPhoto(uri = uri),
                            isCapturing = false
                        )
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    _uiState.update { it.copy(isCapturing = false, error = exc.message) }
                }
            }
        )
    }

    fun removePhoto(index: Int) {
        _uiState.update { state ->
            val newPhotos = state.capturedPhotos.toMutableList()
            if (index in newPhotos.indices) {
                val removed = newPhotos.removeAt(index)
                // Delete from MediaStore so it doesn't linger in Google Photos
                try {
                    context.contentResolver.delete(Uri.parse(removed.uri), null, null)
                } catch (_: Exception) { /* ignore */ }
            }
            state.copy(capturedPhotos = newPhotos)
        }
    }

    fun setOrganForPhoto(index: Int, organ: OrganType) {
        _uiState.update { state ->
            val newPhotos = state.capturedPhotos.toMutableList()
            if (index in newPhotos.indices) {
                newPhotos[index] = newPhotos[index].copy(organ = organ)
            }
            state.copy(capturedPhotos = newPhotos)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getImagePathsJson(): String = _uiState.value.capturedPhotos.joinToString(",") { it.uri }

    fun getOrgansJson(): String = _uiState.value.capturedPhotos.joinToString(",") { it.organ.apiValue }
}
