package com.photomaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自建文件夹元数据。
 * 文件夹内的图片通过 [FolderMediaMappingEntity] 关联，
 * 实际媒体文件仍存于系统 MediaStore。
 */
@Entity(tableName = "custom_folders")
data class CustomFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)
