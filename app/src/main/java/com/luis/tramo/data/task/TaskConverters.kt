package com.luis.tramo.data.task

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room converters. Tags and subtasks are persisted as JSON strings (see the design note: they
 * are per-task and never queried across tasks, so a normalized table would add joins for nothing).
 */
class TaskConverters {

    @TypeConverter
    fun fromTags(tags: List<String>): String = json.encodeToString(tags)

    @TypeConverter
    fun toTags(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromSubtasks(subtasks: List<Subtask>): String = json.encodeToString(subtasks)

    @TypeConverter
    fun toSubtasks(value: String): List<Subtask> = json.decodeFromString(value)

    @TypeConverter
    fun fromIntList(days: List<Int>): String = json.encodeToString(days)

    @TypeConverter
    fun toIntList(value: String): List<Int> = json.decodeFromString(value)

    @TypeConverter
    fun fromCategory(category: TaskCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): TaskCategory = TaskCategory.valueOf(value)

    @TypeConverter
    fun fromPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): TaskPriority = TaskPriority.valueOf(value)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
