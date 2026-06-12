package com.photomaster.app.domain.usecase

import com.photomaster.app.data.MediaStoreRepository
import com.photomaster.app.data.local.CustomFolderDao
import com.photomaster.app.domain.model.FolderType
import com.photomaster.app.domain.model.MediaItem
import com.photomaster.app.domain.model.PhotoFolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 扫描 MediaStore 并按规则生成 [PhotoFolder] 列表：
 *
 * - **拍摄_{yyyy-MM-dd}**：DCIM/Camera 下的图片，按日期分组
 * - **截图_{yyyy-MM-dd}**：截图，按日期分组
 * - **视频_{yyyy-MM-dd}**：DCIM/Camera 下的视频，按日期分组
 * - **图片_{AppName}**：其余三方 App 来源图片，按包名聚合
 * - 自建文件夹：从 Room DB 加载，关联 MediaStore ID
 */
class ClassifyMediaUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val customFolderDao: CustomFolderDao,
) {
    // ThreadLocal to avoid SimpleDateFormat concurrency issues in coroutines
    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    suspend operator fun invoke(): List<PhotoFolder> {
        val allMedia = mediaStoreRepository.queryAllMedia()
        val folders = mutableListOf<PhotoFolder>()

        // ── 1. 相机照片 ───────────────────────────────────────────────────────
        val cameraPhotos = allMedia.filter { it.isCameraPhoto }
        cameraPhotos
            .groupBy { dateFormat.get()!!.format(Date(it.dateTaken)) }
            .forEach { (date, items) ->
                folders += PhotoFolder(
                    id = "camera_$date",
                    name = "拍摄_$date",
                    type = FolderType.CAMERA,
                    items = items.sortedByDescending { it.dateTaken },
                    coverUri = items.firstOrNull()?.uri,
                )
            }

        // ── 2. 截图 ───────────────────────────────────────────────────────────
        val screenshots = allMedia.filter { it.isScreenshot && it.isImage }
        screenshots
            .groupBy { dateFormat.get()!!.format(Date(it.dateTaken)) }
            .forEach { (date, items) ->
                folders += PhotoFolder(
                    id = "screenshot_$date",
                    name = "截图_$date",
                    type = FolderType.SCREENSHOT,
                    items = items.sortedByDescending { it.dateTaken },
                    coverUri = items.firstOrNull()?.uri,
                )
            }

        // ── 3. 相机视频 ───────────────────────────────────────────────────────
        val cameraVideos = allMedia.filter { it.isCameraVideo }
        cameraVideos
            .groupBy { dateFormat.get()!!.format(Date(it.dateTaken)) }
            .forEach { (date, items) ->
                folders += PhotoFolder(
                    id = "video_$date",
                    name = "视频_$date",
                    type = FolderType.VIDEO,
                    items = items.sortedByDescending { it.dateTaken },
                    coverUri = items.firstOrNull()?.uri,
                )
            }

        // ── 4. 三方 App 图片 ──────────────────────────────────────────────────
        val thirdPartyImages = allMedia.filter { item ->
            item.isImage &&
                    !item.isCameraPhoto &&
                    !item.isScreenshot &&
                    item.ownerPackageName != null &&
                    !mediaStoreRepository.isSystemCameraPackage(item.ownerPackageName)
        }
        thirdPartyImages
            .groupBy { it.ownerPackageName ?: "未知应用" }
            .forEach { (pkg, items) ->
                val appName = mediaStoreRepository.getAppLabel(pkg)
                folders += PhotoFolder(
                    id = "third_$pkg",
                    name = "图片_$appName",
                    type = FolderType.THIRD_PARTY,
                    items = items.sortedByDescending { it.dateTaken },
                    coverUri = items.firstOrNull()?.uri,
                )
            }

        // ── 5. 自建文件夹 ─────────────────────────────────────────────────────
        val mediaById = allMedia.associateBy { it.id }
        val customFolders = customFolderDao.getAllFolders()
        customFolders.forEach { folderEntity ->
            val mappings = customFolderDao.getMappingsByFolder(folderEntity.id)
            val folderItems = mappings.mapNotNull { mediaById[it.mediaId] }
                .sortedByDescending { it.dateTaken }
            folders += PhotoFolder(
                id = "custom_${folderEntity.id}",
                name = folderEntity.name,
                type = FolderType.CUSTOM,
                items = folderItems,
                coverUri = folderItems.firstOrNull()?.uri,
                customFolderId = folderEntity.id,
            )
        }

        // 整体按最新一张图的时间倒序排，CUSTOM 文件夹排最后
        return folders.sortedWith(
            compareBy<PhotoFolder> { it.type == FolderType.CUSTOM }
                .thenByDescending { it.items.firstOrNull()?.dateTaken ?: 0L }
        )
    }

    /** 只返回今天的文件夹 */
    suspend fun invokeToday(): List<PhotoFolder> {
        val today = dateFormat.get()!!.format(Date())
        return invoke().filter { folder ->
            when (folder.type) {
                FolderType.CAMERA -> folder.name == "拍摄_$today"
                FolderType.SCREENSHOT -> folder.name == "截图_$today"
                FolderType.VIDEO -> folder.name == "视频_$today"
                else -> false
            }
        }
    }
}
