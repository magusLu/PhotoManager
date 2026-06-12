package com.photomaster.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.photomaster.app.domain.model.MediaItem
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.shimmer.Shimmer
import com.skydoves.landscapist.placeholder.shimmer.ShimmerPlugin

/**
 * 媒体网格。
 *
 * @param isSelectMode   是否处于多选模式
 * @param onItemView     非多选时点击「预览」
 * @param onItemMove     非多选时点击「移动」；为 null 则不显示移动菜单项
 * @param onItemLongClick 长按进入多选模式
 * @param onItemToggle   多选模式下切换选中状态
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    items: List<MediaItem>,
    selectedIds: Set<Long>,
    isSelectMode: Boolean = false,
    onItemView: (MediaItem) -> Unit = {},
    onItemMove: ((MediaItem) -> Unit)? = null,
    onItemLongClick: (MediaItem) -> Unit = {},
    onItemToggle: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
    columns: Int = 3,
    // 兼容旧调用：单一 onItemClick 入口
    onItemClick: ((MediaItem) -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
    ) {
        items(items, key = { it.id }) { item ->
            MediaGridItem(
                item = item,
                isSelected = item.id in selectedIds,
                isSelectMode = isSelectMode,
                onView = { onItemView(item) },
                onMove = if (onItemMove != null) { { onItemMove(item) } } else null,
                onLongClick = { onItemLongClick(item) },
                onToggle = { onItemToggle(item) },
                // 兼容旧调用
                legacyOnClick = if (onItemClick != null) { { onItemClick(item) } } else null,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridItem(
    item: MediaItem,
    isSelected: Boolean,
    isSelectMode: Boolean,
    onView: () -> Unit,
    onMove: (() -> Unit)?,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
    legacyOnClick: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "item_scale",
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .scale(scale)
            .clip(RoundedCornerShape(if (isSelected) 10.dp else 4.dp))
            .then(
                if (isSelected)
                    Modifier.border(
                        width = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                    )
                else Modifier
            )
            .combinedClickable(
                onClick = {
                    when {
                        legacyOnClick != null -> legacyOnClick()   // 兼容旧路径
                        isSelectMode -> onToggle()
                        onMove != null -> menuExpanded = true       // 有移动能力时弹菜单
                        else -> onView()                            // 仅预览
                    }
                },
                onLongClick = onLongClick,
            )
    ) {
        CoilImage(
            imageModel = { item.uri },
            imageOptions = ImageOptions(contentScale = ContentScale.Crop),
            component = rememberImageComponent {
                +CrossfadePlugin(duration = 300)
                +ShimmerPlugin(
                    shimmer = Shimmer.Flash(
                        baseColor = Color(0xFFE0E0E0),
                        highlightColor = Color(0xFFF5F5F5),
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Video badge
        if (item.isVideo) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = "视频",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
            )
        }

        // Selected overlay + checkmark
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(22.dp)
                    .background(Color.White, CircleShape),
            )
        }

        // 单张操作菜单（非多选模式且有移动能力时）
        if (legacyOnClick == null && !isSelectMode && onMove != null) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("预览") },
                    leadingIcon = {
                        Icon(Icons.Filled.Visibility, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onView()
                    },
                )
                DropdownMenuItem(
                    text = { Text("移动到") },
                    leadingIcon = {
                        Icon(Icons.Filled.DriveFileMove, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onMove()
                    },
                )
            }
        }
    }
}
