package com.photomaster.app.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photomaster.app.domain.model.MediaItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    items: List<MediaItem>,
    selectedIds: Set<Long>,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
    ) {
        items(items, key = { it.id }) { item ->
            MediaGridItem(
                item = item,
                isSelected = item.id in selectedIds,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridItem(
    item: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .then(
                if (isSelected)
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // 视频标识
        if (item.isVideo) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = "视频",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            )
        }

        // 选中遮罩
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}
