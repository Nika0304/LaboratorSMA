package com.example.myapplication

import android.os.Bundle
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.myapplication.ui.theme.MyApplicationTheme

// ---------- ETAPA 1: modelul de date ----------
data class TodoItem(val id: Int, val text: String, val done: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TodoScreen()
            }
        }
    }
}

// ---------- ETAPA 2: ecranul + butonul de plus ----------
@Composable
fun TodoScreen() {
    val todos = remember { mutableStateListOf<TodoItem>() }
    var text by remember { mutableStateOf("") }
    var nextId by remember { mutableIntStateOf(0) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Task nou") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = {
                    if (text.isNotBlank()) {
                        todos.add(TodoItem(id = nextId++, text = text.trim()))
                        text = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Adaugă")
                }
            }

            Spacer(Modifier.height(16.dp))
            DraggableTodoList(todos)
        }
    }
}

// ---------- ETAPA 3: lista cu drag and drop (apăsare lungă + tragi) ----------
@Composable
fun DraggableTodoList(todos: SnapshotStateList<TodoItem>) {
    val listState = rememberLazyListState()
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                        offset.y.toInt() in it.offset..(it.offset + it.size)
                    }
                    if (hit != null) {
                        draggedIndex = hit.index
                        dragOffset = 0f
                    }
                },
                onDrag = { change, amount ->
                    change.consume()
                    val current = draggedIndex
                    if (current != null) {
                        dragOffset += amount.y
                        val infos = listState.layoutInfo.visibleItemsInfo
                        val currentInfo = infos.firstOrNull { it.index == current }
                        if (currentInfo != null) {
                            val center = currentInfo.offset + currentInfo.size / 2 + dragOffset
                            val target = infos.firstOrNull {
                                it.index != current && center.toInt() in it.offset..(it.offset + it.size)
                            }
                            if (target != null) {
                                todos.add(target.index, todos.removeAt(current))
                                val newOffset = if (target.index > current)
                                    target.offset + target.size - currentInfo.size
                                else target.offset
                                dragOffset += currentInfo.offset - newOffset
                                draggedIndex = target.index
                            }
                        }
                    }
                },
                onDragEnd = { draggedIndex = null; dragOffset = 0f },
                onDragCancel = { draggedIndex = null; dragOffset = 0f }
            )
        }
    ) {
        itemsIndexed(todos, key = { _, item -> item.id }) { index, item ->
            val isDragged = draggedIndex == index
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragged) dragOffset else 0f },
                elevation = CardDefaults.cardElevation(if (isDragged) 8.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.done,
                        onCheckedChange = { checked ->
                            val i = todos.indexOfFirst { it.id == item.id }
                            if (i >= 0) todos[i] = item.copy(done = checked)
                        }
                    )
                    Text(
                        text = item.text,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (item.done) TextDecoration.LineThrough else null
                    )
                    IconButton(onClick = { todos.remove(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Șterge")
                    }
                }
            }
        }
    }
}