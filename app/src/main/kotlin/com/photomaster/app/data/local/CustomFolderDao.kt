package com.photomaster.app.data.local

import androidx.room.*
import com.photomaster.app.data.local.entity.CustomFolderEntity
import com.photomaster.app.data.local.entity.FolderMediaMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFolderDao {

    // ── Folders ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM custom_folders ORDER BY createdAt DESC")
    fun observeAllFolders(): Flow<List<CustomFolderEntity>>

    @Query("SELECT * FROM custom_folders ORDER BY createdAt DESC")
    suspend fun getAllFolders(): List<CustomFolderEntity>

    @Query("SELECT * FROM custom_folders WHERE id = :id")
    suspend fun getFolderById(id: Long): CustomFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: CustomFolderEntity): Long

    @Query("UPDATE custom_folders SET name = :name WHERE id = :id")
    suspend fun renameFolder(id: Long, name: String)

    @Query("DELETE FROM custom_folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    // ── Mappings ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM folder_media_mappings WHERE folderId = :folderId ORDER BY addedAt DESC")
    fun observeMappingsByFolder(folderId: Long): Flow<List<FolderMediaMappingEntity>>

    @Query("SELECT * FROM folder_media_mappings WHERE folderId = :folderId ORDER BY addedAt DESC")
    suspend fun getMappingsByFolder(folderId: Long): List<FolderMediaMappingEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMappings(mappings: List<FolderMediaMappingEntity>)

    @Query("DELETE FROM folder_media_mappings WHERE folderId = :folderId AND mediaId IN (:mediaIds)")
    suspend fun removeMappings(folderId: Long, mediaIds: List<Long>)

    @Query("DELETE FROM folder_media_mappings WHERE mediaId = :mediaId")
    suspend fun removeMappingsByMediaId(mediaId: Long)
}
