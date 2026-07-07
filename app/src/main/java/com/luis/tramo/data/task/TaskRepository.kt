package com.luis.tramo.data.task

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val dao: TaskDao
) {
    fun activeTasks(): Flow<List<TaskEntity>> = dao.activeTasks()
    fun completedTasks(): Flow<List<TaskEntity>> = dao.completedTasks()

    suspend fun count(): Int = dao.count()
    suspend fun add(task: TaskEntity): Long = dao.insert(task)
    suspend fun update(task: TaskEntity) = dao.update(task)
    suspend fun delete(task: TaskEntity) = dao.delete(task)
}
