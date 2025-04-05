package com.example.taskmaster

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
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
import com.google.android.material.tabs.TabLayout
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

    // Filter section
    private lateinit var filterSortSection: View
    private lateinit var toggleFiltersButton: MaterialButton
    private lateinit var prioritySpinner: Spinner
    private lateinit var categorySpinner: Spinner
    private lateinit var sortBySpinner: Spinner
    private lateinit var taskStatusTabs: TabLayout

    private var currentStatusFilter: String = "all"

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

        // Stats section
        statsSection = findViewById(R.id.statsSection)
        toggleStatsButton = findViewById(R.id.toggleStatsButton)
        highPriorityCount = findViewById(R.id.highPriorityCount)
        overdueCount = findViewById(R.id.overdueCount)
        completionProgress = findViewById(R.id.completionProgress)
        completionText = findViewById(R.id.completionText)
        inProgressText = findViewById(R.id.inProgressText)

        // Filter section
        filterSortSection = findViewById(R.id.filterSortSection)
        toggleFiltersButton = findViewById(R.id.toggleFiltersButton)
        prioritySpinner = findViewById(R.id.prioritySpinner)
        categorySpinner = findViewById(R.id.categorySpinner)
        sortBySpinner = findViewById(R.id.sortBySpinner)
        taskStatusTabs = findViewById(R.id.taskStatusTabs)

        setupRecyclerView()
        setupToggles()
        setupFilters()

        // Initialize ViewModel
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        // Observe tasks and update UI
        taskViewModel.allTasks.observe(this) { tasks ->
            updateTasks(tasks)
        }

        // New Task button
        findViewById<MaterialButton>(R.id.newTaskButton).setOnClickListener {
            openTaskDetailFragment()
        }

        // FAB
        findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fab).setOnClickListener {
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
        val bottomNavigation: com.google.android.material.bottomnavigation.BottomNavigationView = findViewById(R.id.bottomNavigation)
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
        recyclerView.setHasFixedSize(true)
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

        toggleFiltersButton.setOnClickListener {
            val isVisible = filterSortSection.isVisible
            filterSortSection.isVisible = !isVisible
            toggleFiltersButton.setText(if (isVisible) R.string.show_filters else R.string.hide_filters)
        }
    }

    private fun setupFilters() {
        // Status filter tabs
        taskStatusTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatusFilter = when (tab?.position) {
                    0 -> "all"
                    1 -> "todo"
                    2 -> "in_progress"
                    3 -> "completed"
                    else -> "all"
                }
                updateTasks(taskViewModel.allTasks.value!!)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Priority and Category spinners are already populated via XML
        // Sort By spinner
        val sortOptions = arrayOf("Due Date", "Priority", "Title")
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortBySpinner.adapter = sortAdapter
    }

    private fun updateTasks(tasks: List<Task>) {
        // Apply status filter
        var filteredTasks = when (currentStatusFilter) {
            "todo" -> tasks.filter { !it.completed && !isOverdue(it) }
            "in_progress" -> tasks.filter { !it.completed }
            "completed" -> tasks.filter { it.completed }
            else -> tasks
        }

        // Apply priority filter
        val selectedPriority = prioritySpinner.selectedItem.toString()
        if (selectedPriority != getString(R.string.all_priorities)) {
            filteredTasks = filteredTasks.filter { it.priority.equals(selectedPriority, ignoreCase = true) }
        }

        // Apply category filter
        val selectedCategory = categorySpinner.selectedItem.toString()
        if (selectedCategory != getString(R.string.all_categories)) {
            filteredTasks = filteredTasks.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        // Apply sort
        when (sortBySpinner.selectedItem.toString()) {
            "Due Date" -> filteredTasks = filteredTasks.sortedBy { it.dueDate }
            "Priority" -> filteredTasks = filteredTasks.sortedBy {
                when (it.priority.lowercase()) {
                    "high" -> 1
                    "medium" -> 2
                    "low" -> 3
                    else -> 4
                }
            }
            "Title" -> filteredTasks = filteredTasks.sortedBy { it.title }
        }

        taskAdapter.updateTasks(filteredTasks)

        // Update UI
        recyclerView.isVisible = filteredTasks.isNotEmpty()
        emptyView.isVisible = filteredTasks.isEmpty()

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
        } else {
            userNameTextView.setText(R.string.add_profile)
            userEmailTextView.setText(R.string.tap_to_create_profile)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProfileUI()
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
        val fragment = PerformanceTestFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showMainView() {
        fragmentContainer.isVisible = false
        mainContent.isVisible = true
        updateTasks(taskViewModel.allTasks.value!!)
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
}