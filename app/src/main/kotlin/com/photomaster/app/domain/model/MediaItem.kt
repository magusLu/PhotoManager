package com.photomaster.app.domain.model

import android.net.Uri

data class MediaItem(
    /** MediaStore._ID */
    val id: Long,
    /** Content URI (content://media/external/images/media/id) */
    val uri: Uri,
    /** 文件显示名 */
    val displayName: String,
    /** 拍摄/创建时间戳（ms），来自 DATE_TAKEN 或 DATE_ADDED */
    val dateTaken: Long,
    /** MIME type，如 image/jpeg, video/mp4 */
    val mimeType: String,
    /** 相对路径，如 DCIM/Camera/ */
    val relativePath: String,
    /** 来源包名（API 29+），null 表示不可用 */
    val ownerPackageName: String?,
    /** 是否截图（API 29+） */
    val isScreenshot: Boolean,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isCameraPhoto: Boolean
        get() = isImage &&
                (relativePath.startsWith("DCIM/Camera") ||
                        relativePath.startsWith("DCIM/camera"))
    val isCameraVideo: Boolean
        get() = isVideo &&
                (relativePath.startsWith("DCIM/Camera") ||
                        relativePath.startsWith("DCIM/camera"))
}
