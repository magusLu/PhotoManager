package com.photomaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 自动清理规则。
 *
 * @param folderType  适用的文件夹类型字符串（对应 FolderType.name）
 * @param scheduleType  调度类型：DAILY / WEEKLY / MONTHLY
 * @param weekDay  WEEKLY 时：1=周一…7=周日（0 表示不适用）
 * @param monthDay  MONTHLY 时：1-31（0 表示不适用）
 * @param enabled  是否启用
 * @param lastRunAt  上次执行时间戳（毫秒），0=从未执行
 */
@Entity(tableName = "clean_rules")
data class CleanRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderType: String,
    val scheduleType: String,   // DAILY | WEEKLY | MONTHLY
    val weekDay: Int = 0,       // 1-7，WEEKLY 有效
    val monthDay: Int = 0,      // 1-31，MONTHLY 有效
    val enabled: Boolean = true,
    val lastRunAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
