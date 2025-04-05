package com.example.taskmaster

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {
    @Insert
    fun insertSync(task: Task): Long

    @Insert
    fun insertAllSync(tasks: List<Task>): List<Long>

    @Update
    fun updateSync(task: Task): Int

    @Delete
    fun deleteSync(task: Task): Int

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): LiveData<List<Task>>
    
    @Query("SELECT * FROM tasks")
    fun getAllTasksSync(): List<Task>
    
    @Query("SELECT * FROM tasks WHERE category = :category")
    fun getTasksByCategory(category: String): LiveData<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE priority = :priority")
    fun getTasksByPriority(priority: String): LiveData<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE completed = :completed")
    fun getTasksByCompletion(completed: Boolean): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE title LIKE :query OR description LIKE :query")
    fun searchTasks(query: String): LiveData<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun getPagedTasks(limit: Int, offset: Int): List<Task>

    // Add extension suspend functions
    suspend fun insert(task: Task): Long {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            insertSync(task)
        }
    }

    suspend fun insertAll(tasks: List<Task>): List<Long> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            insertAllSync(tasks)
        }
    }

    suspend fun update(task: Task): Int {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            updateSync(task)
        }
    }

    suspend fun delete(task: Task): Int {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            deleteSync(task)
        }
    }
}