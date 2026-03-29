package com.smartfilemanager.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfilemanager.app.data.db.AppDatabase
import com.smartfilemanager.app.data.entity.DeletionLogEntity
import com.smartfilemanager.app.data.repository.DeletionLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val deletionLogRepository: DeletionLogRepository
) : ViewModel() {

    val logsFlow: Flow<List<DeletionLogEntity>> = deletionLogRepository.getAllLogs()

    private val _detailLog = MutableStateFlow<DeletionLogEntity?>(null)
    val detailLog: StateFlow<DeletionLogEntity?> = _detailLog.asStateFlow()

    fun loadDetail(logId: Int) {
        viewModelScope.launch {
            _detailLog.value = deletionLogRepository.getLogById(logId)
        }
    }

    fun deleteLog(log: DeletionLogEntity) {
        viewModelScope.launch {
            deletionLogRepository.deleteLog(log)
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    return HistoryViewModel(DeletionLogRepository(db)) as T
                }
            }
    }
}
