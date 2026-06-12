package com.photomaster.app.data.local

import androidx.room.*
import com.photomaster.app.data.local.entity.CleanRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CleanRuleDao {

    @Query("SELECT * FROM clean_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<CleanRuleEntity>>

    @Query("SELECT * FROM clean_rules WHERE enabled = 1")
    suspend fun getEnabledRules(): List<CleanRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CleanRuleEntity): Long

    @Update
    suspend fun updateRule(rule: CleanRuleEntity)

    @Delete
    suspend fun deleteRule(rule: CleanRuleEntity)

    @Query("UPDATE clean_rules SET lastRunAt = :ts WHERE id = :ruleId")
    suspend fun markRuleRun(ruleId: Long, ts: Long)

    @Query("UPDATE clean_rules SET enabled = :enabled WHERE id = :ruleId")
    suspend fun setEnabled(ruleId: Long, enabled: Boolean)
}
