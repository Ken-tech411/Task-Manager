package com.example.taskmaster

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmaster.databinding.ItemTaskBinding

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
                val descriptionText = if (task.description.isNotEmpty() && task.notes.isNotEmpty()) {
                    "${task.description}\n\nNotes: ${task.notes}"
                } else if (task.notes.isNotEmpty()) {
                    "Notes: ${task.notes}"
                } else {
                    task.description
                }
                
                taskDescription.text = descriptionText
                
                taskCategory.text = task.category ?: "Uncategorized"
                
                // For due date, indicate if it's recurring
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

                // Handle checkbox state changes
                taskCompleted.setOnCheckedChangeListener(null)
                taskCompleted.isChecked = task.completed
                taskCompleted.setOnCheckedChangeListener { _, isChecked ->
                    checkListener(task, isChecked)
                }

                // Set priority indicator color based on task.priority
                val context = root.context
                val backgroundColor = when (task.priority.lowercase()) {
                    "high" -> context.getColor(android.R.color.holo_red_light)
                    "medium" -> context.getColor(android.R.color.holo_orange_light)
                    else -> context.getColor(android.R.color.holo_green_light)
                }
                priorityIndicator.setBackgroundColor(backgroundColor)

                // Apply grey-out effect if greyOutCompleted is true and the task is completed.
                root.alpha = if (greyOutCompleted && task.completed) 0.6f else 1.0f

                // Set click listener
                root.setOnClickListener { listener(task) }
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean =
            false // Always force rebind to update the grey-out effect.
    }

    // Updated updateTasks that forces a full rebind.
    fun updateTasks(newTasks: List<Task>) {
        submitList(newTasks.toList()) {
            notifyDataSetChanged()
        }
    }
}
