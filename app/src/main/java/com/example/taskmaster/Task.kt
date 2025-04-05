package com.example.taskmaster

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["priority"]),
        Index(value = ["category"]),
        Index(value = ["completed"]),
        Index(value = ["profileId"]) // new index
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "priority") val priority: String,
    @ColumnInfo(name = "dueDate") val dueDate: String,
    @ColumnInfo(name = "category") val category: String = "Other",
    @ColumnInfo(name = "completed") val completed: Boolean,
    @ColumnInfo(name = "profileId") val profileId: String, // Field to bind the task to a profile
    @ColumnInfo(name = "recurrence") val recurrence: String = "NONE", // New field for recurring tasks
    @ColumnInfo(name = "notes") val notes: String = "" // New field for task notes
) : Parcelable

