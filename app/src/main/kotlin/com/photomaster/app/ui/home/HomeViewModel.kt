package com.photomaster.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photomaster.app.domain.model.PhotoFolder
import com.photomaster.app.domain.usecase.ClassifyMediaUseCase
import com.photomaster.app.domain.usecase.ManageFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeTab { TODAY, ALL, CUSTOM }

data class HomeUiState(
    val isLoading: Boolean = false,
    val folders: List<PhotoFolder> = emptyList(),
    val selectedTab: HomeTab = HomeTab.TODAY,
    val showCreateFolderDialog: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val classifyMediaUseCase: ClassifyMediaUseCase,
    private val manageFolderUseCase: ManageFolderUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val folders = classifyMediaUseCase()
                _uiState.update { it.copy(isLoading = false, folders = folders) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun showCreateFolderDialog() {
        _uiState.update { it.copy(showCreateFolderDialog = true) }
    }

    fun hideCreateFolderDialog() {
        _uiState.update { it.copy(showCreateFolderDialog = false) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            manageFolderUseCase.createFolder(name.trim())
            hideCreateFolderDialog()
            loadFolders()
        }
    }

    fun deleteCustomFolder(folderId: Long) {
        viewModelScope.launch {
            manageFolderUseCase.deleteFolder(folderId)
            loadFolders()
        }
    }

    /** 按当前 Tab 过滤文件夹 */
    fun filteredFolders(): List<PhotoFolder> {
        val all = _uiState.value.folders
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        return when (_uiState.value.selectedTab) {
            HomeTab.TODAY -> all.filter { it.name.contains(today) }
            HomeTab.ALL -> all.filter { it.type != com.photomaster.app.domain.model.FolderType.CUSTOM }
            HomeTab.CUSTOM -> all.filter { it.type == com.photomaster.app.domain.model.FolderType.CUSTOM }
        }
    }
}
