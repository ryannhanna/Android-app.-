package com.smartfilemanager.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfilemanager.app.data.db.AppDatabase
import com.smartfilemanager.app.data.entity.RuleEntity
import com.smartfilemanager.app.data.model.RuleWithConditions
import com.smartfilemanager.app.data.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class RulesListViewModel(private val ruleRepository: RuleRepository) : ViewModel() {

    val rulesFlow: Flow<List<RuleWithConditions>> = ruleRepository.getAllRulesWithConditions()

    fun deleteRule(rule: RuleEntity) {
        viewModelScope.launch {
            ruleRepository.deleteRule(rule)
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    return RulesListViewModel(RuleRepository(db)) as T
                }
            }
    }
}
