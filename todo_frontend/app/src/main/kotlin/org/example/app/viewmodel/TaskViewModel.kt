package org.example.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.app.data.CategoryEntity
import org.example.app.data.TaskEntity
import org.example.app.repository.TodoRepository

class TaskViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    private val _tasks = MutableLiveData<List<TaskEntity>>(emptyList())
    val tasks: LiveData<List<TaskEntity>> = _tasks

    private val _categories = MutableLiveData<List<CategoryEntity>>(emptyList())
    val categories: LiveData<List<CategoryEntity>> = _categories

    val query = MutableLiveData("")
    val selectedCategoryName = MutableLiveData("All")

    private val _filteredTasks = MutableLiveData<List<TaskEntity>>(emptyList())
    val filteredTasks: LiveData<List<TaskEntity>> = _filteredTasks

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            loadCategories()
            loadTasks()
            applyFilters()
        }
    }

    fun setQuery(q: String) {
        query.value = q
        viewModelScope.launch { applyFilters() }
    }

    fun setSelectedCategoryName(name: String) {
        selectedCategoryName.value = name
        viewModelScope.launch { applyFilters() }
    }

    fun ensureDefaultCategory() {
        viewModelScope.launch {
            val current = repository.getCategories()
            if (current.isEmpty()) {
                repository.ensureCategoryExists("Personal")
                repository.ensureCategoryExists("Work")
            }
            loadCategories()
            applyFilters()
        }
    }

    fun addTask(task: TaskEntity, onInserted: (Long) -> Unit) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { repository.addTask(task) }
            loadTasks()
            applyFilters()
            onInserted(id)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.updateTask(task) }
            loadTasks()
            applyFilters()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteTask(task) }
            loadTasks()
            applyFilters()
        }
    }

    fun toggleComplete(task: TaskEntity) {
        updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    private suspend fun loadTasks() {
        val all = withContext(Dispatchers.IO) { repository.getAllTasks() }
        _tasks.postValue(all)
    }

    private suspend fun loadCategories() {
        val cats = withContext(Dispatchers.IO) { repository.getCategories() }
        _categories.postValue(cats)
    }

    private suspend fun applyFilters() {
        val q = query.value.orEmpty()
        val catName = selectedCategoryName.value ?: "All"

        val categoryId = if (catName == "All") {
            null
        } else {
            categories.value?.firstOrNull { it.name == catName }?.id
        }

        val results = withContext(Dispatchers.IO) {
            repository.searchAndFilterTasks(q, categoryId)
        }
        _filteredTasks.postValue(results)
    }
}
