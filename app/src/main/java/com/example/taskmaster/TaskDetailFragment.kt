package com.example.taskmaster

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import com.example.taskmaster.databinding.FragmentTaskDetailBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskDetailFragment : Fragment() {

    private lateinit var taskViewModel: TaskViewModel
    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!
    private var taskToEdit: Task? = null
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the binding
        _binding = FragmentTaskDetailBinding.inflate(inflater, container, false)

        // Initialize ViewModel
        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)

        // Setup category spinner
        setupCategorySpinner()

        // Setup date picker
        setupDatePicker()

        // Set form title based on edit/add mode
        binding.taskFormTitle.text = if (taskToEdit != null)
            getString(R.string.task_details)
        else
            getString(R.string.add_task)

        // Load task data if editing an existing task
        taskToEdit = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_TASK, Task::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_TASK)
        }
        taskToEdit?.let { task ->
            binding.apply {
                titleEditText.setText(task.title)
                descriptionEditText.setText(task.description)

                // Set priority radio button
                when (task.priority.lowercase()) {
                    "low" -> priorityLow.isChecked = true
                    "medium" -> priorityMedium.isChecked = true
                    "high" -> priorityHigh.isChecked = true
                    else -> priorityLow.isChecked = true
                }

                dueDateEditText.setText(task.dueDate)
                completedCheckBox.isChecked = task.completed
                saveButton.text = getString(R.string.update)

                // Set recurrence option
                when (task.recurrence) {
                    "DAILY" -> recurrenceDaily.isChecked = true
                    "WEEKLY" -> recurrenceWeekly.isChecked = true
                    "MONTHLY" -> recurrenceMonthly.isChecked = true
                    else -> recurrenceNone.isChecked = true
                }

                // Set notes
                notesEditText.setText(task.notes)

                // Set category in spinner
                val adapter = binding.categorySpinner.adapter as? ArrayAdapter<String>
                adapter?.let {
                    val position = it.getPosition(task.category)
                    if (position >= 0) {
                        binding.categorySpinner.setSelection(position)
                    }
                }
            }
        }

        // Save button click listener
        binding.saveButton.setOnClickListener {
            saveTask()
        }

        // Cancel button click listener
        binding.cancelButton.setOnClickListener {
            // Just pop the back stack and let the fragment manager handle it
            parentFragmentManager.popBackStack()

            // Check if there are more fragments in the back stack
            if (parentFragmentManager.backStackEntryCount == 0) {
                // If no more fragments, hide the container (returning to main activity)
                requireActivity().findViewById<View>(R.id.fragment_container).visibility = View.GONE
            }
        }

        return binding.root
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf(
            getString(R.string.category_work),
            getString(R.string.category_personal),
            getString(R.string.category_shopping),
            getString(R.string.category_other)
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter
    }

    private fun setupDatePicker() {
        binding.dueDateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    binding.dueDateEditText.setText(dateFormatter.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun saveTask() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val dueDate = binding.dueDateEditText.text.toString().trim()
        val completed = binding.completedCheckBox.isChecked
        val category = binding.categorySpinner.selectedItem.toString()
        val notes = binding.notesEditText.text.toString().trim()

        // Get selected priority
        val priorityId = binding.priorityRadioGroup.checkedRadioButtonId
        if (priorityId == -1) {
            Toast.makeText(context, "Please select a priority", Toast.LENGTH_SHORT).show()
            return
        }

        val priority = when (priorityId) {
            R.id.priorityLow -> "Low"
            R.id.priorityMedium -> "Medium"
            R.id.priorityHigh -> "High"
            else -> "Low"
        }

        // Get selected recurrence
        val recurrenceId = binding.recurrenceRadioGroup.checkedRadioButtonId
        val recurrence = when (recurrenceId) {
            R.id.recurrenceDaily -> "DAILY"
            R.id.recurrenceWeekly -> "WEEKLY"
            R.id.recurrenceMonthly -> "MONTHLY"
            else -> "NONE"
        }

        if (title.isEmpty()) {
            Toast.makeText(context, getString(R.string.required_field), Toast.LENGTH_SHORT).show()
            return
        }

        // Retrieve the active profile
        val profileManager = ProfileManager(requireContext())
        val activeProfile = profileManager.getActiveProfile()
        if (activeProfile == null) {
            Toast.makeText(context, "No active profile found. Please create a profile.", Toast.LENGTH_SHORT).show()
            return
        }

        if (taskToEdit != null) {
            // Update existing task (retain the original profileId)
            val updatedTask = taskToEdit!!.copy(
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                category = category,
                completed = completed,
                recurrence = recurrence,
                notes = notes
                // profileId remains unchanged
            )
            taskViewModel.updateTask(updatedTask)

            // If the task is completed and it's recurring, create a new instance
            if (completed && recurrence != "NONE") {
                createNextRecurringTask(updatedTask)
            }

            Toast.makeText(context, getString(R.string.task_updated), Toast.LENGTH_SHORT).show()
        } else {
            // Create new task and bind it to the active profile
            val newTask = Task(
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                category = category,
                completed = completed,
                profileId = activeProfile.id,  // bind task to active profile
                recurrence = recurrence,
                notes = notes
            )
            taskViewModel.addTask(newTask)
            Toast.makeText(context, getString(R.string.task_added), Toast.LENGTH_SHORT).show()
        }

        // Close fragment
        parentFragmentManager.popBackStack()
    }

    // Add method to create next recurring task
    private fun createNextRecurringTask(completedTask: Task) {
        // Calculate the next due date based on recurrence pattern
        val nextDueDate = calculateNextDueDate(completedTask.dueDate, completedTask.recurrence)

        // Create a new task with the updated due date and reset completion status
        val newTask = completedTask.copy(
            id = 0, // Create as new task
            dueDate = nextDueDate,
            completed = false
        )

        // Add the new task
        taskViewModel.addTask(newTask)
    }

    // Helper method to calculate next due date
    private fun calculateNextDueDate(currentDueDate: String, recurrence: String): String {
        try {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val date = dateFormat.parse(currentDueDate) ?: return currentDueDate

            val calendar = Calendar.getInstance()
            calendar.time = date

            when (recurrence) {
                "DAILY" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            }

            return dateFormat.format(calendar.time)
        } catch (e: Exception) {
            return currentDueDate
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TASK = "task"

        fun newInstance(task: Task?): TaskDetailFragment {
            val fragment = TaskDetailFragment()
            if (task != null) {
                val args = Bundle()
                args.putParcelable(ARG_TASK, task)
                fragment.arguments = args
            }
            return fragment
        }
    }
}
