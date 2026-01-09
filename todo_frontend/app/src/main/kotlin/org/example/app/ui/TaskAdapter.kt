package org.example.app.ui

import android.graphics.Paint
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.data.TaskEntity
import java.util.Date

class TaskAdapter(
    private val onToggleComplete: (TaskEntity) -> Unit,
    private val onEdit: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean = oldItem == newItem
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val completeCheckBox: CheckBox = itemView.findViewById(R.id.completeCheckBox)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val metaTextView: TextView = itemView.findViewById(R.id.metaTextView)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
    }

    /**
     * Updated by MainActivity so we can render category names without changing TaskEntity schema.
     * Key: categoryId, Value: categoryName
     */
    private var categoryNameById: Map<Long, String> = emptyMap()

    // PUBLIC_INTERFACE
    fun setCategoryNameMap(map: Map<Long, String>) {
        /** Provide category id->name mapping for richer item rendering. */
        categoryNameById = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)

        holder.titleTextView.text = task.title
        holder.titleTextView.paintFlags = if (task.isCompleted) {
            holder.titleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.titleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.completeCheckBox.setOnCheckedChangeListener(null)
        holder.completeCheckBox.isChecked = task.isCompleted
        holder.completeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            // Avoid redundant updates triggered by view recycling.
            if (isChecked != task.isCompleted) onToggleComplete(task)
        }

        val due = task.dueAtMillis?.let {
            val formatted = DateFormat.format("yyyy-MM-dd HH:mm", Date(it)).toString()
            "Due: $formatted"
        } ?: "No due date"

        val cat = task.categoryId?.let { id ->
            categoryNameById[id]?.let { "Category: $it" } ?: "Category: (deleted)"
        } ?: "No category"

        holder.metaTextView.text = "$due • $cat"

        holder.editButton.setOnClickListener { onEdit(task) }
        holder.itemView.setOnClickListener { onEdit(task) }
    }
}
