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
    /** 待确认删除的文件夹（含图片） */
    val pendingDeleteFolder: PhotoFolder? = null,
    /** 拖放移动操作结果提示（非空时短暂显示后清除） */
    val dragMoveResult: String? = null,
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
                _uiState.update { it.copy(isLoading = false, error = e.message ?: e.toString()) }
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

    // ── 删除文件夹（含图片）确认流程 ─────────────────────────────────────────────

    /** 请求删除：弹出确认对话框 */
    fun requestDeleteFolderWithMedia(folder: PhotoFolder) {
        _uiState.update { it.copy(pendingDeleteFolder = folder) }
    }

    /** 取消删除 */
    fun cancelDeleteFolderWithMedia() {
        _uiState.update { it.copy(pendingDeleteFolder = null) }
    }

    /**
     * 确认删除：先删除文件夹内所有图片，再删除文件夹记录。
     * Android 11+ 需要系统确认弹窗，此处直接删除（同 FolderDetailViewModel.confirmDelete）。
     */
    fun confirmDeleteFolderWithMedia() {
        val folder = _uiState.value.pendingDeleteFolder ?: return
        _uiState.update { it.copy(pendingDeleteFolder = null) }
        viewModelScope.launch {
            // 删除文件夹内所有图片
            val uris = folder.items.map { it.uri }
            if (uris.isNotEmpty()) {
                manageFolderUseCase.deleteMedia(uris)
            }
            // 删除文件夹数据库记录（CASCADE 删除映射）
            folder.customFolderId?.let { manageFolderUseCase.deleteFolder(it) }
            loadFolders()
        }
    }

    // ── 拖放移动：将文件夹拖到「自建」Tab 自动创建文件夹并移动图片 ────────────────

    /**
     * 把 [folder] 内所有图片移动到一个同名（或带时间戳）自建文件夹。
     * 如果 folder 本身就是自建文件夹则直接刷新，不重复操作。
     */
    fun moveFolderToCustom(folder: PhotoFolder) {
        viewModelScope.launch {
            if (folder.type == com.photomaster.app.domain.model.FolderType.CUSTOM) {
                // 已是自建文件夹，无需操作
                _uiState.update { it.copy(dragMoveResult = "「${folder.name}」已经是自建文件夹") }
                return@launch
            }
            if (folder.items.isEmpty()) {
                _uiState.update { it.copy(dragMoveResult = "文件夹为空，无需移动") }
                return@launch
            }
            // 创建同名自建文件夹（名字冲突由用户感知）
            val newFolderId = manageFolderUseCase.createFolder(folder.name)
            manageFolderUseCase.moveToCustomFolder(
                folderId = newFolderId,
                folderName = folder.name,
                items = folder.items,
            )
            _uiState.update { it.copy(dragMoveResult = "已移动到自建「${folder.name}」") }
            loadFolders()
        }
    }

    fun clearDragMoveResult() {
        _uiState.update { it.copy(dragMoveResult = null) }
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
