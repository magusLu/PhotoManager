package com.photomaster.app.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.photomaster.app.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 与系统 MediaStore 交互的唯一入口。
 * 负责：查询、删除、移动（更新 RELATIVE_PATH）、复制媒体文件。
 */
@Singleton
class MediaStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    // ── 系统相机包名（用于过滤三方 App） ───────────────────────────────────────
    private val systemCameraPackages = setOf(
        "com.android.camera",
        "com.android.camera2",
        "com.google.android.GoogleCamera",
        "com.google.android.apps.cameralite",
        "com.huawei.camera",
        "com.samsung.android.app.camera",
        "com.miui.camera",
        "com.oppo.camera",
        "com.vivo.camera",
        "com.oneplus.camera",
        "com.sonyericsson.android.camera",
        "com.lge.camera",
    )

    /**
     * 查询所有图片和视频，返回 [MediaItem] 列表，按拍摄时间降序。
     */
    suspend fun queryAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        items += queryFromUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        items += queryFromUri(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        items.sortByDescending { it.dateTaken }
        items
    }

    private fun queryFromUri(contentUri: Uri): List<MediaItem> {
        val projection = buildProjection()
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

        return try {
            resolver.query(contentUri, projection, null, null, sortOrder)
        } catch (e: Exception) {
            null // Some devices/APIs may throw SecurityException
        }?.use { cursor ->
                val result = mutableListOf<MediaItem>()
                // Use getColumnIndex (not OrThrow) for graceful handling on unusual devices
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID).takeIf { it >= 0 } ?: return emptyList()
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME).takeIf { it >= 0 } ?: return emptyList()
                val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH).takeIf { it >= 0 } ?: return emptyList()
                val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN).takeIf { it >= 0 } ?: return emptyList()
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE).takeIf { it >= 0 } ?: return emptyList()
                val ownerCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    cursor.getColumnIndex(MediaStore.MediaColumns.OWNER_PACKAGE_NAME) else -1
                // IS_SCREENSHOT column unreliable across devices/emulators; use path fallback
                val screenshotCol = -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val mimeType = cursor.getString(mimeCol) ?: continue
                    val uri = ContentUris.withAppendedId(contentUri, id)
                    val relativePath = cursor.getString(pathCol) ?: ""
                    val ownerPkg = if (ownerCol >= 0) cursor.getString(ownerCol) else null
                    val isScreenshot = when {
                        screenshotCol >= 0 -> cursor.getInt(screenshotCol) == 1
                        else -> relativePath.contains("screenshot", ignoreCase = true) ||
                                relativePath.contains("Screenshot", ignoreCase = true)
                    }
                    result += MediaItem(
                        id = id,
                        uri = uri,
                        displayName = cursor.getString(nameCol) ?: "",
                        dateTaken = cursor.getLong(dateCol).let { if (it == 0L) System.currentTimeMillis() else it },
                        mimeType = mimeType,
                        relativePath = relativePath,
                        ownerPackageName = ownerPkg,
                        isScreenshot = isScreenshot,
                    )
                }
                result
            } ?: emptyList()
    }

    private fun buildProjection(): Array<String> {
        val base = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            base + arrayOf(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
        } else base
    }

    /**
     * 删除一批媒体文件。
     * - API < 30：直接删除，返回 true。
     * - API 30+（Android 11+）：尝试直接删除（自建文件夹内、App 自己写入的文件可以直接删），
     *   如果权限不足则返回 false，调用方应使用 [getDeleteRequest] 弹系统确认。
     *   Worker 场景（无 UI）：在 API 30+ 上会跳过无权限的文件，不 crash。
     */
    suspend fun deleteMedia(uris: List<Uri>): Boolean = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // On API 30+ we try direct delete; files we created can be deleted directly.
            // Files from camera/other apps will throw RecoverableSecurityException.
            var allSucceeded = true
            uris.forEach { uri ->
                try {
                    val deleted = resolver.delete(uri, null, null)
                    if (deleted == 0) allSucceeded = false
                } catch (_: SecurityException) {
                    // No permission to delete this file; caller may use getDeleteRequest
                    allSucceeded = false
                } catch (_: Exception) {
                    allSucceeded = false
                }
            }
            return@withContext allSucceeded
        }
        try {
            uris.forEach { resolver.delete(it, null, null) }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Android 11+ 批量删除 IntentSender（在 UI 层 startIntentSenderForResult 调用）。
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun getDeleteRequest(uris: List<Uri>) =
        MediaStore.createDeleteRequest(resolver, uris)

    /**
     * 移动媒体到新路径（仅修改 RELATIVE_PATH，不复制文件内容）。
     * 要求 API 29+，同卷操作。
     */
    suspend fun moveMedia(uri: Uri, newRelativePath: String): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, newRelativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        try {
            resolver.update(uri, values, null, null)
            val clear = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, clear, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 复制媒体到新路径（读取字节流 → 写入新 URI）。
     */
    suspend fun copyMedia(sourceUri: Uri, newRelativePath: String, displayName: String, mimeType: String): Uri? =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, newRelativePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val destUri = if (mimeType.startsWith("video/"))
                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            else
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (destUri == null) return@withContext null

            try {
                resolver.openInputStream(sourceUri)?.use { input ->
                    resolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val clear = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    resolver.update(destUri, clear, null, null)
                }
                destUri
            } catch (e: Exception) {
                resolver.delete(destUri, null, null)
                null
            }
        }

    /** 获取 App 显示名（三方 App 文件夹名使用）。 */
    fun getAppLabel(packageName: String): String =
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }

    fun isSystemCameraPackage(packageName: String?): Boolean =
        packageName == null || systemCameraPackages.any {
            packageName.equals(it, ignoreCase = true)
        }
}
