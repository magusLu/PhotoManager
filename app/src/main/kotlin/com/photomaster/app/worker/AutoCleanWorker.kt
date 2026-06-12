package com.photomaster.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photomaster.app.data.local.CleanRuleDao
import com.photomaster.app.domain.model.FolderType
import com.photomaster.app.domain.usecase.ClassifyMediaUseCase
import com.photomaster.app.domain.usecase.ManageFolderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

private const val TAG = "AutoCleanWorker"

/**
 * 每天凌晨检查并执行到期的自动清理规则。
 *
 * 注意（Android 11+ / API 30+）：
 *   Worker 在后台无法弹出系统删除确认框，
 *   只能删除 App 自己写入的文件（如移动到自建文件夹的图片）。
 *   相机、截图等系统媒体在 API 30+ 上无法后台直接删除，会被跳过。
 */
@HiltWorker
class AutoCleanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cleanRuleDao: CleanRuleDao,
    private val classifyMediaUseCase: ClassifyMediaUseCase,
    private val manageFolderUseCase: ManageFolderUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val rules = cleanRuleDao.getEnabledRules()
            if (rules.isEmpty()) {
                Log.d(TAG, "No enabled rules, skipping.")
                return Result.success()
            }

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val todayWeekDay = cal.get(Calendar.DAY_OF_WEEK).let {
                // Calendar: 1=日 2=一 … 7=六 → 转为 1=一 … 7=日
                if (it == Calendar.SUNDAY) 7 else it - 1
            }
            val todayMonthDay = cal.get(Calendar.DAY_OF_MONTH)

            // 获取所有文件夹（一次查询，所有规则共用）
            val allFolders = try {
                classifyMediaUseCase()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to classify media", e)
                return Result.retry()
            }

            for (rule in rules) {
                // 判断今天是否是该规则的执行日
                val isDue = when (rule.scheduleType) {
                    "DAILY" -> true
                    "WEEKLY" -> rule.weekDay == todayWeekDay
                    "MONTHLY" -> rule.monthDay == todayMonthDay
                    else -> {
                        Log.w(TAG, "Unknown scheduleType: ${rule.scheduleType}")
                        false
                    }
                }
                if (!isDue) continue

                // 避免同一天重复执行
                if (rule.lastRunAt > 0) {
                    val lastRunCal = Calendar.getInstance().apply { timeInMillis = rule.lastRunAt }
                    val todayCal = Calendar.getInstance()
                    val alreadyRanToday =
                        lastRunCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        lastRunCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                    if (alreadyRanToday) {
                        Log.d(TAG, "Rule ${rule.id} already ran today, skipping.")
                        continue
                    }
                }

                // 找到匹配类型的所有文件夹
                val targetType = try {
                    FolderType.valueOf(rule.folderType)
                } catch (_: IllegalArgumentException) {
                    Log.w(TAG, "Invalid folderType: ${rule.folderType}, skipping rule ${rule.id}")
                    continue
                }

                val matchedFolders = allFolders.filter { it.type == targetType }
                var deletedCount = 0
                var skippedCount = 0

                for (folder in matchedFolders) {
                    val uris = folder.items.map { it.uri }
                    if (uris.isEmpty()) continue
                    // deleteMedia 在 API 30+ 上对无权限文件会返回 false 而非 crash
                    val success = manageFolderUseCase.deleteMedia(uris)
                    if (success) deletedCount += uris.size else skippedCount += uris.size
                }

                Log.i(TAG, "Rule ${rule.id} (${rule.folderType}): deleted=$deletedCount, skipped=$skippedCount")
                cleanRuleDao.markRuleRun(rule.id, now)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "AutoCleanWorker failed, will retry", e)
            Result.retry()
        }
    }
}
