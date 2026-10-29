package com.classlog.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.classlog.app.data.datastore.AppSettings
import com.classlog.app.data.export.BackupRestoreManager
import com.classlog.app.data.export.RestoreResult
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.TimeSlotEntity
import com.classlog.app.data.repository.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * UI state for all setup screens and configuration management.
 */
data class SetupUiState(
    val batches: List<BatchEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false
)

class SetupViewModel(
    private val configRepository: ConfigRepository,
    private val backupRestoreManager: BackupRestoreManager
) : ViewModel() {

    suspend fun createBackupJson(): String = backupRestoreManager.createBackupJson()

    suspend fun restoreFromJson(jsonString: String): Result<RestoreResult> =
        backupRestoreManager.restoreFromJson(jsonString)

    val uiState: StateFlow<SetupUiState> = combine(
        configRepository.observeAllBatches(),
        configRepository.observeAllCategories(),
        configRepository.observeAllTimeSlots(),
        configRepository.settings
    ) { batches, categories, timeSlots, settings ->
        SetupUiState(
            batches = batches,
            categories = categories,
            timeSlots = timeSlots,
            settings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SetupUiState(isLoading = true)
    )

    // --- Batches ---
    fun saveBatch(name: String) {
        viewModelScope.launch {
            configRepository.saveBatch(name, sortOrder = uiState.value.batches.size)
        }
    }

    fun updateBatch(batch: BatchEntity) {
        viewModelScope.launch {
            configRepository.updateBatch(batch)
        }
    }

    fun toggleBatchActive(batch: BatchEntity) {
        viewModelScope.launch {
            configRepository.toggleBatchActive(batch)
        }
    }

    // --- Categories ---
    fun saveCategory(name: String, isTeaching: Boolean) {
        viewModelScope.launch {
            configRepository.saveCategory(name, isTeaching, sortOrder = uiState.value.categories.size)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            configRepository.updateCategory(category)
        }
    }

    fun toggleCategoryActive(category: CategoryEntity) {
        viewModelScope.launch {
            configRepository.toggleCategoryActive(category)
        }
    }

    // --- Time Slots ---
    fun saveTimeSlot(label: String, startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch {
            configRepository.saveTimeSlot(
                label = label,
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                sortOrder = uiState.value.timeSlots.size
            )
        }
    }

    fun updateTimeSlot(timeSlot: TimeSlotEntity) {
        viewModelScope.launch {
            configRepository.updateTimeSlot(timeSlot)
        }
    }

    fun toggleTimeSlotActive(timeSlot: TimeSlotEntity) {
        viewModelScope.launch {
            configRepository.toggleTimeSlotActive(timeSlot)
        }
    }

    // --- Settings ---
    fun setUse24HourFormat(use24Hour: Boolean) {
        viewModelScope.launch {
            configRepository.setUse24HourFormat(use24Hour)
        }
    }

    fun setQuickDurations(durations: List<Int>) {
        viewModelScope.launch {
            configRepository.setQuickDurations(durations)
        }
    }

    fun setTeacherName(name: String) {
        viewModelScope.launch {
            configRepository.setTeacherName(name)
        }
    }

    fun setInstitutionName(name: String) {
        viewModelScope.launch {
            configRepository.setInstitutionName(name)
        }
    }

    fun saveProfileImage(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        val original = BitmapFactory.decodeStream(input)
                        if (original != null) {
                            val maxDim = 512
                            val ratio = minOf(1f, maxDim.toFloat() / maxOf(original.width, original.height))
                            val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
                            val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
                            val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)
                            val photoFile = File(appContext.filesDir, "profile_photo.jpg")
                            FileOutputStream(photoFile).use { out ->
                                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            if (scaled != original) original.recycle()
                            configRepository.setProfileImagePath(photoFile.absolutePath)
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(appContext, "Profile photo updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(appContext, "Could not decode selected image.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(appContext, "Failed to load image: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun removeProfileImage(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val photoFile = File(context.filesDir, "profile_photo.jpg")
                    if (photoFile.exists()) {
                        photoFile.delete()
                    }
                    configRepository.setProfileImagePath(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configRepository.setAppLockEnabled(enabled)
        }
    }

    companion object {
        fun provideFactory(
            configRepository: ConfigRepository,
            backupRestoreManager: BackupRestoreManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SetupViewModel(configRepository, backupRestoreManager) as T
                }
            }
    }
}
