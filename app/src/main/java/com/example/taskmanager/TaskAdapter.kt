package com.example.taskmanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanager.databinding.ItemTaskBinding

class TaskAdapter(
    private val listener: (Task) -> Unit,
    private val checkListener: (Task, Boolean) -> Unit,
    private val greyOutCompleted: Boolean = true
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task, listener, checkListener)
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task, listener: (Task) -> Unit, checkListener: (Task, Boolean) -> Unit) {
            binding.apply {
                taskTitle.text = task.title

                // Show description or notes or both
                val descriptionText = when {
                    task.description.isNotEmpty() && task.notes.isNotEmpty() -> "${task.description}\n\nNotes: ${task.notes}"
                    task.notes.isNotEmpty() -> "Notes: ${task.notes}"
                    else -> task.description
                }
                taskDescription.text = descriptionText

                taskCategory.text = task.category ?: "Uncategorized"

                // Handle recurring tasks in due date
                val recurringSymbol = when (task.recurrence) {
                    "DAILY" -> "🔄 Daily"
                    "WEEKLY" -> "🔄 Weekly"
                    "MONTHLY" -> "🔄 Monthly"
                    else -> ""
                }
                taskDueDate.text = if (recurringSymbol.isNotEmpty()) {
                    "Due: ${task.dueDate ?: "No due date"} ($recurringSymbol)"
                } else {
                    "Due: ${task.dueDate ?: "No due date"}"
                }

                // Handle checkbox state
                taskCompleted.setOnCheckedChangeListener(null)
                taskCompleted.isChecked = task.completed
                taskCompleted.setOnCheckedChangeListener { _, isChecked ->
                    checkListener(task, isChecked)
                }

                // Set priority indicator color
                val context = root.context
                val backgroundColor = when (task.priority.lowercase()) {
                    "high" -> context.getColor(android.R.color.holo_red_light)
                    "medium" -> context.getColor(android.R.color.holo_orange_light)
                    else -> context.getColor(android.R.color.holo_green_light)
                }
                priorityIndicator.setBackgroundColor(backgroundColor)

                // Apply grey-out effect for completed tasks
                root.alpha = if (greyOutCompleted && task.completed) 0.6f else 1.0f

                // Set click listener
                root.setOnClickListener { listener(task) }
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem // Compare all fields using data class equality
        }
    }

    // Simplified updateTasks to rely on ListAdapter's diffing
    fun updateTasks(newTasks: List<Task>) {
        submitList(newTasks.toList())
    }
}