package com.example.todocompose

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todocompose.data.UserPreferences
import com.example.todocompose.data.model.Todo
import com.example.todocompose.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class TodoFilter(val label: String) {
    ALL("Todas"),
    MY_TASKS("Mis Tareas"),
    PENDING("Pendientes"),
    COMPLETED("Completadas")
}

class TodoViewModel(
    private val repository: TodoRepository,
    private var userPreferences: UserPreferences? = null
) : ViewModel() {

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionLoadingId = MutableStateFlow<String?>(null)
    val actionLoadingId: StateFlow<String?> = _actionLoadingId.asStateFlow()

    private val _myTodoIds = MutableStateFlow<Set<String>>(emptySet())
    val myTodoIds: StateFlow<Set<String>> = _myTodoIds.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TodoFilter.ALL)
    val selectedFilter: StateFlow<TodoFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun initUserTracking(context: Context) {
        if (userPreferences == null) {
            userPreferences = UserPreferences(context)
        }
        _myTodoIds.value = userPreferences?.getMyTodoIds() ?: emptySet()
    }

    fun isUserCreated(id: String): Boolean {
        return _myTodoIds.value.contains(id)
    }

    fun claimTodo(id: String) {
        userPreferences?.addMyTodoId(id)
        _myTodoIds.value = _myTodoIds.value + id
    }

    fun setFilter(filter: TodoFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun loadTodos(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _loading.value = true
            }
            try {
                val result = repository.fetchTodos()
                _todos.value = result
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error al cargar tareas: ${e.message}", e)
                _snackbarMessage.value = "Error al obtener tareas: ${e.localizedMessage ?: "Fallo de conexión"}"
            } finally {
                _loading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun addTodo(title: String, description: String, onSuccess: () -> Unit = {}) {
        val trimmedTitle = title.trim()
        val trimmedDesc = description.trim()
        if (trimmedTitle.isBlank()) {
            _snackbarMessage.value = "El título no puede estar vacío"
            return
        }

        val newTodo = Todo(
            id = UUID.randomUUID().toString(),
            title = trimmedTitle,
            description = trimmedDesc,
            done = false
        )

        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.addTodo(newTodo)
                // Registramos el ID asignado por el servidor como creado por este usuario
                userPreferences?.addMyTodoId(result.id)
                _myTodoIds.value = _myTodoIds.value + result.id
                _todos.value = _todos.value + result
                _snackbarMessage.value = "Tarea agregada correctamente"
                onSuccess()
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error al agregar tarea: ${e.message}", e)
                _snackbarMessage.value = "Error al agregar tarea: ${e.localizedMessage ?: "Error en la petición"}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleTodoDone(todo: Todo) {
        val updated = todo.copy(done = !todo.done)
        viewModelScope.launch {
            _actionLoadingId.value = todo.id
            // Optimistic update para respuesta inmediata en la interfaz
            _todos.value = _todos.value.map { if (it.id == todo.id) updated else it }
            try {
                val result = repository.updateTodo(updated)
                _todos.value = _todos.value.map { if (it.id == todo.id) result else it }
                val estado = if (result.done) "completada" else "pendiente"
                _snackbarMessage.value = "Tarea marcada como $estado"
            } catch (e: Exception) {
                // Revertimos en caso de error
                _todos.value = _todos.value.map { if (it.id == todo.id) todo else it }
                Log.e("TodoViewModel", "Error al cambiar estado: ${e.message}", e)
                _snackbarMessage.value = "Error al cambiar estado: ${e.localizedMessage ?: "Fallo de red"}"
            } finally {
                _actionLoadingId.value = null
            }
        }
    }

    fun updateTodo(
        id: String,
        title: String,
        description: String,
        done: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val trimmedTitle = title.trim()
        val trimmedDesc = description.trim()
        if (trimmedTitle.isBlank()) {
            _snackbarMessage.value = "El título no puede estar vacío"
            return
        }

        val updatedTodo = Todo(
            id = id,
            title = trimmedTitle,
            description = trimmedDesc,
            done = done
        )

        viewModelScope.launch {
            _actionLoadingId.value = id
            try {
                val result = repository.updateTodo(updatedTodo)
                _todos.value = _todos.value.map { if (it.id == id) result else it }
                _snackbarMessage.value = "Tarea actualizada con éxito"
                onSuccess()
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error al modificar tarea: ${e.message}", e)
                _snackbarMessage.value = "Error al actualizar tarea: ${e.localizedMessage ?: "Error de red"}"
            } finally {
                _actionLoadingId.value = null
            }
        }
    }

    fun deleteTodo(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _actionLoadingId.value = id
            try {
                repository.deleteTodo(id)
                userPreferences?.removeMyTodoId(id)
                _myTodoIds.value = _myTodoIds.value - id
                _todos.value = _todos.value.filterNot { it.id == id }
                _snackbarMessage.value = "Tarea eliminada correctamente"
                onSuccess()
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error al eliminar tarea: ${e.message}", e)
                _snackbarMessage.value = "Error al eliminar tarea: ${e.localizedMessage ?: "Fallo de red"}"
            } finally {
                _actionLoadingId.value = null
            }
        }
    }
}
