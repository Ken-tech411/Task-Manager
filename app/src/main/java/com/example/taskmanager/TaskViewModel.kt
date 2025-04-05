package com.example.taskmanager

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDao: TaskDao = TaskDatabaseProvider.getDatabase(application).taskDao()

    // MutableLiveData for active profile ID
    private val _activeProfileId = MutableLiveData<String>()
    val activeProfileId: LiveData<String> get() = _activeProfileId

    init {
        // Initialize with the current active profile ID
        val initialProfileId = ProfileManager(getApplication()).getActiveProfile()?.id ?: ""
        _activeProfileId.value = initialProfileId
        Log.d("TaskViewModel", "Initialized with active profile ID: $initialProfileId")
    }

    // Fetch tasks directly by profile ID using switchMap
    val allTasks: LiveData<List<Task>> = activeProfileId.switchMap { profileId ->
        taskDao.getAllTasks().map { tasks ->
            tasks.filter { it.profileId == profileId }
        }
    }

    val inProgressTasks: LiveData<List<Task>> = allTasks.map { tasks ->
        tasks.filter { !it.completed }
    }

    val completedTasks: LiveData<List<Task>> = allTasks.map { tasks ->
        tasks.filter { it.completed }
    }

    // Update the active profile ID and log the change
    fun refreshActiveProfile() {
        val newProfileId = ProfileManager(getApplication()).getActiveProfile()?.id ?: ""
        Log.d("TaskViewModel", "Refreshing active profile to: $newProfileId")
        _activeProfileId.value = newProfileId
    }

    fun addTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.insert(task)
            Log.d("TaskViewModel", "Added task: ${task.title} for profile: ${task.profileId}")
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.update(task)
            Log.d("TaskViewModel", "Updated task: ${task.title}")
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.delete(task)
            Log.d("TaskViewModel", "Deleted task: ${task.title}")
        }
    }

    fun searchTasks(text: String): LiveData<List<Task>> {
        return taskDao.searchTasks("%$text%").switchMap { tasks ->
            activeProfileId.map { profileId ->
                tasks.filter { it.profileId == profileId }
            }
        }
    }
}