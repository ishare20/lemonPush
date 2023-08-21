package net.lemontree.push.model

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PCClientViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {

    private val todoItemsKey = "PCList"
    var pcItems = mutableStateListOf<PCClient>()
    fun addPCItem(pc: PCClient) {
        pcItems.add(pc)
        saveTodoItems()
    }

    init {
        loadTodoItems()
    }

    fun updatePCItem(idx: Int, pc: PCClient) {
        pcItems[idx] = pc
        saveTodoItems()
    }

    fun delItem(idx: Int) {
        pcItems.removeAt(idx)
        saveTodoItems()
    }

    // 从 SharedPreferences 加载
    private fun loadTodoItems() {
        val todoItemsJson = sharedPreferences.getString(todoItemsKey, null)
        todoItemsJson?.let {
            val items =
                Gson().fromJson<List<PCClient>>(it, object : TypeToken<List<PCClient>>() {}.type)
            pcItems.addAll(items)
        }
    }

    private fun saveTodoItems() {
        val todoItemsJson = Gson().toJson(pcItems)
        sharedPreferences.edit().putString(todoItemsKey, todoItemsJson).apply()
    }
}