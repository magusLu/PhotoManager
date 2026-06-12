package com.photomaster.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.photomaster.app.data.local.entity.CleanRuleEntity
import com.photomaster.app.data.local.entity.CustomFolderEntity
import com.photomaster.app.data.local.entity.FolderMediaMappingEntity

@Database(
    entities = [
        CustomFolderEntity::class,
        FolderMediaMappingEntity::class,
        CleanRuleEntity::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customFolderDao(): CustomFolderDao
    abstract fun cleanRuleDao(): CleanRuleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `clean_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `folderType` TEXT NOT NULL,
                        `scheduleType` TEXT NOT NULL,
                        `weekDay` INTEGER NOT NULL DEFAULT 0,
                        `monthDay` INTEGER NOT NULL DEFAULT 0,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `lastRunAt` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
