package com.photomaster.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.photomaster.app.domain.model.PhotoFolder

/**
 * 文件夹选择弹窗，供「移动到」和「复制到」功能复用。
 */
@Composable
fun FolderPickerDialog(
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
