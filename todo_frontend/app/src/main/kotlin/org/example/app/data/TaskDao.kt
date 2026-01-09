package org.example.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {

    @Query(
        """
        SELECT * FROM tasks
        ORDER BY 
            CASE WHEN dueAtMillis IS NULL THEN 1 ELSE 0 END,
            dueAtMillis ASC,
            id DESC
        """
    )
    suspend fun getAll(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE
            (:query = "" OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        AND (
            :categoryId IS NULL OR categoryId = :categoryId
        )
        ORDER BY 
            CASE WHEN dueAtMillis IS NULL THEN 1 ELSE 0 END,
            dueAtMillis ASC,
            id DESC
        """
    )
    suspend fun searchAndFilter(query: String, categoryId: Long?): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE dueAtMillis IS NOT NULL AND isCompleted = 0 AND dueAtMillis <= :untilMillis")
    suspend fun getDueBy(untilMillis: Long): List<TaskEntity>
}
