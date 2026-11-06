package com.example.myapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoRepository(private val dao: TodoDao) {
    private val remote = FirebaseFirestore.getInstance().collection("todos")
    private var listenerRegistration: ListenerRegistration? = null

    val todos = dao.observeAll()

    fun startRealtimeSync(scope: kotlinx.coroutines.CoroutineScope) {
        listenerRegistration?.remove()
        listenerRegistration = remote.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseSync", "Eroare la ascultarea Firestore: ${error.message}", error)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val items = snapshot.documents.mapNotNull { it.toTodo() }
                scope.launch {
                    try {
                        dao.insertAll(items)
                        Log.d("FirebaseSync", "S-au sincronizat ${items.size} task-uri din Firestore în Room")
                    } catch (e: Exception) {
                        Log.e("FirebaseSync", "Eroare la salvarea în Room: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun stopRealtimeSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    suspend fun add(title: String, description: String, priority: Priority) {
        val item = TodoItem(
            title = title,
            description = description,
            priority = priority,
            position = dao.nextPosition()
        )
        val id = dao.insert(item).toInt()
        push(item.copy(id = id))
    }

    suspend fun update(item: TodoItem) {
        dao.update(item)
        push(item)
    }

    suspend fun delete(item: TodoItem) {
        dao.deleteById(item.id)
        remote.document(item.id.toString()).delete()
            .addOnSuccessListener { Log.d("FirebaseSync", "Task-ul ${item.id} a fost șters din Firestore") }
            .addOnFailureListener { e -> Log.e("FirebaseSync", "Eroare la ștergerea task-ului ${item.id} din Firestore", e) }
    }

    suspend fun deleteDone() {
        val done = dao.getDone()
        dao.deleteDone()
        done.forEach { item ->
            remote.document(item.id.toString()).delete()
                .addOnSuccessListener { Log.d("FirebaseSync", "Task-ul finalizat ${item.id} a fost șters din Firestore") }
                .addOnFailureListener { e -> Log.e("FirebaseSync", "Eroare la ștergerea task-ului ${item.id}", e) }
        }
    }

    suspend fun saveOrder(list: List<TodoItem>) {
        val ordered = list.mapIndexed { i, t -> t.copy(position = i) }
        dao.insertAll(ordered)
        ordered.forEach { push(it) }
    }

    private fun push(item: TodoItem) {
        remote.document(item.id.toString()).set(item.toMap())
            .addOnSuccessListener { Log.d("FirebaseSync", "Task trimis cu succes în Firestore: ${item.title} (ID: ${item.id})") }
            .addOnFailureListener { e -> Log.e("FirebaseSync", "Eroare la trimiterea în Firestore: ${e.message}", e) }
    }

    private fun TodoItem.toMap() = hashMapOf<String, Any>(
        "id" to id,
        "title" to title,
        "description" to description,
        "priority" to priority.name,
        "done" to done,
        "position" to position
    )

    private fun DocumentSnapshot.toTodo(): TodoItem? {
        val docId = id.toIntOrNull() ?: getLong("id")?.toInt() ?: return null
        val title = getString("title") ?: return null
        return TodoItem(
            id = docId,
            title = title,
            description = getString("description") ?: "",
            priority = runCatching { Priority.valueOf(getString("priority") ?: "MEDIUM") }
                .getOrDefault(Priority.MEDIUM),
            done = getBoolean("done") ?: false,
            position = (getLong("position") ?: 0L).toInt()
        )
    }
}

class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TodoRepository(AppDatabase.get(app).todoDao())

    val todos: StateFlow<List<TodoItem>> = repo.todos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        repo.startRealtimeSync(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        repo.stopRealtimeSync()
    }

    fun add(title: String, description: String, priority: Priority) =
        viewModelScope.launch { repo.add(title, description, priority) }

    fun update(item: TodoItem) = viewModelScope.launch { repo.update(item) }

    fun delete(item: TodoItem) = viewModelScope.launch { repo.delete(item) }

    fun deleteDone() = viewModelScope.launch { repo.deleteDone() }

    fun saveOrder(list: List<TodoItem>) = viewModelScope.launch { repo.saveOrder(list) }
}