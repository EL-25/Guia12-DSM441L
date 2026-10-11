package com.example.todocompose.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de preferencias para persistir los IDs de las tareas creadas por el usuario localmente.
 * Cumple con la restricción de solo modificar o eliminar los registros creados por el usuario.
 */
class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("todo_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MY_TODO_IDS = "my_created_todo_ids"
    }

    fun getMyTodoIds(): Set<String> {
        return prefs.getStringSet(KEY_MY_TODO_IDS, emptySet()) ?: emptySet()
    }

    fun addMyTodoId(id: String) {
        val current = getMyTodoIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet(KEY_MY_TODO_IDS, current).apply()
    }

    fun removeMyTodoId(id: String) {
        val current = getMyTodoIds().toMutableSet()
        current.remove(id)
        prefs.edit().putStringSet(KEY_MY_TODO_IDS, current).apply()
    }

    fun isMyTodo(id: String): Boolean {
        return getMyTodoIds().contains(id)
    }
}
