package com.luis.tramo.data.task

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
@TypeConverters(TaskConverters::class)
abstract class TramoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
