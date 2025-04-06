package com.example.taskmanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CalendarFragment : Fragment() {

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_upcoming, container, false)

        recyclerView = view.findViewById(R.id.CalendarRecyclerView)
        emptyView = view.findViewById(R.id.todoEmptyView)
        bottomNavigation = view.findViewById(R.id.bottomNavigation)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = DailyTasksAdapter()
        recyclerView.adapter = adapter

        // Set up bottom navigation
        setupBottomNavigation()

        // Observe tasks from the ViewModel
        taskViewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]
        taskViewModel.inProgressTasks.observe(viewLifecycleOwner) { tasks ->
            if (tasks.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE

                // Group tasks by date and sort dates
                val groupedTasks = groupTasksByDate(tasks)
                adapter.submitList(groupedTasks)
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    // Return to MainActivity by clearing the back stack
                    parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    true
                }
                R.id.nav_find -> {
                    navigateToFragment(FindTaskFragment())
                    true
                }
                R.id.nav_todo -> {
                    // Already in CalendarFragment (To Do)
                    true
                }
                R.id.nav_finished -> {
                    navigateToFragment(FinishedTasksFragment())
                    true
                }
                else -> false
            }
        }
        // Highlight the current fragment
        bottomNavigation.selectedItemId = R.id.nav_todo
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun groupTasksByDate(tasks: List<Task>): List<DailyTasks> {
        // Group tasks by due date
        val groupedMap = HashMap<Calendar, MutableList<Task>>()
        val today = Calendar.getInstance()
        // Reset time portion for today to compare dates only
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        for (task in tasks) {
            // If task has a due date
            if (task.dueDate.isNotEmpty()) {
                try {
                    // The dates are stored in "MMM dd, yyyy" format (e.g., "Mar 31, 2023")
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    val date = dateFormat.parse(task.dueDate)

                    if (date != null) {
                        val cal = Calendar.getInstance()
                        cal.time = date

                        // Reset time portion to compare dates only
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)

                        // Only include tasks due today or in the future
                        if (cal.timeInMillis >= today.timeInMillis) {
                            if (!groupedMap.containsKey(cal)) {
                                groupedMap[cal] = mutableListOf()
                            }
                            groupedMap[cal]?.add(task)
                        }
                    }
                } catch (e: Exception) {
                    // Try alternative date format as fallback
                    try {
                        val altDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = altDateFormat.parse(task.dueDate)

                        if (date != null) {
                            val cal = Calendar.getInstance()
                            cal.time = date

                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)

                            if (cal.timeInMillis >= today.timeInMillis) {
                                if (!groupedMap.containsKey(cal)) {
                                    groupedMap[cal] = mutableListOf()
                                }
                                groupedMap[cal]?.add(task)
                            }
                        }
                    } catch (e2: Exception) {
                        // Both date formats failed, just log the error
                        android.util.Log.e("CalendarFragment", "Could not parse date: ${task.dueDate}", e2)
                    }
                }
            }
        }

        // Convert to list and sort by date (nearest first)
        val result = ArrayList<DailyTasks>()
        for ((date, taskList) in groupedMap) {
            result.add(DailyTasks(date, taskList))
        }

        // Sort by date
        result.sortBy { it.date.timeInMillis }

        return result
    }

    // Data class to hold a date and its tasks
    data class DailyTasks(val date: Calendar, val tasks: List<Task>)

    // Adapter for showing tasks grouped by day
    inner class DailyTasksAdapter : RecyclerView.Adapter<DailyTasksAdapter.DayViewHolder>() {
        private var dailyTasks: List<DailyTasks> = emptyList()

        fun submitList(list: List<DailyTasks>) {
            dailyTasks = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_day_tasks, parent, false)
            return DayViewHolder(view)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val dailyTask = dailyTasks[position]
            holder.bind(dailyTask)
        }

        override fun getItemCount(): Int = dailyTasks.size

        inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dateText: TextView = itemView.findViewById(R.id.dayDateText)
            private val tasksContainer: ViewGroup = itemView.findViewById(R.id.dayTasksContainer)

            fun bind(dailyTask: DailyTasks) {
                // Format the date
                val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                dateText.text = dateFormat.format(dailyTask.date.time)

                // Add task views
                tasksContainer.removeAllViews()

                for (task in dailyTask.tasks) {
                    val taskView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_todo_task, tasksContainer, false)

                    // Set task details
                    val titleText: TextView = taskView.findViewById(R.id.todoTaskTitle)
                    val priorityText: TextView = taskView.findViewById(R.id.todoTaskPriority)
                    val recurrenceText: TextView = taskView.findViewById(R.id.todoTaskRecurrence)

                    titleText.text = task.title
                    priorityText.text = task.priority

                    // Set priority color
                    when (task.priority.lowercase()) {
                        "low" -> priorityText.setTextColor(requireContext().getColor(R.color.green))
                        "medium" -> priorityText.setTextColor(requireContext().getColor(R.color.orange))
                        "high" -> priorityText.setTextColor(requireContext().getColor(R.color.red))
                    }

                    // Set recurrence indicator
                    when (task.recurrence) {
                        "DAILY" -> {
                            recurrenceText.visibility = View.VISIBLE
                            recurrenceText.text = "🔄 Daily"
                        }
                        "WEEKLY" -> {
                            recurrenceText.visibility = View.VISIBLE
                            recurrenceText.text = "🔄 Weekly"
                        }
                        "MONTHLY" -> {
                            recurrenceText.visibility = View.VISIBLE
                            recurrenceText.text = "🔄 Monthly"
                        }
                        else -> recurrenceText.visibility = View.GONE
                    }

                    // Make the task item clickable
                    taskView.setOnClickListener {
                        // Open task detail fragment
                        val fragment = TaskDetailFragment.newInstance(task)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }

                    tasksContainer.addView(taskView)
                }
            }
        }
    }
}