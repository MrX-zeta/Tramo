package com.luis.tramo.di

import android.content.Context
import androidx.room.Room
import com.luis.tramo.data.task.TaskDao
import com.luis.tramo.data.task.TramoDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): TramoDatabase =
        Room.databaseBuilder(context, TramoDatabase::class.java, "tramo.db").build()

    @Provides
    fun provideTaskDao(database: TramoDatabase): TaskDao = database.taskDao()
}
