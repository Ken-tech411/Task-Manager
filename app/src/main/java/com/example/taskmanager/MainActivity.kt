package com.example.taskmanager

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), EditProfileFragment.OnProfileUpdatedListener {

    lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var profileManager: ProfileManager
    private lateinit var mainContent: View
    private lateinit var fragmentContainer: View
    private lateinit var bottomNavigation: com.google.android.material.bottomnavigation.BottomNavigationView

    // Header elements
    private lateinit var tasksProgressText: TextView
    private lateinit var tasksProgressBar: LinearProgressIndicator
    private lateinit var progressPercentageText: TextView

    // Stats section
    private lateinit var statsSection: View
    private lateinit var toggleStatsButton: MaterialButton
    private lateinit var highPriorityCount: TextView
    private lateinit var overdueCount: TextView
    private lateinit var completionProgress: CircularProgressIndicator
    private lateinit var completionText: TextView
    private lateinit var inProgressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ProfileManager
        profileManager = ProfileManager(this)

        // Initialize views
        mainContent = findViewById(R.id.mainContent)
        fragmentContainer = findViewById(R.id.fragment_container)
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        tasksProgressText = findViewById(R.id.tasksProgressText)
        tasksProgressBar = findViewById(R.id.tasksProgressBar)
        progressPercentageText = findViewById(R.id.progressPercentageText)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Stats section
        statsSection = findViewById(R.id.statsSection)
        toggleStatsButton = findViewById(R.id.toggleStatsButton)
        highPriorityCount = findViewById(R.id.highPriorityCount)
        overdueCount = findViewById(R.id.overdueCount)
        completionProgress = findViewById(R.id.completionProgress)
        completionText = findViewById(R.id.completionText)
        inProgressText = findViewById(R.id.inProgressText)

        setupRecyclerView()
        setupToggles()

        // Initialize ViewModel
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        // Observe tasks and update UI
        taskViewModel.allTasks.observe(this) { tasks ->
            Log.d("MainActivity", "Received ${tasks.size} tasks from ViewModel for profile: ${taskViewModel.activeProfileId.value}")
            tasks.forEach { task ->
                Log.d("MainActivity", "Task: ${task.title}, ProfileId: ${task.profileId}")
            }
            updateTasks(tasks)
        }

        // New Task button
        findViewById<MaterialButton>(R.id.newTaskButton).setOnClickListener {
            openTaskDetailFragment()
        }

        // User profile icon
        val userProfileImage: ImageView = findViewById(R.id.userProfileImage)
        userProfileImage.setOnClickListener {
            showProfilesFragment()
        }

        // Display active profile data
        refreshProfileUI()

        // Settings icon
        val settingsIcon: MaterialButton = findViewById(R.id.notificationIcon)
        settingsIcon.setIconResource(android.R.drawable.ic_menu_manage)
        settingsIcon.contentDescription = getString(R.string.settings)
        settingsIcon.setOnClickListener { showSettingsMenu() }

        // Back stack listener
        supportFragmentManager.addOnBackStackChangedListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment == null) {
                showMainView()
            } else {
                hideMainView()
            }
        }

        // Bottom Navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    showMainView()
                    true
                }
                R.id.nav_find -> {
                    hideMainView()
                    fragmentContainer.isVisible = true
                    val fragment = FindTaskFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_todo -> {
                    hideMainView()
                    fragmentContainer.isVisible = true
                    val fragment = CalendarFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_finished -> {
                    hideMainView()
                    fragmentContainer.isVisible = true
                    val fragment = FinishedTasksFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupRecyclerView() {
        // Remove setHasFixedSize(true) to allow dynamic height adjustment
        // recyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.list_divider)?.let {
            itemDecoration.setDrawable(it)
        }
        recyclerView.addItemDecoration(itemDecoration)

        val itemAnimator = DefaultItemAnimator()
        itemAnimator.supportsChangeAnimations = false
        recyclerView.itemAnimator = itemAnimator

        taskAdapter = TaskAdapter(
            listener = { task ->
                hideMainView()
                fragmentContainer.isVisible = true
                val fragment = TaskDetailFragment.newInstance(task)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            checkListener = { task, isChecked ->
                val updatedTask = task.copy(completed = isChecked)
                taskViewModel.updateTask(updatedTask)
            }
        )

        recyclerView.adapter = taskAdapter
    }

    private fun setupToggles() {
        toggleStatsButton.setOnClickListener {
            val isVisible = statsSection.isVisible
            statsSection.isVisible = !isVisible
            toggleStatsButton.setText(if (isVisible) R.string.show_stats else R.string.hide_stats)
        }
    }

    private fun updateTasks(tasks: List<Task>) {
        taskAdapter.updateTasks(tasks)
        Log.d("MainActivity", "Updated adapter with ${tasks.size} tasks")

        // Update UI visibility
        recyclerView.isVisible = tasks.isNotEmpty()
        emptyView.isVisible = tasks.isEmpty()

        // Log RecyclerView height after update
        recyclerView.post {
            Log.d("MainActivity", "RecyclerView height after update: ${recyclerView.height}")
        }

        // Update progress
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.completed }
        val progress = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0
        tasksProgressText.text = getString(R.string.tasks_progress_format, completedTasks, totalTasks)
        tasksProgressBar.progress = progress
        progressPercentageText.text = getString(R.string.progress_complete_format, progress)

        // Update stats
        val highPriority = tasks.count { it.priority.equals("high", ignoreCase = true) && !it.completed }
        val overdue = tasks.count { !it.completed && isOverdue(it) }
        highPriorityCount.text = highPriority.toString()
        overdueCount.text = overdue.toString()
        completionProgress.progress = progress
        completionText.text = getString(R.string.completed_progress_format, progress)
        inProgressText.text = getString(R.string.in_progress_progress_format, 100 - progress)
    }

    private fun isOverdue(task: Task): Boolean {
        if (task.dueDate.isNullOrEmpty()) return false
        return try {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val dueDate = dateFormat.parse(task.dueDate) ?: return false
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            dueDate.before(today)
        } catch (e: Exception) {
            false
        }
    }

    fun refreshProfileUI() {
        val userNameTextView: TextView = findViewById(R.id.userNameTextView)
        val userEmailTextView: TextView = findViewById(R.id.userEmailTextView)

        val activeProfile = profileManager.getActiveProfile()
        if (activeProfile != null) {
            userNameTextView.text = activeProfile.name
            userEmailTextView.text = activeProfile.email
            Log.d("MainActivity", "Refreshed profile UI: ${activeProfile.name}, ${activeProfile.email}")
        } else {
            userNameTextView.setText(R.string.add_profile)
            userEmailTextView.setText(R.string.tap_to_create_profile)
            Log.d("MainActivity", "No active profile, prompting to add one")
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProfileUI()
        taskViewModel.refreshActiveProfile()
    }

    override fun onProfileUpdated() {
        refreshProfileUI()
        taskViewModel.refreshActiveProfile()
    }

    private fun showSettingsMenu() {
        val options = arrayOf(
            getString(R.string.manage_profiles),
            getString(R.string.performance_test),
            getString(R.string.app_settings)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.settings)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showProfilesFragment()
                    1 -> showPerformanceTestFragment()
                    2 -> Toast.makeText(this, getString(R.string.app_settings), Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showProfilesFragment() {
        hideMainView()
        fragmentContainer.isVisible = true
        hideBottomNavigation()
        if (profileManager.getAllProfiles().isEmpty()) {
            val bundle = Bundle().apply {
                putBoolean("isNewProfile", true)
            }
            val editFragment = EditProfileFragment().apply {
                arguments = bundle
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit()
        } else {
            val fragment = ProfilesFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showPerformanceTestFragment() {
        hideMainView()
        fragmentContainer.isVisible = true
        hideBottomNavigation()
        val fragment = PerformanceTestFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun showMainView() {
        fragmentContainer.isVisible = false
        mainContent.isVisible = true
        showBottomNavigation()
        taskViewModel.refreshActiveProfile()
        val tasks = taskViewModel.allTasks.value ?: emptyList()
        updateTasks(tasks)
    }

    private fun hideMainView() {
        mainContent.isVisible = false
    }

    private fun openTaskDetailFragment() {
        hideMainView()
        fragmentContainer.isVisible = true
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TaskDetailFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun hideBottomNavigation() {
        bottomNavigation.visibility = View.GONE
    }

    private fun showBottomNavigation() {
        bottomNavigation.visibility = View.VISIBLE
    }
}