package com.photomaster.app.domain.model

/**
 * 逻辑文件夹，展示给用户的分类单元。
 *
 * @param id         唯一标识。自动文件夹用 "type_date" 或 "pkg_name"；自建文件夹用 Room DB 主键字符串
 * @param name       显示名称，如 "拍摄_2024-01-15"
 * @param type       文件夹类型
 * @param items      当前文件夹包含的媒体项列表
 * @param coverUri   封面图 URI（取第一张）
 * @param customFolderId Room DB 中自建文件夹的 ID，CUSTOM 类型时不为 null
 */
data class PhotoFolder(
    val id: String,
    val name: String,
    val type: FolderType,
    val items: List<MediaItem>,
    val coverUri: android.net.Uri?,
    val customFolderId: Long? = null,
) {
    val count: Int get() = items.size
}
