package com.luis.tramo.data

import com.luis.tramo.data.session.SessionDao
import com.luis.tramo.data.session.SessionRecordEntity
import com.luis.tramo.data.task.Subtask
import com.luis.tramo.data.task.TaskCategory
import com.luis.tramo.data.task.TaskDao
import com.luis.tramo.data.task.TaskEntity
import com.luis.tramo.data.task.TaskPriority
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only helper that fills the database with realistic data so the report, heatmap, streaks and
 * task list can be exercised. Triggered from [com.luis.tramo.MainActivity] via an intent extra on
 * debuggable builds only — never wired into normal app flow.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val sessionDao: SessionDao,
    private val taskDao: TaskDao,
    private val preferences: UserPreferencesRepository
) {
    suspend fun seed() {
        sessionDao.deleteAll()
        taskDao.deleteAll()
        // Set the goal (6) BEFORE seeding sessions, so the reactive celebration never catches a
        // transient where an old, lower goal is already met. This leaves a clean 5/6, un-celebrated,
        // so completing one more focus session by hand triggers the celebration for real.
        preferences.setDailyGoal(6)
        preferences.setFocusPreset(5) // short focus so the test session finishes quickly
        preferences.setLastGoalCelebratedDate("")
        seedSessions()
        seedTasks()
    }

    private suspend fun seedSessions() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val random = Random(seed = 42)
        val focusHours = listOf(9, 10, 11, 14, 15, 16, 20, 21)

        for (dayOffset in 0..74) {
            val date = today.minusDays(dayOffset.toLong())
            // Guarantee a live streak: the last 6 days are always active. Older days ~55%.
            val active = dayOffset < 6 || random.nextFloat() < 0.55f
            if (!active) continue

            // Today is fixed at 5 so it sits one below the goal (6) for the celebration test.
            val focusCount = if (dayOffset == 0) 5 else 1 + random.nextInt(5)
            for (i in 0 until focusCount) {
                val hour = focusHours[random.nextInt(focusHours.size)]
                val minute = random.nextInt(60)
                val completedAt = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
                val focusSeconds = if (random.nextFloat() < 0.2f) 50 * 60 else 25 * 60
                sessionDao.insert(SessionRecordEntity(type = "FOCUS", durationSeconds = focusSeconds, completedAt = completedAt))

                // Most focus blocks are followed by a break; every 4th is a long one.
                if (random.nextFloat() < 0.85f) {
                    val longBreak = (i + 1) % 4 == 0
                    sessionDao.insert(
                        SessionRecordEntity(
                            type = if (longBreak) "LONG_BREAK" else "SHORT_BREAK",
                            durationSeconds = if (longBreak) 15 * 60 else 5 * 60,
                            completedAt = completedAt + 60_000L
                        )
                    )
                }
            }
        }
    }

    private suspend fun seedTasks() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val tasks = listOf(
            TaskEntity(
                title = "Terminar informe trimestral",
                category = TaskCategory.WORK,
                priority = TaskPriority.HIGH,
                tags = listOf("trabajo", "urgente"),
                subtasks = listOf(
                    Subtask("Recopilar datos", true),
                    Subtask("Escribir resumen", false),
                    Subtask("Revisar formato", false)
                ),
                isCompleted = false,
                createdAt = now - 2 * day,
                iconEmoji = "📊",
                colorArgb = 0xFF6366F1L
            ),
            TaskEntity(
                title = "Estudiar para el examen",
                category = TaskCategory.STUDY,
                priority = TaskPriority.HIGH,
                tags = listOf("examen"),
                subtasks = listOf(
                    Subtask("Capítulo 1", true),
                    Subtask("Capítulo 2", true),
                    Subtask("Capítulo 3", false)
                ),
                isCompleted = false,
                createdAt = now - 3 * day,
                iconEmoji = "📚",
                colorArgb = 0xFF3B82F6L
            ),
            TaskEntity(
                title = "Rutina de ejercicio",
                category = TaskCategory.HEALTH,
                priority = TaskPriority.MEDIUM,
                tags = listOf("salud"),
                subtasks = emptyList(),
                isCompleted = false,
                createdAt = now - 5 * day,
                iconEmoji = "🏃",
                colorArgb = 0xFF10B981L,
                isRecurring = true,
                recurringDays = listOf(1, 3, 5) // ISO Mon/Wed/Fri
            ),
            TaskEntity(
                title = "Responder correos pendientes",
                category = TaskCategory.WORK,
                priority = TaskPriority.LOW,
                tags = emptyList(),
                subtasks = emptyList(),
                isCompleted = true,
                createdAt = now - 1 * day,
                iconEmoji = "✉️",
                colorArgb = 0xFF64748BL
            ),
            TaskEntity(
                title = "Planificar viaje de fin de semana",
                category = TaskCategory.PERSONAL,
                priority = TaskPriority.LOW,
                tags = listOf("viaje", "ocio"),
                subtasks = listOf(
                    Subtask("Reservar hotel", false),
                    Subtask("Hacer maleta", false)
                ),
                isCompleted = false,
                createdAt = now - 4 * day,
                iconEmoji = "🗓️",
                colorArgb = 0xFFF97316L
            ),
            TaskEntity(
                title = "Meditar 10 minutos",
                category = TaskCategory.HEALTH,
                priority = TaskPriority.MEDIUM,
                tags = listOf("bienestar"),
                subtasks = emptyList(),
                isCompleted = true,
                createdAt = now - 6 * day,
                iconEmoji = "🧘",
                colorArgb = 0xFF8B5CF6L,
                isRecurring = true,
                recurringDays = listOf(1, 2, 3, 4, 5, 6, 7)
            )
        )
        tasks.forEach { taskDao.insert(it) }
    }
}
