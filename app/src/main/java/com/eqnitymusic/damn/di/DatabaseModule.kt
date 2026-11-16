package com.eqnitymusic.damn.di

import android.content.Context
import com.eqnitymusic.damn.data.db.AppDatabase
import com.eqnitymusic.damn.data.repository.DownloadRepository
import com.eqnitymusic.damn.data.repository.MusicRepository
import com.eqnitymusic.damn.data.repository.PlaylistRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMusicRepository(database: AppDatabase): MusicRepository {
        return MusicRepository(database)
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(database: AppDatabase): PlaylistRepository {
        return PlaylistRepository(database)
    }

    @Provides
    @Singleton
    fun provideDownloadRepository(database: AppDatabase): DownloadRepository {
        return DownloadRepository(database)
    }
}