package com.photomaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 自建文件夹与 MediaStore 媒体 ID 的映射关系。
 *
 * @param folderId   所属自建文件夹 ID
 * @param mediaId    MediaStore._ID
 * @param isCopy     true = 复制操作（原文件保留原位）；false = 移动操作（通过 ContentResolver 更新路径）
 * @param addedAt    加入时间戳（ms）
 */
@Entity(
    tableName = "folder_media_mappings",
    primaryKeys = ["folderId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = CustomFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId"), Index("mediaId")]
)
data class FolderMediaMappingEntity(
    val folderId: Long,
    val mediaId: Long,
    val isCopy: Boolean,
    val addedAt: Long = System.currentTimeMillis(),
)
