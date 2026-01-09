package org.example.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import org.example.app.data.CategoryEntity

object CategoryManageDialog {

    // PUBLIC_INTERFACE
    fun show(
        activity: Activity,
        categoriesProvider: () -> List<CategoryEntity>,
        onAdd: (String) -> Unit,
        onRename: (CategoryEntity, String) -> Unit,
        onDelete: (CategoryEntity) -> Unit
    ) {
        /** Show a category management dialog allowing add/rename/delete. */
        val categories = categoriesProvider().sortedBy { it.name.lowercase() }

        val listView = ListView(activity).apply {
            dividerHeight = 1
        }

        val adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_list_item_1,
            categories.map { it.name }
        )
        listView.adapter = adapter

        val emptyView = TextView(activity).apply {
            text = "No categories yet. Add one!"
            setPadding(32, 32, 32, 32)
        }
        listView.emptyView = emptyView

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(emptyView)
            addView(listView)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Categories")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                promptForName(
                    activity = activity,
                    title = "Add category",
                    initial = "",
                    onConfirm = { name -> onAdd(name) }
                )
            }
            .setNegativeButton("Close", null)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val cat = categories[position]
            AlertDialog.Builder(activity)
                .setTitle(cat.name)
                .setItems(arrayOf("Rename", "Delete")) { _, which ->
                    when (which) {
                        0 -> promptForName(
                            activity = activity,
                            title = "Rename category",
                            initial = cat.name,
                            onConfirm = { newName -> onRename(cat, newName) }
                        )

                        1 -> {
                            AlertDialog.Builder(activity)
                                .setTitle("Delete category?")
                                .setMessage("Tasks in this category will become uncategorized.")
                                .setPositiveButton("Delete") { _, _ -> onDelete(cat) }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    private fun promptForName(
        activity: Activity,
        title: String,
        initial: String,
        onConfirm: (String) -> Unit
    ) {
        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(initial)
            setSelection(text?.length ?: 0)
        }

        AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotBlank()) onConfirm(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
