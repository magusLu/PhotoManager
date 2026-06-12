package com.photomaster.app.domain.usecase

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import com.photomaster.app.data.MediaStoreRepository
import com.photomaster.app.data.local.CustomFolderDao
import com.photomaster.app.data.local.entity.CustomFolderEntity
import com.photomaster.app.data.local.entity.FolderMediaMappingEntity
import com.photomaster.app.domain.model.MediaItem
import javax.inject.Inject

/**
 * 文件夹管理：创建、重命名、删除自建文件夹，以及移动/复制图片进文件夹。
 */
class ManageFolderUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val customFolderDao: CustomFolderDao,
) {

    // ── 自建文件夹 CRUD ───────────────────────────────────────────────────────

    suspend fun createFolder(name: String): Long =
        customFolderDao.insertFolder(CustomFolderEntity(name = name))

    suspend fun renameFolder(folderId: Long, newName: String) =
        customFolderDao.renameFolder(folderId, newName)

    suspend fun deleteFolder(folderId: Long) =
        customFolderDao.deleteFolder(folderId) // CASCADE 自动删除 mappings

    // ── 添加图片到自建文件夹 ────────────────────────────────────────────────────

    /**
     * 将图片"移动"到自建文件夹（更新 RELATIVE_PATH + 记录映射）。
     * 自建文件夹对应系统路径：Pictures/PhotoMaster/{folderName}/
     */
    suspend fun moveToCustomFolder(
        folderId: Long,
        folderName: String,
        items: List<MediaItem>,
    ): List<Long> {
        val succeeded = mutableListOf<Long>()
        val targetPath = "Pictures/PhotoMaster/$folderName/"
        items.forEach { item ->
            val ok = mediaStoreRepository.moveMedia(item.uri, targetPath)
            if (ok) {
                customFolderDao.insertMappings(
                    listOf(FolderMediaMappingEntity(folderId = folderId, mediaId = item.id, isCopy = false))
                )
                succeeded += item.id
            }
        }
        return succeeded
    }

    /**
     * 将图片"复制"到自建文件夹（写入新文件 + 记录映射）。
     */
    suspend fun copyToCustomFolder(
        folderId: Long,
        folderName: String,
        items: List<MediaItem>,
    ): List<Long> {
        val succeeded = mutableListOf<Long>()
        val targetPath = "Pictures/PhotoMaster/$folderName/"
        items.forEach { item ->
            val newUri = mediaStoreRepository.copyMedia(
                sourceUri = item.uri,
                newRelativePath = targetPath,
                displayName = item.displayName,
                mimeType = item.mimeType,
            )
            if (newUri != null) {
                // 查询新文件的 ID；ID 无效时跳过（避免写入 -1L 到数据库）
                val newId = extractMediaId(newUri)
                if (newId > 0) {
                    customFolderDao.insertMappings(
                        listOf(FolderMediaMappingEntity(folderId = folderId, mediaId = newId, isCopy = true))
                    )
                    succeeded += newId
                }
            }
        }
        return succeeded
    }

    /**
     * 从自建文件夹移除图片（不删除实际文件）。
     */
    suspend fun removeFromCustomFolder(folderId: Long, mediaIds: List<Long>) =
        customFolderDao.removeMappings(folderId, mediaIds)

    // ── 删除图片（从手机删除）────────────────────────────────────────────────

    /**
     * Android 11+ 返回 null，需 UI 层用 [getDeleteRequest] 弹系统确认；
     * 低版本直接删除返回 true。
     */
    suspend fun deleteMedia(uris: List<Uri>): Boolean =
        mediaStoreRepository.deleteMedia(uris)

    @SuppressLint("NewApi")
    fun getDeleteRequest(uris: List<Uri>) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            mediaStoreRepository.getDeleteRequest(uris)
        else null

    // ── 内部 ──────────────────────────────────────────────────────────────────

    private fun extractMediaId(uri: Uri): Long =
        uri.lastPathSegment?.toLongOrNull() ?: -1L
}
