package com.smartfilemanager.app.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.IntentSender
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.smartfilemanager.app.data.db.AppDatabase
import com.smartfilemanager.app.data.entity.DeletionLogEntity
import com.smartfilemanager.app.data.entity.RuleEntity
import com.smartfilemanager.app.data.repository.DeletionLogRepository
import com.smartfilemanager.app.data.repository.RuleRepository
import com.smartfilemanager.app.data.repository.VideoRepository
import com.smartfilemanager.app.domain.engine.RuleEngine
import com.smartfilemanager.app.domain.model.ScannedFile
import com.smartfilemanager.app.util.FileRefreshBus
import com.smartfilemanager.app.util.PreselectRuleBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanPhase { PICK_RULE, SCANNING, RESULTS, DONE }

data class ScanUiState(
    val savedRules: List<RuleEntity> = emptyList(),
    val selectedRule: RuleEntity? = null,
    val targetDirectory: String? = null,    // null = all storage
    val phase: ScanPhase = ScanPhase.PICK_RULE,
    val scanResults: List<ScannedFile> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val deletionResult: DeletionResult? = null,
    val pendingPreselectId: Int? = null,
    val error: String? = null
)

data class DeletionResult(
    val deletedCount: Int,
    val failedFiles: List<String>,
    val bytesFreed: Long
)

class ScanViewModel(
    application: Application,
    private val ruleRepository: RuleRepository,
    private val videoRepository: VideoRepository,
    private val deletionLogRepository: DeletionLogRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val ruleEngine = RuleEngine()
    private val gson = Gson()

    init {
        viewModelScope.launch {
            ruleRepository.getAllRules().collect { rules ->
                val pending = _uiState.value.pendingPreselectId
                val autoSelect = if (pending != null) rules.find { it.id == pending } else null
                _uiState.update {
                    it.copy(
                        savedRules = rules,
                        selectedRule = autoSelect ?: it.selectedRule,
                        targetDirectory = autoSelect?.targetDirectory ?: it.targetDirectory,
                        pendingPreselectId = if (autoSelect != null) null else pending
                    )
                }
                if (autoSelect != null) PreselectRuleBus.consume()
            }
        }
        viewModelScope.launch {
            PreselectRuleBus.pendingRuleId.collect { ruleId ->
                if (ruleId == null) return@collect
                val rule = _uiState.value.savedRules.find { it.id == ruleId }
                if (rule != null) {
                    selectRule(rule)
                    PreselectRuleBus.consume()
                } else {
                    // Rules not loaded yet — store and resolve when they arrive
                    _uiState.update { it.copy(pendingPreselectId = ruleId) }
                }
            }
        }
    }

    fun selectRule(rule: RuleEntity) {
        _uiState.update {
            it.copy(
                selectedRule = rule,
                targetDirectory = rule.targetDirectory
            )
        }
    }

    fun setTargetDirectory(dir: String?) {
        _uiState.update { it.copy(targetDirectory = dir) }
    }

    fun scan() {
        val rule = _uiState.value.selectedRule ?: return
        _uiState.update { it.copy(phase = ScanPhase.SCANNING, error = null) }
        viewModelScope.launch {
            try {
                val targetDir = _uiState.value.targetDirectory
                val allFiles = if (targetDir.isNullOrBlank()) {
                    videoRepository.getAllVideos()
                } else {
                    videoRepository.getVideosInDirectory(targetDir)
                }
                val ruleWithConditions = ruleRepository.getRuleWithConditions(rule.id)
                val conditions = ruleWithConditions?.second ?: emptyList()
                val matched = ruleEngine.evaluate(allFiles, conditions, rule.conditionLogic)
                _uiState.update {
                    it.copy(
                        phase = ScanPhase.RESULTS,
                        scanResults = matched,
                        selectedIds = matched.map { f -> f.id }.toSet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(phase = ScanPhase.PICK_RULE, error = "Scan failed: ${e.message}")
                }
            }
        }
    }

    fun toggleSelection(id: Long) {
        val updated = _uiState.value.selectedIds.toMutableSet().apply {
            if (id in this) remove(id) else add(id)
        }
        _uiState.update { it.copy(selectedIds = updated) }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedIds = it.scanResults.map { f -> f.id }.toSet()) }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun getDeleteIntentSender(): IntentSender? {
        val selectedFiles = _uiState.value.scanResults.filter { it.id in _uiState.value.selectedIds }
        if (selectedFiles.isEmpty()) return null
        return MediaStore.createDeleteRequest(
            getApplication<Application>().contentResolver,
            selectedFiles.map { it.uri }
        ).intentSender
    }

    fun onDeleteResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) {
            // User cancelled the system dialog — stay on results screen
            return
        }
        val selectedFiles = _uiState.value.scanResults.filter { it.id in _uiState.value.selectedIds }
        val count = selectedFiles.size
        val bytes = selectedFiles.sumOf { it.sizeBytes }
        val rule = _uiState.value.selectedRule

        viewModelScope.launch {
            deletionLogRepository.saveLog(
                DeletionLogEntity(
                    ruleId = rule?.id,
                    ruleName = rule?.name ?: "Ad-hoc",
                    runAt = System.currentTimeMillis(),
                    filesDeleted = count,
                    bytesFreed = bytes,
                    fileListJson = gson.toJson(selectedFiles.map {
                        mapOf("name" to it.name, "path" to it.path, "sizeBytes" to it.sizeBytes)
                    })
                )
            )
        }

        FileRefreshBus.notifyFilesChanged()

        _uiState.update {
            it.copy(
                phase = ScanPhase.DONE,
                deletionResult = DeletionResult(
                    deletedCount = count,
                    failedFiles = emptyList(),
                    bytesFreed = bytes
                )
            )
        }
    }

    fun reset() {
        _uiState.update {
            it.copy(
                selectedRule = null,
                targetDirectory = null,
                phase = ScanPhase.PICK_RULE,
                scanResults = emptyList(),
                selectedIds = emptySet(),
                deletionResult = null,
                error = null
            )
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    return ScanViewModel(
                        application,
                        RuleRepository(db),
                        VideoRepository(application),
                        DeletionLogRepository(db)
                    ) as T
                }
            }
    }
}
