package com.example.taskmaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class FindTaskFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: MaterialButton
    private lateinit var emptyView: ConstraintLayout
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskViewModel: TaskViewModel

    // Priority filter chips
    private lateinit var chipHigh: Chip
    private lateinit var chipMedium: Chip
    private lateinit var chipLow: Chip

    // Category filter chips
    private lateinit var chipWork: Chip
    private lateinit var chipPersonal: Chip
    private lateinit var chipShopping: Chip
    private lateinit var chipOther: Chip

    private lateinit var resultsCountText: TextView
    private lateinit var clearSearchButton: MaterialButton

    // Track active filters
    private val activeFilters = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_find_task, container, false)

        // Initialize views
        recyclerView = view.findViewById(R.id.searchRecyclerView)
        searchEditText = view.findViewById(R.id.findSearchEditText)
        searchButton = view.findViewById(R.id.findSearchButton)
        emptyView = view.findViewById(R.id.searchEmptyView)
        resultsCountText = view.findViewById(R.id.resultsCountText)
        clearSearchButton = view.findViewById(R.id.clearSearchButton)

        // Initialize priority filter chips
        chipHigh = view.findViewById(R.id.chipHigh)
        chipMedium = view.findViewById(R.id.chipMedium)
        chipLow = view.findViewById(R.id.chipLow)

        // Initialize category filter chips
        chipWork = view.findViewById(R.id.chipWork)
        chipPersonal = view.findViewById(R.id.chipPersonal)
        chipShopping = view.findViewById(R.id.chipShopping)
        chipOther = view.findViewById(R.id.chipOther)

        setupRecyclerView()
        setupFilterListeners()

        taskViewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]

        // Set up search button listeners (if any additional listeners are required)
        searchButton.setOnClickListener {
            performFilteredSearch(searchEditText.text.toString().trim())
        }
        clearSearchButton.setOnClickListener {
            clearAllFilters()
        }

        // Initial data load
        performFilteredSearch("")

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                layoutManager.orientation
            )
        )
        val itemAnimator = DefaultItemAnimator()
        itemAnimator.supportsChangeAnimations = false
        recyclerView.itemAnimator = itemAnimator

        // Instantiate TaskAdapter with greyOutCompleted set to false.
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
            },
            greyOutCompleted = true  // Disable the dimming effect in FindTaskFragment
        )

        recyclerView.adapter = taskAdapter
    }

    private fun setupFilterListeners() {
        val chipListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            when (buttonView.id) {
                // Priority filters
                R.id.chipHigh -> {
                    if (isChecked) activeFilters.add("high") else activeFilters.remove("high")
                    updateChipAppearance(chipHigh, isChecked)
                }
                R.id.chipMedium -> {
                    if (isChecked) activeFilters.add("medium") else activeFilters.remove("medium")
                    updateChipAppearance(chipMedium, isChecked)
                }
                R.id.chipLow -> {
                    if (isChecked) activeFilters.add("low") else activeFilters.remove("low")
                    updateChipAppearance(chipLow, isChecked)
                }
                // Category filters
                R.id.chipWork -> {
                    if (isChecked) activeFilters.add("work") else activeFilters.remove("work")
                    updateChipAppearance(chipWork, isChecked)
                }
                R.id.chipPersonal -> {
                    if (isChecked) activeFilters.add("personal") else activeFilters.remove("personal")
                    updateChipAppearance(chipPersonal, isChecked)
                }
                R.id.chipShopping -> {
                    if (isChecked) activeFilters.add("shopping") else activeFilters.remove("shopping")
                    updateChipAppearance(chipShopping, isChecked)
                }
                R.id.chipOther -> {
                    if (isChecked) activeFilters.add("other") else activeFilters.remove("other")
                    updateChipAppearance(chipOther, isChecked)
                }
            }
            // Re-apply search with current filters
            performFilteredSearch(searchEditText.text.toString().trim())
        }

        // Set up chip listeners
        chipHigh.setOnCheckedChangeListener(chipListener)
        chipMedium.setOnCheckedChangeListener(chipListener)
        chipLow.setOnCheckedChangeListener(chipListener)
        chipWork.setOnCheckedChangeListener(chipListener)
        chipPersonal.setOnCheckedChangeListener(chipListener)
        chipShopping.setOnCheckedChangeListener(chipListener)
        chipOther.setOnCheckedChangeListener(chipListener)
    }

    private fun performFilteredSearch(query: String) {
        // Remove previous observers to prevent duplicate data
        taskViewModel.allTasks.removeObservers(viewLifecycleOwner)
        if (query.isNotEmpty()) {
            taskViewModel.searchTasks(query).removeObservers(viewLifecycleOwner)
        }

        // Choose data source based on search query
        val tasksLiveData = if (query.isNotEmpty()) {
            taskViewModel.searchTasks(query)
        } else {
            taskViewModel.allTasks
        }

        // Observe and apply filters
        tasksLiveData.observe(viewLifecycleOwner) { allTasks ->
            var filteredTasks = allTasks

            // Apply priority and category filters
            if (activeFilters.isNotEmpty()) {
                val priorityFilters = activeFilters.intersect(setOf("high", "medium", "low"))
                val categoryFilters = activeFilters.minus(priorityFilters)

                if (priorityFilters.isNotEmpty()) {
                    filteredTasks = filteredTasks.filter { task ->
                        priorityFilters.contains(task.priority.lowercase())
                    }
                }

                if (categoryFilters.isNotEmpty()) {
                    filteredTasks = filteredTasks.filter { task ->
                        categoryFilters.contains(task.category.lowercase())
                    }
                }
            }

            // Update adapter with filtered results
            taskAdapter.updateTasks(filteredTasks)

            // If no filters and no search query, force rebind so greyOutCompleted is applied
            if (activeFilters.isEmpty() && query.isEmpty()) {
                taskAdapter.notifyDataSetChanged()
            }

            // Update results count display
            resultsCountText.text = "Results (${filteredTasks.size})"

            // Toggle visibility of empty state
            if (filteredTasks.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            }
        }
    }


    fun setPriorityFilter(priority: String?) {
        // Reset chip states
        chipHigh.isChecked = false
        chipMedium.isChecked = false
        chipLow.isChecked = false

        // Clear existing priority filters
        activeFilters.removeAll { it in listOf("high", "medium", "low") }

        // Set chip based on new priority
        when (priority?.lowercase()) {
            "high" -> {
                chipHigh.isChecked = true
                activeFilters.add("high")
                updateChipAppearance(chipHigh, true)
            }
            "medium" -> {
                chipMedium.isChecked = true
                activeFilters.add("medium")
                updateChipAppearance(chipMedium, true)
            }
            "low" -> {
                chipLow.isChecked = true
                activeFilters.add("low")
                updateChipAppearance(chipLow, true)
            }
        }
        // Reapply filters
        performFilteredSearch(searchEditText.text.toString().trim())
    }

    private fun clearAllFilters() {
        // Clear search text
        searchEditText.setText("")

        // Uncheck all chips and reset appearance
        chipHigh.isChecked = false
        chipMedium.isChecked = false
        chipLow.isChecked = false
        chipWork.isChecked = false
        chipPersonal.isChecked = false
        chipShopping.isChecked = false
        chipOther.isChecked = false

        updateChipAppearance(chipHigh, false)
        updateChipAppearance(chipMedium, false)
        updateChipAppearance(chipLow, false)
        updateChipAppearance(chipWork, false)
        updateChipAppearance(chipPersonal, false)
        updateChipAppearance(chipShopping, false)
        updateChipAppearance(chipOther, false)

        activeFilters.clear()

        // Refresh with no filters and force rebind
        performFilteredSearch("")
        taskAdapter.notifyDataSetChanged()
    }


    private fun updateChipAppearance(chip: Chip, isSelected: Boolean) {
        if (isSelected) {
            chip.chipStrokeWidth = 3f
            chip.setChipStrokeColorResource(android.R.color.black)
        } else {
            chip.chipStrokeWidth = 1f
            when (chip.id) {
                R.id.chipHigh -> chip.setChipStrokeColorResource(R.color.red)
                R.id.chipMedium -> chip.setChipStrokeColorResource(R.color.orange)
                R.id.chipLow -> chip.setChipStrokeColorResource(R.color.green)
                R.id.chipWork -> chip.setChipStrokeColorResource(R.color.blue)
                R.id.chipPersonal -> chip.setChipStrokeColorResource(R.color.purple)
                R.id.chipShopping -> chip.setChipStrokeColorResource(R.color.cyan)
                R.id.chipOther -> chip.setChipStrokeColorResource(R.color.gray)
            }
        }
    }
}
