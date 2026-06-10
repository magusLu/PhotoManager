package com.photomaster.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photomaster.app.ui.components.FolderCard
import com.photomaster.app.ui.components.MediaPermissionHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFolderClick: (folderId: String, folderName: String) -> Unit,
    onTransferClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    MediaPermissionHandler {
        HomeContent(
            onFolderClick = onFolderClick,
            onTransferClick = onTransferClick,
            viewModel = viewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    onFolderClick: (folderId: String, folderName: String) -> Unit,
    onTransferClick: () -> Unit,
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayedFolders = viewModel.filteredFolders()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PhotoMaster") },
                actions = {
                    IconButton(onClick = onTransferClick) {
                        Icon(Icons.Filled.WifiTethering, contentDescription = "传到电脑")
                    }
                    IconButton(onClick = { viewModel.loadFolders() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB 仅在"自建"Tab 显示
            if (uiState.selectedTab == HomeTab.CUSTOM) {
                FloatingActionButton(onClick = { viewModel.showCreateFolderDialog() }) {
                    Icon(Icons.Filled.Add, contentDescription = "新建文件夹")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            // Tab 栏
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                HomeTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = uiState.selectedTab.ordinal == index,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    HomeTab.TODAY -> "今天"
                                    HomeTab.ALL -> "全部"
                                    HomeTab.CUSTOM -> "自建"
                                }
                            )
                        }
                    )
                }
            }

            // 内容区
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("加载失败：${uiState.error}")
                            Button(
                                onClick = { viewModel.loadFolders() },
                                modifier = Modifier.padding(top = 8.dp)
                            ) { Text("重试") }
                        }
                    }
                }

                displayedFolders.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            when (uiState.selectedTab) {
                                HomeTab.TODAY -> "今天还没有新图片 📷"
                                HomeTab.ALL -> "暂无图片，请先授权并确认相册有内容"
                                HomeTab.CUSTOM -> "还没有自建文件夹，点击右下角 + 创建"
                            }
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(displayedFolders, key = { it.id }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { onFolderClick(folder.id, folder.name) },
                            )
                        }
                    }
                }
            }
        }
    }

    // 创建文件夹 Dialog
    if (uiState.showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { viewModel.createFolder(it) },
            onDismiss = { viewModel.hideCreateFolderDialog() }
        )
    }
}

@Composable
private fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("文件夹名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
