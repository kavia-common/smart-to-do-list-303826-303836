package org.example.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import org.example.app.R
import org.example.app.data.CategoryEntity
import org.example.app.data.TaskEntity
import java.util.Calendar
import java.util.Date

object TaskEditDialog {

    fun show(
        activity: Activity,
        initial: TaskEntity?,
        categoriesProvider: () -> List<CategoryEntity>,
        onSave: (TaskEntity) -> Unit,
        onDelete: ((TaskEntity) -> Unit)?
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_task_edit, null)

        val titleEditText = view.findViewById<EditText>(R.id.titleEditText)
        val descriptionEditText = view.findViewById<EditText>(R.id.descriptionEditText)
        val categorySpinner = view.findViewById<Spinner>(R.id.categorySpinner)
        val dueValueTextView = view.findViewById<TextView>(R.id.dueValueTextView)
        val pickDueButton = view.findViewById<ImageButton>(R.id.pickDueButton)
        val clearDueButton = view.findViewById<ImageButton>(R.id.clearDueButton)

        val categories = categoriesProvider()
        val spinnerNames = mutableListOf<String>().apply {
            add("None")
            addAll(categories.map { it.name })
        }
        val spinnerAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, spinnerNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categorySpinner.adapter = spinnerAdapter

        titleEditText.setText(initial?.title.orEmpty())
        descriptionEditText.setText(initial?.description.orEmpty())

        var dueMillis: Long? = initial?.dueAtMillis
        fun renderDue() {
            dueValueTextView.text = if (dueMillis == null) {
                "None"
            } else {
                DateFormat.format("yyyy-MM-dd HH:mm", Date(dueMillis!!)).toString()
            }
        }
        renderDue()

        // Set initial category selection
        val initialCatId = initial?.categoryId
        if (initialCatId == null) {
            categorySpinner.setSelection(0)
        } else {
            val idx = categories.indexOfFirst { it.id == initialCatId }
            categorySpinner.setSelection(if (idx >= 0) idx + 1 else 0)
        }

        pickDueButton.setOnClickListener {
            val cal = Calendar.getInstance()
            if (dueMillis != null) cal.timeInMillis = dueMillis!!

            DatePickerDialog(
                activity,
                { _, year, month, dayOfMonth ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    TimePickerDialog(
                        activity,
                        { _, hourOfDay, minute ->
                            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            cal.set(Calendar.MINUTE, minute)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            dueMillis = cal.timeInMillis
                            renderDue()
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        clearDueButton.setOnClickListener {
            dueMillis = null
            renderDue()
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(if (initial == null) "Add task" else "Edit task")
            .setView(view)
            .setPositiveButton("Save", null) // set later to avoid auto-dismiss on validation failure
            .setNegativeButton("Cancel", null)
            .apply {
                if (initial != null && onDelete != null) {
                    setNeutralButton("Delete") { _, _ -> onDelete(initial) }
                }
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleEditText.text?.toString()?.trim().orEmpty()
                val desc = descriptionEditText.text?.toString()?.trim().orEmpty()

                if (title.isBlank()) {
                    titleEditText.error = "Title is required"
                    titleEditText.requestFocus()
                    return@setOnClickListener
                }

                val selectedPos = categorySpinner.selectedItemPosition
                val selectedCategoryId = if (selectedPos <= 0) null else categories[selectedPos - 1].id

                val updated = if (initial == null) {
                    TaskEntity(
                        title = title,
                        description = desc,
                        dueAtMillis = dueMillis,
                        isCompleted = false,
                        categoryId = selectedCategoryId
                    )
                } else {
                    initial.copy(
                        title = title,
                        description = desc,
                        dueAtMillis = dueMillis,
                        categoryId = selectedCategoryId
                    )
                }

                onSave(updated)
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
