package com.photomaster.app.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * 权限请求封装。
 * 如果权限已授予，直接执行 [onGranted]。
 * 如果未授予，弹 Dialog 引导用户授权。
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPermissionHandler(
    onGranted: @Composable () -> Unit
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    when {
        permissionsState.allPermissionsGranted -> onGranted()

        permissionsState.shouldShowRationale ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text("需要访问图片权限") },
                text = { Text("PhotoMaster 需要读取您的相册来分类管理图片，请授权后继续使用。") },
                confirmButton = {
                    Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                        Text("去授权")
                    }
                }
            )

        else ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text("权限不足") },
                text = { Text("相册权限已被拒绝。请前往系统设置 → 应用 → PhotoMaster → 权限，手动开启。") },
                confirmButton = {
                    Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                        Text("重试")
                    }
                }
            )
    }
}
