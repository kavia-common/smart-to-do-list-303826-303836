package org.example.app

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.example.app.data.TodoDatabase
import org.example.app.notifications.NotificationScheduler
import org.example.app.repository.TodoRepository
import org.example.app.ui.TaskAdapter
import org.example.app.ui.TaskEditDialog
import org.example.app.viewmodel.TaskViewModel
import org.example.app.viewmodel.TaskViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    private lateinit var adapter: TaskAdapter

    private lateinit var searchEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var clearFiltersButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var addFab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure notification channel exists.
        NotificationScheduler.ensureNotificationChannel(this)

        setContentView(R.layout.activity_main)

        searchEditText = findViewById(R.id.searchEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        clearFiltersButton = findViewById(R.id.clearFiltersButton)
        recyclerView = findViewById(R.id.tasksRecyclerView)
        addFab = findViewById(R.id.addTaskFab)

        val db = TodoDatabase.getInstance(applicationContext)
        val repo = TodoRepository(db.taskDao(), db.categoryDao())
        val factory = TaskViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        adapter = TaskAdapter(
            onToggleComplete = { task ->
                viewModel.toggleComplete(task)
                // reschedule notification depending on completion state
                NotificationScheduler.scheduleOrCancelForTask(this, task.copy(isCompleted = !task.isCompleted))
            },
            onEdit = { task ->
                TaskEditDialog.show(
                    activity = this,
                    initial = task,
                    categoriesProvider = { viewModel.categories.value ?: emptyList() },
                    onSave = { updated ->
                        viewModel.updateTask(updated)
                        NotificationScheduler.scheduleOrCancelForTask(this, updated)
                    },
                    onDelete = { toDelete ->
                        confirmDelete(toDelete.id) {
                            viewModel.deleteTask(toDelete)
                            NotificationScheduler.cancelForTask(this, toDelete.id)
                        }
                    }
                )
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Observe categories then populate spinner.
        viewModel.categories.observe(this) { categories ->
            val names = mutableListOf<String>()
            names.add("All")
            names.addAll(categories.map { it.name })

            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            categorySpinner.adapter = spinnerAdapter

            // keep current selection if possible
            viewModel.selectedCategoryName.value?.let { selected ->
                val idx = names.indexOf(selected).takeIf { it >= 0 } ?: 0
                categorySpinner.setSelection(idx)
            }
        }

        viewModel.filteredTasks.observe(this) { tasks ->
            adapter.submitList(tasks)
        }

        // Search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        // Category filter
        categorySpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val selected = categorySpinner.selectedItem?.toString() ?: "All"
                viewModel.setSelectedCategoryName(selected)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        })

        clearFiltersButton.setOnClickListener {
            searchEditText.setText("")
            categorySpinner.setSelection(0)
            viewModel.setQuery("")
            viewModel.setSelectedCategoryName("All")
        }

        addFab.setOnClickListener {
            TaskEditDialog.show(
                activity = this,
                initial = null,
                categoriesProvider = { viewModel.categories.value ?: emptyList() },
                onSave = { created ->
                    viewModel.addTask(created) { newId ->
                        // schedule with the ID that Room generated
                        NotificationScheduler.scheduleOrCancelForTask(this, created.copy(id = newId))
                    }
                },
                onDelete = null
            )
        }

        // Seed a default category if empty (non-blocking).
        viewModel.ensureDefaultCategory()
    }

    private fun confirmDelete(taskId: Long, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete task?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
