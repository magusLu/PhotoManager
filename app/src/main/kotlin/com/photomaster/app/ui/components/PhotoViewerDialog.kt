package com.photomaster.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.photomaster.app.domain.model.MediaItem
import com.photomaster.app.domain.model.PhotoFolder
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Full-screen photo viewer with pinch-to-zoom, double-tap zoom, close and move buttons.
 *
 * @param item            被预览的媒体项
 * @param availableFolders 可移动到的自建文件夹列表（为空时移动按钮不显示）
 * @param onMove          用户选择目标文件夹后的回调
 * @param onDismiss       关闭预览
 */
@Composable
fun PhotoViewerDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
    availableFolders: List<PhotoFolder> = emptyList(),
    onMove: ((PhotoFolder) -> Unit)? = null,
) {
    var showMoveDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val zoomState = rememberZoomState()

            CoilImage(
                imageModel = { item.uri },
                imageOptions = ImageOptions(contentScale = ContentScale.Fit),
                component = rememberImageComponent {
                    +CrossfadePlugin(duration = 300)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(zoomState),
            )

            // Top-end button row: move + close
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                // Move button（仅有可用目标文件夹且提供 onMove 回调时显示）
                if (onMove != null && availableFolders.isNotEmpty()) {
                    IconButton(
                        onClick = { showMoveDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DriveFileMove,
                            contentDescription = "移动到",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    // 移动目标选择弹窗
    if (showMoveDialog && onMove != null) {
        FolderPickerDialog(
            title = "移动到",
            folders = availableFolders,
            onSelect = { folder ->
                showMoveDialog = false
                onMove(folder)
                onDismiss()   // 移动后关闭预览
            },
            onDismiss = { showMoveDialog = false },
        )
    }
}
