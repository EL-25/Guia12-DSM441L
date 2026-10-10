package com.example.todocompose.repository

import com.example.todocompose.data.model.Todo
import com.example.todocompose.data.model.network.TodoApiService

class TodoRepository(private val api: TodoApiService) {
    suspend fun fetchTodos(): List<Todo> {
        return api.getTodos()
    }
    suspend fun addTodo(todo: Todo): Todo {
        return api.addTodo(todo)
    }
}
