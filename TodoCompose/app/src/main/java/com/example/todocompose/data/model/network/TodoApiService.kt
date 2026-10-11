package com.example.todocompose.data.model.network

import com.example.todocompose.data.model.Todo
import retrofit2.http.*

interface TodoApiService {
    @GET("/to-do")
    suspend fun getTodos(): List<Todo>

    @POST("/to-do")
    suspend fun addTodo(@Body todo: Todo): Todo

    @PUT("/to-do/{id}")
    suspend fun updateTodo(@Path("id") id: String, @Body todo: Todo): Todo

    @DELETE("/to-do/{id}")
    suspend fun deleteTodo(@Path("id") id: String): Todo
}
