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

        return resolver.query(contentUri, projection, null, null, sortOrder)
            ?.use { cursor ->
                val result = mutableListOf<MediaItem>()
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val ownerCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    cursor.getColumnIndex(MediaStore.MediaColumns.OWNER_PACKAGE_NAME) else -1
                val screenshotCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    cursor.getColumnIndex(MediaStore.MediaColumns.IS_SCREENSHOT) else -1

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
            base + arrayOf(
                MediaStore.MediaColumns.OWNER_PACKAGE_NAME,
                MediaStore.MediaColumns.IS_SCREENSHOT,
            )
        } else base
    }

    /**
     * 删除一批媒体文件（直接从手机删除）。
     * Android 10+ 会弹系统确认弹窗，需从 Activity 调用 [getDeleteRequest] 处理。
     * Android 9 及以下：直接 delete。
     */
    suspend fun deleteMedia(uris: List<Uri>): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Caller should use MediaStore.createDeleteRequest — return flag to signal
            return@withContext false // handled by UI layer
        }
        uris.forEach { resolver.delete(it, null, null) }
        true
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
