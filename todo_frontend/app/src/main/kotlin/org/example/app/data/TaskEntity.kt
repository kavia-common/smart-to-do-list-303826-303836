package org.example.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("dueAtMillis"), Index("isCompleted")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,
    val description: String,

    /** Nullable due timestamp in epoch millis. */
    val dueAtMillis: Long? = null,

    val isCompleted: Boolean = false,

    /** Nullable category reference; SET_NULL when category removed. */
    val categoryId: Long? = null
)
