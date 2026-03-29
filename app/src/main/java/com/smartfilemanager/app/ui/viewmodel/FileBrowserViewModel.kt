package com.smartfilemanager.app.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.smartfilemanager.app.data.db.AppDatabase
import com.smartfilemanager.app.data.entity.DeletionLogEntity
import com.smartfilemanager.app.data.repository.DeletionLogRepository
import com.smartfilemanager.app.data.repository.VideoRepository
import com.smartfilemanager.app.domain.model.ScannedFile
import com.smartfilemanager.app.util.FormatUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    SIZE_ASC("Smallest first"),
    SIZE_DESC("Largest first"),
    DURATION_ASC("Shortest first"),
    DURATION_DESC("Longest first"),
    DATE_ASC("Oldest first"),
    DATE_DESC("Newest first")
}

data class FileBrowserUiState(
    val allFiles: List<ScannedFile> = emptyList(),
    val displayedFiles: List<ScannedFile> = emptyList(),
    val folders: List<String> = emptyList(),
    val selectedFolder: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null,
    val snackbarMessage: String? = null
)

class FileBrowserViewModel(
    application: Application,
    private val videoRepository: VideoRepository,
    private val deletionLogRepository: DeletionLogRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val files = videoRepository.getAllVideos()
                val folders = files.map { it.path }.distinct().sorted()
                val displayed = applyFilterAndSort(files, _uiState.value.selectedFolder, _uiState.value.sortOrder)
                _uiState.update {
                    it.copy(
                        allFiles = files,
                        displayedFiles = displayed,
                        folders = folders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load videos: ${e.message}") }
            }
        }
    }

    fun setSort(sortOrder: SortOrder) {
        val displayed = applyFilterAndSort(_uiState.value.allFiles, _uiState.value.selectedFolder, sortOrder)
        _uiState.update { it.copy(sortOrder = sortOrder, displayedFiles = displayed) }
    }

    fun setFolder(folder: String?) {
        val displayed = applyFilterAndSort(_uiState.value.allFiles, folder, _uiState.value.sortOrder)
        _uiState.update { it.copy(selectedFolder = folder, displayedFiles = displayed) }
    }

    fun onFileLongPress(id: Long) {
        _uiState.update { it.copy(isSelectionMode = true, selectedIds = setOf(id)) }
    }

    fun toggleSelection(id: Long) {
        val updated = _uiState.value.selectedIds.toMutableSet().apply {
            if (id in this) remove(id) else add(id)
        }
        if (updated.isEmpty()) {
            _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }
        } else {
            _uiState.update { it.copy(selectedIds = updated) }
        }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }
    }

    fun getDeleteIntentSender(): android.content.IntentSender? {
        val selectedFiles = _uiState.value.allFiles.filter { it.id in _uiState.value.selectedIds }
        if (selectedFiles.isEmpty()) return null
        val deleteRequest = MediaStore.createDeleteRequest(
            getApplication<Application>().contentResolver,
            selectedFiles.map { it.uri }
        )
        return deleteRequest.intentSender
    }

    fun onDeleteResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) {
            _uiState.update { it.copy(snackbarMessage = "Deletion cancelled") }
            return
        }

        val selectedFiles = _uiState.value.allFiles.filter { it.id in _uiState.value.selectedIds }
        val count = selectedFiles.size
        val bytes = selectedFiles.sumOf { it.sizeBytes }

        viewModelScope.launch {
            deletionLogRepository.saveLog(
                DeletionLogEntity(
                    ruleId = null,
                    ruleName = "Manual deletion",
                    runAt = System.currentTimeMillis(),
                    filesDeleted = count,
                    bytesFreed = bytes,
                    fileListJson = gson.toJson(selectedFiles.map { it.name })
                )
            )
        }

        val remaining = _uiState.value.allFiles.filter { it.id !in _uiState.value.selectedIds }
        val newFolders = remaining.map { it.path }.distinct().sorted()
        // If selected folder is now empty, reset to "All"
        val newFolder = _uiState.value.selectedFolder?.takeIf { f -> remaining.any { it.path == f } }
        val displayed = applyFilterAndSort(remaining, newFolder, _uiState.value.sortOrder)

        _uiState.update {
            it.copy(
                allFiles = remaining,
                displayedFiles = displayed,
                folders = newFolders,
                selectedFolder = newFolder,
                isSelectionMode = false,
                selectedIds = emptySet(),
                snackbarMessage = "$count ${if (count == 1) "file" else "files"} deleted · ${FormatUtil.formatSize(bytes)} freed"
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun applyFilterAndSort(
        files: List<ScannedFile>,
        folder: String?,
        sort: SortOrder
    ): List<ScannedFile> {
        val filtered = if (folder == null) files else files.filter { it.path == folder }
        return when (sort) {
            SortOrder.NAME_ASC      -> filtered.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC     -> filtered.sortedByDescending { it.name.lowercase() }
            SortOrder.SIZE_ASC      -> filtered.sortedBy { it.sizeBytes }
            SortOrder.SIZE_DESC     -> filtered.sortedByDescending { it.sizeBytes }
            SortOrder.DURATION_ASC  -> filtered.sortedBy { it.durationMs }
            SortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            SortOrder.DATE_ASC      -> filtered.sortedBy { it.lastModified }
            SortOrder.DATE_DESC     -> filtered.sortedByDescending { it.lastModified }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    return FileBrowserViewModel(
                        application,
                        VideoRepository(application),
                        DeletionLogRepository(db)
                    ) as T
                }
            }
    }
}
