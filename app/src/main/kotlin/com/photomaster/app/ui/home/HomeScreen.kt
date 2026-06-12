package com.photomaster.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.photomaster.app.domain.model.PhotoFolder
import com.photomaster.app.ui.components.FolderCard
import com.photomaster.app.ui.components.MediaPermissionHandler
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFolderClick: (folderId: String, folderName: String) -> Unit,
    onTransferClick: () -> Unit = {},
    onCleanRulesClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    MediaPermissionHandler {
        HomeContent(
            onFolderClick = onFolderClick,
            onTransferClick = onTransferClick,
            onCleanRulesClick = onCleanRulesClick,
            viewModel = viewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    onFolderClick: (folderId: String, folderName: String) -> Unit,
    onTransferClick: () -> Unit,
    onCleanRulesClick: () -> Unit,
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    // Use derivedStateOf to avoid re-filtering on every irrelevant recomposition
    val displayedFolders by remember {
        androidx.compose.runtime.derivedStateOf { viewModel.filteredFolders() }
    }
    val density = LocalDensity.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ── 拖放状态 ──────────────────────────────────────────────────────────────
    // 当前正在被拖动的文件夹
    var draggingFolder by remember { mutableStateOf<PhotoFolder?>(null) }
    // 拖动时手指在屏幕上的绝对位置
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // 拖动开始时的位置（用于计算相对偏移）
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    // 「自建」Tab 在屏幕上的绝对范围（用于判断 drop 是否命中）
    var customTabBounds by remember { mutableStateOf(Rect.Zero) }
    // 是否悬停在「自建」Tab 上
    val isOverCustomTab = draggingFolder != null &&
            customTabBounds != Rect.Zero &&
            customTabBounds.contains(dragOffset)

    // Snackbar 显示移动结果
    LaunchedEffect(uiState.dragMoveResult) {
        uiState.dragMoveResult?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            delay(200)
            viewModel.clearDragMoveResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PhotoMaster",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = onCleanRulesClick) {
                        Icon(Icons.Filled.AutoDelete, contentDescription = "自动清理设置")
                    }
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
            AnimatedVisibility(
                visible = uiState.selectedTab == HomeTab.CUSTOM,
                enter = scaleIn(animationSpec = tween(220)),
                exit = scaleOut(animationSpec = tween(180)),
            ) {
                FloatingActionButton(
                    onClick = { viewModel.showCreateFolderDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "新建文件夹")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // 用 Box 包裹，使拖动浮层可以覆盖整个内容区
        Box(modifier = Modifier.padding(innerPadding)) {

            Column(modifier = Modifier.fillMaxSize()) {

                // Tab bar
                TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                    HomeTab.entries.forEachIndexed { index, tab ->
                        val isCustomTab = tab == HomeTab.CUSTOM
                        Tab(
                            selected = uiState.selectedTab.ordinal == index,
                            onClick = { viewModel.selectTab(tab) },
                            modifier = if (isCustomTab) {
                                Modifier.onGloballyPositioned { coords ->
                                    customTabBounds = coords.boundsInWindow()
                                }
                            } else Modifier,
                            text = {
                                // 「自建」Tab 悬停高亮
                                val highlighted = isCustomTab && isOverCustomTab
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = if (highlighted) Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                    else Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when (tab) {
                                            HomeTab.TODAY -> "今天"
                                            HomeTab.ALL -> "全部"
                                            HomeTab.CUSTOM -> if (highlighted) "📂 放这里" else "自建"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (highlighted)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        )
                    }
                }

                when {
                    uiState.isLoading -> {
                        ShimmerFolderGrid()
                    }

                    uiState.error != null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "加载失败：${uiState.error}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Button(
                                    onClick = { viewModel.loadFolders() },
                                    modifier = Modifier.padding(top = 12.dp),
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
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(displayedFolders, key = { it.id }) { folder ->
                                val isDragging = draggingFolder?.id == folder.id

                                Box(
                                    modifier = Modifier
                                        // 长按触发拖动
                                        .pointerInput(folder.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { startPos ->
                                                    draggingFolder = folder
                                                    dragStartOffset = startPos
                                                    dragOffset = startPos
                                                },
                                                onDrag = { _, dragAmount ->
                                                    dragOffset += dragAmount
                                                },
                                                onDragEnd = {
                                                    if (isOverCustomTab) {
                                                        draggingFolder?.let { viewModel.moveFolderToCustom(it) }
                                                    }
                                                    draggingFolder = null
                                                    dragOffset = Offset.Zero
                                                },
                                                onDragCancel = {
                                                    draggingFolder = null
                                                    dragOffset = Offset.Zero
                                                },
                                            )
                                        }
                                        // 拖动中原位置半透明
                                        .alpha(if (isDragging) 0.35f else 1f)
                                ) {
                                    FolderCard(
                                        folder = folder,
                                        onClick = { onFolderClick(folder.id, folder.name) },
                                        onDelete = { viewModel.requestDeleteFolderWithMedia(folder) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 拖动浮层：跟随手指移动的文件夹缩略图 ─────────────────────────
            draggingFolder?.let { folder ->
                val cardWidthDp = 160.dp
                val cardWidthPx = with(density) { cardWidthDp.toPx() }
                val cardHeightPx = cardWidthPx / 1.4f + with(density) { 48.dp.toPx() }

                Box(
                    modifier = Modifier
                        .zIndex(10f)
                        .offset {
                            IntOffset(
                                x = (dragOffset.x - cardWidthPx / 2).roundToInt(),
                                y = (dragOffset.y - cardHeightPx / 2).roundToInt(),
                            )
                        }
                        .width(cardWidthDp)
                        .scale(1.08f)
                        .alpha(0.88f)
                ) {
                    FolderCard(
                        folder = folder,
                        onClick = {},
                    )
                }

                // 提示文字
                if (isOverCustomTab) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "松开移动到自建",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }

    if (uiState.showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { viewModel.createFolder(it) },
            onDismiss = { viewModel.hideCreateFolderDialog() },
        )
    }

    // 删除文件夹（含图片）确认弹窗
    uiState.pendingDeleteFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteFolderWithMedia() },
            title = { Text("删除文件夹") },
            text = {
                Text(
                    "将从手机相册永久删除「${folder.name}」文件夹及其中 ${folder.count} 张图片，操作不可撤销。确定继续？"
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { viewModel.confirmDeleteFolderWithMedia() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                ) { Text("删除", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteFolderWithMedia() }) {
                    Text("取消")
                }
            }
        )
    }
}

/** Shimmer skeleton shown while folders are loading */
@Composable
private fun ShimmerFolderGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .shimmer(),
        userScrollEnabled = false,
    ) {
        items(6) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .drawBehind { drawRect(Color(0xFFE0E0E0)) }
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .drawBehind { drawRect(Color(0xFFE0E0E0)) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
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
                shape = RoundedCornerShape(12.dp),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
