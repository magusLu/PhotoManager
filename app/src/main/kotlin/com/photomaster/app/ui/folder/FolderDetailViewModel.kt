package com.photomaster.app.ui.folder

import android.net.Uri
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photomaster.app.domain.model.FolderType
import com.photomaster.app.domain.model.MediaItem
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

data class FolderDetailUiState(
    val isLoading: Boolean = true,
    val folder: PhotoFolder? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectMode: Boolean = false,
    val allFolders: List<PhotoFolder> = emptyList(),
    val showMoveDialog: Boolean = false,
    val showCopyDialog: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    /** Android 11+: 待传给系统删除弹窗的 IntentSenderRequest */
    val pendingDeleteRequest: IntentSenderRequest? = null,
    val error: String? = null,
)

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val classifyMediaUseCase: ClassifyMediaUseCase,
    private val manageFolderUseCase: ManageFolderUseCase,
) : ViewModel() {

    private val folderId: String = checkNotNull(savedStateHandle["folderId"])

    private val _uiState = MutableStateFlow(FolderDetailUiState())
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val all = classifyMediaUseCase()
                val folder = all.find { it.id == folderId }
                _uiState.update { it.copy(isLoading = false, folder = folder, allFolders = all) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: e.toString()) }
            }
        }
    }

    // ── 选择 ──────────────────────────────────────────────────────────────────

    fun toggleItem(item: MediaItem) {
        _uiState.update { state ->
            val newIds = if (item.id in state.selectedIds)
                state.selectedIds - item.id
            else
                state.selectedIds + item.id
            state.copy(
                selectedIds = newIds,
                isSelectMode = newIds.isNotEmpty(),
            )
        }
    }

    fun enterSelectMode(item: MediaItem) {
        _uiState.update { it.copy(selectedIds = setOf(item.id), isSelectMode = true) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectMode = false) }
    }

    fun selectAll() {
        _uiState.update { state ->
            val ids = state.folder?.items?.map { it.id }?.toSet() ?: emptySet()
            state.copy(selectedIds = ids, isSelectMode = ids.isNotEmpty())
        }
    }

    // ── 操作 Dialog 控制 ───────────────────────────────────────────────────────

    fun showMoveDialog() = _uiState.update { it.copy(showMoveDialog = true) }
    fun hideMoveDialog() = _uiState.update { it.copy(showMoveDialog = false) }
    fun showCopyDialog() = _uiState.update { it.copy(showCopyDialog = true) }
    fun hideCopyDialog() = _uiState.update { it.copy(showCopyDialog = false) }
    fun showDeleteConfirm() = _uiState.update { it.copy(showDeleteConfirm = true) }
    fun hideDeleteConfirm() = _uiState.update { it.copy(showDeleteConfirm = false) }

    // ── 移动 ──────────────────────────────────────────────────────────────────

    fun moveToFolder(targetFolder: PhotoFolder) {
        viewModelScope.launch {
            val items = selectedItems()
            manageFolderUseCase.moveToCustomFolder(
                folderId = targetFolder.customFolderId ?: return@launch,
                folderName = targetFolder.name,
                items = items,
            )
            hideMoveDialog()
            clearSelection()
            load()
        }
    }

    /** 单张图片直接移动（不经过多选模式）*/
    fun moveSingleItem(item: MediaItem, targetFolder: PhotoFolder) {
        viewModelScope.launch {
            manageFolderUseCase.moveToCustomFolder(
                folderId = targetFolder.customFolderId ?: return@launch,
                folderName = targetFolder.name,
                items = listOf(item),
            )
            load()
        }
    }

    // ── 复制 ──────────────────────────────────────────────────────────────────

    fun copyToFolder(targetFolder: PhotoFolder) {
        viewModelScope.launch {
            val items = selectedItems()
            manageFolderUseCase.copyToCustomFolder(
                folderId = targetFolder.customFolderId ?: return@launch,
                folderName = targetFolder.name,
                items = items,
            )
            hideCopyDialog()
            clearSelection()
            load()
        }
    }

    // ── 删除 ──────────────────────────────────────────────────────────────────

    /** 低版本直接删，Android 11+ 返回 IntentSenderRequest 给 UI 层弹系统确认 */
    fun confirmDelete() {
        viewModelScope.launch {
            val uris = selectedItems().map { it.uri }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sender = manageFolderUseCase.getDeleteRequest(uris)
                if (sender != null) {
                    _uiState.update {
                        it.copy(
                            showDeleteConfirm = false,
                            pendingDeleteRequest = IntentSenderRequest.Builder(sender.intentSender).build()
                        )
                    }
                    return@launch
                }
            }
            // API < 30: direct delete
            manageFolderUseCase.deleteMedia(uris)
            hideDeleteConfirm()
            clearSelection()
            load()
        }
    }

    /** 系统删除弹窗完成后回调 */
    fun onSystemDeleteResult(success: Boolean) {
        _uiState.update { it.copy(pendingDeleteRequest = null) }
        if (success) {
            clearSelection()
            load()
        }
    }

    // ── 内部 ──────────────────────────────────────────────────────────────────

    private fun selectedItems(): List<MediaItem> {
        val ids = _uiState.value.selectedIds
        return _uiState.value.folder?.items?.filter { it.id in ids } ?: emptyList()
    }

    /** 可供移动/复制的目标文件夹（自建文件夹，排除自身） */
    fun availableTargetFolders(): List<PhotoFolder> =
        _uiState.value.allFolders.filter {
            it.type == FolderType.CUSTOM && it.id != folderId
        }
}
