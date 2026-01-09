package org.example.app.repository

import org.example.app.data.CategoryDao
import org.example.app.data.CategoryEntity
import org.example.app.data.TaskDao
import org.example.app.data.TaskEntity

class TodoRepository(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao
) {
    suspend fun getCategories(): List<CategoryEntity> = categoryDao.getAll()

    suspend fun ensureCategoryExists(name: String): CategoryEntity {
        val existing = categoryDao.findByName(name)
        if (existing != null) return existing

        val newId = categoryDao.insert(CategoryEntity(name = name))
        // If IGNORE caused no insert, re-query.
        return categoryDao.findByName(name) ?: CategoryEntity(id = newId, name = name)
    }

    suspend fun addCategory(name: String): CategoryEntity = ensureCategoryExists(name)

    suspend fun renameCategory(categoryId: Long, newName: String) {
        categoryDao.updateName(categoryId = categoryId, newName = newName)
    }

    suspend fun deleteCategory(categoryId: Long) {
        categoryDao.deleteById(categoryId)
        // Tasks will be updated by Room FK (SET_NULL) on delete.
    }

    suspend fun getAllTasks(): List<TaskEntity> = taskDao.getAll()

    suspend fun searchAndFilterTasks(query: String, categoryId: Long?): List<TaskEntity> =
        taskDao.searchAndFilter(query = query.trim(), categoryId = categoryId)

    suspend fun addTask(task: TaskEntity): Long = taskDao.insert(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getById(id)

    suspend fun getDueTasksBy(untilMillis: Long): List<TaskEntity> = taskDao.getDueBy(untilMillis)
}
