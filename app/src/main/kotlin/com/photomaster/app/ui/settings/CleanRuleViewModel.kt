package com.photomaster.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photomaster.app.data.local.CleanRuleDao
import com.photomaster.app.data.local.entity.CleanRuleEntity
import com.photomaster.app.domain.model.FolderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 调度类型 */
enum class ScheduleType(val label: String) {
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月");
}

data class CleanRuleUiState(
    val rules: List<CleanRuleEntity> = emptyList(),
)

@HiltViewModel
class CleanRuleViewModel @Inject constructor(
    private val cleanRuleDao: CleanRuleDao,
) : ViewModel() {

    val uiState: StateFlow<CleanRuleUiState> = cleanRuleDao.getAllRules()
        .map { CleanRuleUiState(rules = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CleanRuleUiState())

    fun addRule(
        folderType: FolderType,
        scheduleType: ScheduleType,
        weekDay: Int = 0,
        monthDay: Int = 0,
    ) {
        viewModelScope.launch {
            cleanRuleDao.insertRule(
                CleanRuleEntity(
                    folderType = folderType.name,
                    scheduleType = scheduleType.name,
                    weekDay = weekDay,
                    monthDay = monthDay,
                )
            )
        }
    }

    fun toggleRule(rule: CleanRuleEntity) {
        viewModelScope.launch {
            cleanRuleDao.setEnabled(rule.id, !rule.enabled)
        }
    }

    fun deleteRule(rule: CleanRuleEntity) {
        viewModelScope.launch {
            cleanRuleDao.deleteRule(rule)
        }
    }
}
