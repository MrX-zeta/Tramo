package com.luis.tramo.data.task

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.luis.tramo.data.session.SessionDao
import com.luis.tramo.data.session.SessionRecordEntity

@Database(
    entities = [TaskEntity::class, SessionRecordEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(TaskConverters::class)
abstract class TramoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
}
