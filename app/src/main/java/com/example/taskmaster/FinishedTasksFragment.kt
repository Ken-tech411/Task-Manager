package com.example.taskmaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class FinishedTasksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: ConstraintLayout
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_finished_tasks, container, false)

        recyclerView = view.findViewById(R.id.finishedRecyclerView)
        emptyView = view.findViewById(R.id.finishedEmptyView)
        toolbar = view.findViewById(R.id.finishedTasksToolbar)

        setupRecyclerView()
        setupToolbar()

        taskViewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]

        // Observe only completed tasks with animation
        taskViewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            val completedTasks = tasks.filter { it.completed }
            taskAdapter.updateTasks(completedTasks)

            if (completedTasks.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f).duration = 300
                }
            } else {
                emptyView.visibility = View.GONE
                recyclerView.apply {
                    if (visibility != View.VISIBLE) {
                        alpha = 0f
                        visibility = View.VISIBLE
                        animate().alpha(1f).duration = 300
                    }
                }
            }

            // Update the toolbar title with count
            updateToolbarTitle(completedTasks.size)
        }

        return view
    }

    private fun setupToolbar() {
        // Navigation click listener removed
        // Just update the toolbar title
    }

    private fun updateToolbarTitle(count: Int) {
        toolbar.title = "Completed Tasks ($count)"
    }

    private fun setupRecyclerView() {
        recyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        // Remove the divider decoration for a cleaner look

        val itemAnimator = DefaultItemAnimator()
        itemAnimator.supportsChangeAnimations = false
        recyclerView.itemAnimator = itemAnimator

        taskAdapter = TaskAdapter(
            listener = { task ->
                // Open task detail fragment
                val fragment = TaskDetailFragment.newInstance(task)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            checkListener = { task, isChecked ->
                // Update task completion status
                val updatedTask = task.copy(completed = isChecked)
                taskViewModel.updateTask(updatedTask)

                // If task is unchecked, it should no longer appear in the finished tasks list
                if (!isChecked) {
                    taskViewModel.allTasks.value?.let { tasks ->
                        val completedTasks = tasks.filter { it.completed }
                        taskAdapter.updateTasks(completedTasks)
                    }
                }
            }
        )

        recyclerView.adapter = taskAdapter
    }
}
