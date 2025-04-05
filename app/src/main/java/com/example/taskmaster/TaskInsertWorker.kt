package com.example.taskmaster

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data

class TaskInsertWorker(
    appContext: Context, 
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val title = inputData.getString("task_title") ?: return Result.failure()
            val description = inputData.getString("task_description") ?: ""
            val priority = inputData.getString("task_priority") ?: "Medium"
            val dueDate = inputData.getString("task_due_date") ?: ""
            val category = inputData.getString("task_category") ?: "Other"
            val completed = inputData.getBoolean("task_completed", false)

            // Get the active profile from ProfileManager
            val profileManager = ProfileManager(applicationContext)
            val activeProfile = profileManager.getActiveProfile()
            val profileId = activeProfile?.id ?: ""

            // Create task with the profile binding
            val task = Task(
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                category = category,
                completed = completed,
                profileId = profileId
            )

            // Insert task into database
            val dao = TaskDatabaseProvider.getDatabase(applicationContext).taskDao()
            dao.insert(task)

            val outputData = Data.Builder()
                .putInt("task_id", task.id)
                .build()

            Result.success(outputData)
        } catch (e: Exception) {
            Result.failure()
        }
    }

}
