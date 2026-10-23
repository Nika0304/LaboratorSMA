package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myapplication.ui.theme.MyApplicationTheme

// ---------- ETAPA 1: modelul de date ----------
enum class Priority(val label: String, val color: Color) {
    LOW("Mică", Color(0xFF43A047)),
    MEDIUM("Medie", Color(0xFFFB8C00)),
    HIGH("Mare", Color(0xFFE53935))
}

data class TodoItem(
    val id: Int,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val done: Boolean = false
)

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

// ---------- ETAPA 2: ecranul principal + dialogul de adăugare/editare ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen() {
    val todos = remember { mutableStateListOf<TodoItem>() }
    var nextId by remember { mutableIntStateOf(0) }

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TodoItem?>(null) }

    val total = todos.size
    val doneCount = todos.count { it.done }
    val progress = if (total == 0) 0f else doneCount.toFloat() / total

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adaugă task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Task-urile mele",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (total == 0) "Nimic de făcut deocamdată"
                    else "$doneCount din $total finalizate",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
                if (doneCount > 0) {
                    TextButton(onClick = { todos.removeAll { it.done } }) {
                        Text("Șterge finalizate")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (todos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lista e goală.\nAdaugă primul task cu butonul +",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                DraggableTodoList(
                    todos = todos,
                    onEdit = {
                        editing = it
                        showDialog = true
                    }
                )
            }
        }
    }

    if (showDialog) {
        TodoDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onConfirm = { title, description, priority ->
                val current = editing
                if (current == null) {
                    todos.add(TodoItem(nextId++, title, description, priority))
                } else {
                    val i = todos.indexOfFirst { it.id == current.id }
                    if (i >= 0) {
                        todos[i] = todos[i].copy(
                            title = title,
                            description = description,
                            priority = priority
                        )
                    }
                }
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDialog(
    initial: TodoItem?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Priority) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: Priority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Task nou" else "Editează task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titlu") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descriere (opțional)") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Prioritate", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.values().forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onConfirm(title.trim(), description.trim(), priority) }
            ) {
                Text(if (initial == null) "Adaugă" else "Salvează")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anulează") }
        }
    )
}

// ---------- ETAPA 3: lista cu drag and drop (apăsare lungă + tragi) ----------
@Composable
fun DraggableTodoList(
    todos: SnapshotStateList<TodoItem>,
    onEdit: (TodoItem) -> Unit
) {
    val listState = rememberLazyListState()
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 88.dp),
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
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.done)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(if (isDragged) 12.dp else 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) dragOffset else 0f
                        val s = if (isDragged) 1.04f else 1f
                        scaleX = s
                        scaleY = s
                    }
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "≡",
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Checkbox(
                        checked = item.done,
                        onCheckedChange = { checked ->
                            val i = todos.indexOfFirst { it.id == item.id }
                            if (i >= 0) todos[i] = todos[i].copy(done = checked)
                        }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onEdit(item) }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (item.done) 0.45f else 1f
                            )
                        )
                        if (item.description.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                fontSize = 14.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(item.priority.color)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Prioritate ${item.priority.label.lowercase()}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = { todos.removeAll { it.id == item.id } }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Șterge",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}