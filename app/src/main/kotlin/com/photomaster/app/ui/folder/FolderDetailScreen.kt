package com.photomaster.app.ui.folder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.photomaster.app.domain.model.PhotoFolder
import com.photomaster.app.ui.components.MediaGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: String,
    folderName: String,
    onBack: () -> Unit,
    viewModel: FolderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Android 11+ 系统删除弹窗 Launcher
    val deleteSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onSystemDeleteResult(
            result.resultCode == android.app.Activity.RESULT_OK
        )
    }

    // 当有 pendingDeleteRequest 时启动系统 Intent
    LaunchedEffect(uiState.pendingDeleteRequest) {
        uiState.pendingDeleteRequest?.let {
            deleteSystemLauncher.launch(it)
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedIds.size,
                    onClose = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll() },
                    onDelete = { viewModel.showDeleteConfirm() },
                    onMove = { viewModel.showMoveDialog() },
                    onCopy = { viewModel.showCopyDialog() },
                )
            } else {
                TopAppBar(
                    title = { Text(folderName) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.folder?.items?.isEmpty() == true -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("文件夹为空")
                    }
                }

                uiState.folder != null -> {
                    MediaGrid(
                        items = uiState.folder!!.items,
                        selectedIds = uiState.selectedIds,
                        onItemClick = { item ->
                            if (uiState.isSelectMode) viewModel.toggleItem(item)
                            // else open viewer (future enhancement)
                        },
                        onItemLongClick = { item ->
                            viewModel.enterSelectMode(item)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    // 删除确认弹窗
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("删除图片") },
            text = {
                Text(
                    "将从手机相册永久删除 ${uiState.selectedIds.size} 张图片，操作不可撤销。确定继续？"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) { Text("取消") }
            }
        )
    }

    // 移动到自建文件夹 Dialog
    if (uiState.showMoveDialog) {
        FolderPickerDialog(
            title = "移动到",
            folders = viewModel.availableTargetFolders(),
            onSelect = { viewModel.moveToFolder(it) },
            onDismiss = { viewModel.hideMoveDialog() },
        )
    }

    // 复制到自建文件夹 Dialog
    if (uiState.showCopyDialog) {
        FolderPickerDialog(
            title = "复制到",
            folders = viewModel.availableTargetFolders(),
            onSelect = { viewModel.copyToFolder(it) },
            onDismiss = { viewModel.hideCopyDialog() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
) {
    TopAppBar(
        title = { Text("已选 $selectedCount 张") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "取消选择")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Filled.SelectAll, contentDescription = "全选")
            }
            IconButton(onClick = onMove) {
                Icon(Icons.Filled.DriveFileMove, contentDescription = "移动")
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "复制")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun FolderPickerDialog(
    title: String,
    folders: List<PhotoFolder>,
    onSelect: (PhotoFolder) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (folders.isEmpty()) {
                Text("没有可用的自建文件夹，请先在首页创建。")
            } else {
                Column {
                    folders.forEach { folder ->
                        TextButton(onClick = { onSelect(folder) }) {
                            Text("📁 ${folder.name}（${folder.count} 张）")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
