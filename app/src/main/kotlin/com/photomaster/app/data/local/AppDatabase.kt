package com.photomaster.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.photomaster.app.data.local.entity.CustomFolderEntity
import com.photomaster.app.data.local.entity.FolderMediaMappingEntity

@Database(
    entities = [CustomFolderEntity::class, FolderMediaMappingEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customFolderDao(): CustomFolderDao
}
