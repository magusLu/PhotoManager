package com.photomaster.app.di

import android.content.Context
import androidx.room.Room
import com.photomaster.app.data.local.AppDatabase
import com.photomaster.app.data.local.CleanRuleDao
import com.photomaster.app.data.local.CustomFolderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "photomaster.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideCustomFolderDao(db: AppDatabase): CustomFolderDao =
        db.customFolderDao()

    @Provides
    @Singleton
    fun provideCleanRuleDao(db: AppDatabase): CleanRuleDao =
        db.cleanRuleDao()
}
