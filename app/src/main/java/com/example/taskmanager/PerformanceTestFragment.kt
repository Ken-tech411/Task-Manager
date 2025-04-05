package com.example.taskmanager

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.taskmanager.databinding.FragmentPerformanceTestBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Fragment to conduct performance tests for the investigation component.
 * This fragment allows running controlled tests to measure database performance
 * with different implementations.
 */
class PerformanceTestFragment : Fragment() {

    private var _binding: FragmentPerformanceTestBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskViewModel: TaskViewModel
    private var isTestRunning = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerformanceTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use the shared viewModel from MainActivity to ensure data consistency
        taskViewModel = (requireActivity() as MainActivity).taskViewModel

        binding.startTestButton.setOnClickListener {
            if (!isTestRunning) {
                startPerformanceTest()
            } else {
                Toast.makeText(context, "Test already running", Toast.LENGTH_SHORT).show()
            }
        }

        binding.clearResultsButton.setOnClickListener {
            binding.resultsTextView.text = ""
        }
    }

    private fun startPerformanceTest() {
        isTestRunning = true
        binding.startTestButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.resultsTextView.append("Starting performance test at ${getCurrentTime()}\n")

        // Toggle this flag to switch between bulk insertion (false) and sequential insertion (true)
        val useSequentialInsertion = false

        // Run the test in a background thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Run the insertion test based on the selected variant
                val insertionTime = if (useSequentialInsertion) {
                    runSequentialInsertionTest(100)
                } else {
                    runBulkInsertionTest(100)
                }

                // Run the query test
                val queryTime = runQueryTest()

                // Run the update test
                val updateTime = runUpdateTest()

                // Run the delete test
                val deleteTime = runDeleteTest()

                // Update UI with results
                withContext(Dispatchers.Main) {
                    binding.resultsTextView.append("\n--- TEST RESULTS ---\n")
                    if (useSequentialInsertion) {
                        binding.resultsTextView.append("Sequential insertion (100 tasks): $insertionTime ms\n")
                    } else {
                        binding.resultsTextView.append("Bulk insertion (100 tasks): $insertionTime ms\n")
                    }
                    binding.resultsTextView.append("Query all tasks: $queryTime ms\n")
                    binding.resultsTextView.append("Update all tasks: $updateTime ms\n")
                    binding.resultsTextView.append("Delete all tasks: $deleteTime ms\n")
                    binding.resultsTextView.append("Test completed at ${getCurrentTime()}\n\n")
                    binding.resultsTextView.append("${measureMemoryUsage()}\n")

                    binding.progressBar.visibility = View.GONE
                    binding.startTestButton.isEnabled = true
                    isTestRunning = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.resultsTextView.append("Error during test: ${e.message}\n")
                    binding.progressBar.visibility = View.GONE
                    binding.startTestButton.isEnabled = true
                    isTestRunning = false
                }
            }
        }
    }

    private suspend fun runBulkInsertionTest(count: Int): Long {
        val startTime = SystemClock.elapsedRealtime()
        val dao = TaskDatabaseProvider.getDatabase(requireContext()).taskDao()

        val categories = arrayOf("Work", "Personal", "Shopping", "Study", "Other")
        val priorities = arrayOf("High", "Medium", "Low")

        // Get the active profile ID (or empty if none)
        val profileManager = ProfileManager(requireContext())
        val activeProfile = profileManager.getActiveProfile()
        val profileId = activeProfile?.id ?: ""

        val tasks = List(count) {
            Task(
                title = "Test Task $it",
                description = "Description for test task $it",
                priority = priorities[Random.nextInt(priorities.size)],
                dueDate = getRandomFutureDate(),
                category = categories[Random.nextInt(categories.size)],
                completed = Random.nextBoolean(),
                profileId = profileId
            )
        }
        dao.insertAll(tasks)
        return SystemClock.elapsedRealtime() - startTime
    }

    // Variant 2: Sequential insertion test
    private suspend fun runSequentialInsertionTest(count: Int): Long {
        val startTime = SystemClock.elapsedRealtime()
        val dao = TaskDatabaseProvider.getDatabase(requireContext()).taskDao()

        val categories = arrayOf("Work", "Personal", "Shopping", "Study", "Other")
        val priorities = arrayOf("High", "Medium", "Low")

        // Get the active profile ID (or empty if none)
        val profileManager = ProfileManager(requireContext())
        val activeProfile = profileManager.getActiveProfile()
        val profileId = activeProfile?.id ?: ""

        for (i in 0 until count) {
            val task = Task(
                title = "Test Task $i",
                description = "Description for test task $i",
                priority = priorities[Random.nextInt(priorities.size)],
                dueDate = getRandomFutureDate(),
                category = categories[Random.nextInt(categories.size)],
                completed = Random.nextBoolean(),
                profileId = profileId
            )
            dao.insert(task)
        }
        return SystemClock.elapsedRealtime() - startTime
    }

    private suspend fun runQueryTest(): Long {
        val startTime = SystemClock.elapsedRealtime()
        val dao = TaskDatabaseProvider.getDatabase(requireContext()).taskDao()

        // Use a synchronous query for measurement purposes
        val tasks = dao.getAllTasksSync()

        withContext(Dispatchers.Main) {
            binding.resultsTextView.append("Retrieved ${tasks.size} tasks\n")
        }

        return SystemClock.elapsedRealtime() - startTime
    }

    private suspend fun runUpdateTest(): Long {
        val startTime = SystemClock.elapsedRealtime()
        val dao = TaskDatabaseProvider.getDatabase(requireContext()).taskDao()

        // Get all tasks
        val tasks = dao.getAllTasksSync()

        // Update each task
        var updateCount = 0
        for (task in tasks) {
            val updatedTask = task.copy(
                title = "${task.title} (updated)",
                completed = !task.completed
            )
            updateCount += dao.update(updatedTask)
        }

        withContext(Dispatchers.Main) {
            binding.resultsTextView.append("Updated $updateCount tasks\n")
        }

        return SystemClock.elapsedRealtime() - startTime
    }

    private suspend fun runDeleteTest(): Long {
        val startTime = SystemClock.elapsedRealtime()
        val dao = TaskDatabaseProvider.getDatabase(requireContext()).taskDao()

        // Get all tasks
        val tasks = dao.getAllTasksSync()
        var deleteCount = 0

        // Delete each task
        for (task in tasks) {
            deleteCount += dao.delete(task)
        }

        withContext(Dispatchers.Main) {
            binding.resultsTextView.append("Deleted $deleteCount tasks\n")
        }

        return SystemClock.elapsedRealtime() - startTime
    }

    private fun getRandomFutureDate(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, Random.nextInt(1, 30))
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(calendar.time)
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    }

    private fun measureMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        return "Memory: $usedMemory MB / $maxMemory MB"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
