package com.example.taskmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import  androidx. lifecycle. MutableLiveData
import androidx.lifecycle.switchMap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDao: TaskDao = TaskDatabaseProvider.getDatabase(application).taskDao()

    // MutableLiveData for active profile id
    private val _activeProfileId = MutableLiveData<String>()
    val activeProfileId: LiveData<String> get() = _activeProfileId

    init {
        // Initialize with the current active profile id
        _activeProfileId.value = ProfileManager(getApplication()).getActiveProfile()?.id ?: ""
    }

    // Now use switchMap so that whenever the activeProfileId changes,
    // we re-run the query and filtering.
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

    // Call this method to update the active profile id
    fun refreshActiveProfile() {
        _activeProfileId.value = ProfileManager(getApplication()).getActiveProfile()?.id ?: ""
    }

    fun addTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.insert(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.update(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.delete(task)
        }
    }

    fun searchTasks(text: String): LiveData<List<Task>> {
        return taskDao.searchTasks("%$text%").map { tasks ->
            tasks.filter { it.profileId == _activeProfileId.value }
        }
    }
}

