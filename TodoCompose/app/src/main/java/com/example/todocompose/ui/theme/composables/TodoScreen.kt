package com.example.todocompose.ui.theme.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todocompose.TodoFilter
import com.example.todocompose.TodoViewModel
import com.example.todocompose.data.model.Todo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    val context = LocalContext.current
    val todos by viewModel.todos.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val actionLoadingId by viewModel.actionLoadingId.collectAsState()
    val myTodoIds by viewModel.myTodoIds.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog states
    var todoToEdit by remember { mutableStateOf<Todo?>(null) }
    var todoToDelete by remember { mutableStateOf<Todo?>(null) }
    var warningAction by remember { mutableStateOf<Pair<Todo, String>?>(null) }

    // Inicializar preferencias y cargar datos
    LaunchedEffect(Unit) {
        viewModel.initUserTracking(context)
        viewModel.loadTodos()
    }

    // Manejar mensajes snackbar
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    // Filtrar tareas por categoría y búsqueda
    val filteredTodos = remember(todos, selectedFilter, searchQuery, myTodoIds) {
        todos.filter { todo ->
            val matchesFilter = when (selectedFilter) {
                TodoFilter.ALL -> true
                TodoFilter.MY_TASKS -> viewModel.isUserCreated(todo.id)
                TodoFilter.PENDING -> !todo.done
                TodoFilter.COMPLETED -> todo.done
            }
            val matchesSearch = searchQuery.isBlank() ||
                    todo.title.contains(searchQuery, ignoreCase = true) ||
                    todo.description.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    val totalCount = todos.size
    val pendingCount = todos.count { !it.done }
    val completedCount = todos.count { it.done }
    val myCount = todos.count { viewModel.isUserCreated(it.id) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TodoCompose",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$pendingCount pendientes • $completedCount completadas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadTodos(isRefresh = true) },
                        enabled = !isRefreshing && !loading
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recargar tareas",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Buscador rápido
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar en mis tareas...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            // Resumen de Estadísticas
            TodoStatsRow(
                totalCount = totalCount,
                pendingCount = pendingCount,
                completedCount = completedCount,
                myCount = myCount
            )

            // Selector de Filtros
            FilterChipRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) },
                totalCount = totalCount,
                myCount = myCount,
                pendingCount = pendingCount,
                completedCount = completedCount
            )

            // Tarjeta para Crear Nueva Tarea
            AddTodoCard(
                onAddTodo = { title, desc ->
                    viewModel.addTodo(title, desc)
                },
                isSubmitting = loading && todos.isNotEmpty()
            )

            // Lista de Tareas o Indicador de Carga
            if (loading && todos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cargando tareas...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                TodoList(
                    todos = filteredTodos,
                    isUserCreated = { viewModel.isUserCreated(it) },
                    actionLoadingId = actionLoadingId,
                    onToggleDone = { todo ->
                        if (viewModel.isUserCreated(todo.id)) {
                            viewModel.toggleTodoDone(todo)
                        } else {
                            // Advertencia: solo modificar registros creados por el usuario
                            warningAction = Pair(todo, "cambiar estado")
                        }
                    },
                    onEditClick = { todo ->
                        if (viewModel.isUserCreated(todo.id)) {
                            todoToEdit = todo
                        } else {
                            // Advertencia: solo modificar registros creados por el usuario
                            warningAction = Pair(todo, "modificar")
                        }
                    },
                    onDeleteClick = { todo ->
                        if (viewModel.isUserCreated(todo.id)) {
                            todoToDelete = todo
                        } else {
                            // Advertencia: solo eliminar registros creados por el usuario
                            warningAction = Pair(todo, "eliminar")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Diálogo para Modificar Tarea
    todoToEdit?.let { todo ->
        EditTodoDialog(
            todo = todo,
            onDismiss = { todoToEdit = null },
            onConfirm = { newTitle, newDesc, newDone ->
                viewModel.updateTodo(
                    id = todo.id,
                    title = newTitle,
                    description = newDesc,
                    done = newDone,
                    onSuccess = { todoToEdit = null }
                )
            }
        )
    }

    // Diálogo para Confirmar Eliminación de Tarea
    todoToDelete?.let { todo ->
        DeleteConfirmDialog(
            todo = todo,
            onDismiss = { todoToDelete = null },
            onConfirm = {
                viewModel.deleteTodo(
                    id = todo.id,
                    onSuccess = { todoToDelete = null }
                )
            }
        )
    }

    // Diálogo de Advertencia de Registro Compartido
    warningAction?.let { (todo, action) ->
        OwnershipWarningDialog(
            todo = todo,
            actionName = action,
            onDismiss = { warningAction = null },
            onClaimAndProceed = {
                viewModel.claimTodo(todo.id)
                val pendingTodo = todo
                val currentAction = action
                warningAction = null
                when (currentAction) {
                    "cambiar estado" -> viewModel.toggleTodoDone(pendingTodo)
                    "modificar" -> todoToEdit = pendingTodo
                    "eliminar" -> todoToDelete = pendingTodo
                }
            }
        )
    }
}
