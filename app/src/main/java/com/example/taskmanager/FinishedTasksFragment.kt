package com.example.taskmanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class FinishedTasksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: ConstraintLayout
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_finished_tasks, container, false)

        recyclerView = view.findViewById(R.id.finishedRecyclerView)
        emptyView = view.findViewById(R.id.finishedEmptyView)
        toolbar = view.findViewById(R.id.finishedTasksToolbar)
        bottomNavigation = view.findViewById(R.id.bottomNavigation)

        setupRecyclerView()
        setupToolbar()
        setupBottomNavigation()

        taskViewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]

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

            updateToolbarTitle(completedTasks.size)
        }

        return view
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
                    navigateToFragment(CalendarFragment())
                    true
                }
                R.id.nav_finished -> {
                    // Already in FinishedTasksFragment
                    true
                }
                else -> false
            }
        }
        // Highlight the current fragment
        bottomNavigation.selectedItemId = R.id.nav_finished
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setupToolbar() {
        // Toolbar setup remains minimal
    }

    private fun updateToolbarTitle(count: Int) {
        toolbar.title = "Completed Tasks ($count)"
    }

    private fun setupRecyclerView() {
        recyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        val itemAnimator = DefaultItemAnimator()
        itemAnimator.supportsChangeAnimations = false
        recyclerView.itemAnimator = itemAnimator

        taskAdapter = TaskAdapter(
            listener = { task ->
                val fragment = TaskDetailFragment.newInstance(task)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            checkListener = { task, isChecked ->
                val updatedTask = task.copy(completed = isChecked)
                taskViewModel.updateTask(updatedTask)

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