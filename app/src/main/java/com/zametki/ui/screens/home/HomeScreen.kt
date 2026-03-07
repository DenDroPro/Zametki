package com.zametki.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.zametki.R
import com.zametki.data.*
import com.zametki.ui.NoteViewModel
import com.zametki.ui.components.*
import kotlinx.coroutines.launch
import java.io.File

enum class NoteListType { ALL, FAVORITES, PINNED, TRASH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel,
    listType: NoteListType,
    title: String,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAll: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPinned: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val sortMode by viewModel.sortMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val notesFlow = remember(listType) {
        when (listType) {
            NoteListType.ALL -> viewModel.allNotes
            NoteListType.FAVORITES -> viewModel.favoriteNotes
            NoteListType.PINNED -> viewModel.pinnedNotes
            NoteListType.TRASH -> viewModel.deletedNotes
        }
    }
    val notes by notesFlow.collectAsState()
    val sorted = remember(notes, sortMode) { viewModel.sortNotes(notes, sortMode) }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf<Note?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newNoteName by remember { mutableStateOf("") }
    var colorFilter by remember { mutableStateOf<SheetColor?>(null) }

    // Multi-select mode
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    fun exitSelection() { selectionMode = false; selectedIds.clear() }

    // Distinct colors that actually exist in notes
    val existingColors = remember(notes) {
        notes.map { it.sheetColor }.distinct().sortedBy { it.ordinal }
    }

    val filtered = remember(sorted, searchQuery, colorFilter) {
        var result = sorted
        if (searchQuery.isNotBlank()) result = result.filter { it.title.contains(searchQuery, true) || it.content.contains(searchQuery, true) }
        if (colorFilter != null) result = result.filter { it.sheetColor == colorFilter }
        result
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerItem = when (listType) {
        NoteListType.ALL -> "all"; NoteListType.FAVORITES -> "favorites"
        NoteListType.PINNED -> "pinned"; NoteListType.TRASH -> "trash"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(drawerItem,
                onAll = { scope.launch { drawerState.close() }; onNavigateToAll() },
                onFavorites = { scope.launch { drawerState.close() }; onNavigateToFavorites() },
                onPinned = { scope.launch { drawerState.close() }; onNavigateToPinned() },
                onTrash = { scope.launch { drawerState.close() }; onNavigateToTrash() },
                onSettings = { scope.launch { drawerState.close() }; onNavigateToSettings() })
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (showSearch) {
                            TextField(value = searchQuery, onValueChange = { searchQuery = it },
                                placeholder = { Text("Поиск...", color = Color(0xFF888888)) }, singleLine = true,
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth())
                        } else {
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                                Text("${filtered.size} заметок", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    },
                    navigationIcon = {
                        if (selectionMode) {
                            IconButton(onClick = { exitSelection() }) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        } else if (showSearch) {
                            IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        } else if (listType != NoteListType.ALL) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, null, tint = Color.White)
                            }
                        }
                    },
                    actions = {
                        if (selectionMode) {
                            Text("${selectedIds.size}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp))
                            // Share selected as .txt files via YD
                            IconButton(onClick = {
                                val selected = filtered.filter { it.id in selectedIds }
                                if (selected.isEmpty()) return@IconButton
                                try {
                                    val cacheDir = File(context.cacheDir, "shared_notes")
                                    cacheDir.mkdirs()
                                    val uris = ArrayList<Uri>()
                                    for (n in selected) {
                                        val name = n.title.ifBlank { "Без названия" }.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                                        val file = File(cacheDir, "$name.txt")
                                        file.writeText(n.content)
                                        uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                                    }
                                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "text/plain"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        setPackage("ru.yandex.disk")
                                    }
                                    context.startActivity(intent)
                                    exitSelection()
                                } catch (e: Exception) {
                                    try {
                                        val cacheDir = File(context.cacheDir, "shared_notes")
                                        cacheDir.mkdirs()
                                        val uris = ArrayList<Uri>()
                                        for (n in selected) {
                                            val name = n.title.ifBlank { "Без названия" }.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                                            val file = File(cacheDir, "$name.txt")
                                            file.writeText(n.content)
                                            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                                        }
                                        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                            type = "text/plain"
                                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Поделиться"))
                                        exitSelection()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Не удалось поделиться", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) { Icon(painterResource(R.drawable.ic_yandex_disk), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) }
                            // Generic share
                            IconButton(onClick = {
                                val selected = filtered.filter { it.id in selectedIds }
                                if (selected.isEmpty()) return@IconButton
                                try {
                                    val cacheDir = File(context.cacheDir, "shared_notes")
                                    cacheDir.mkdirs()
                                    val uris = ArrayList<Uri>()
                                    for (n in selected) {
                                        val name = n.title.ifBlank { "Без названия" }.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                                        val file = File(cacheDir, "$name.txt")
                                        file.writeText(n.content)
                                        uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                                    }
                                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "text/plain"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Поделиться"))
                                    exitSelection()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Не удалось поделиться", Toast.LENGTH_SHORT).show()
                                }
                            }) { Icon(Icons.Default.Share, null, tint = Color.White) }
                            // Delete selected
                            IconButton(onClick = {
                                selectedIds.forEach { viewModel.softDelete(it) }
                                exitSelection()
                            }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B)) }
                        } else if (!showSearch) {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, null, tint = Color.White)
                            }
                            // Select files button
                            IconButton(onClick = {
                                if (!selectionMode) {
                                    selectionMode = true
                                    selectedIds.clear()
                                }
                            }) {
                                Icon(Icons.Default.CheckBox, null, tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.setViewMode(viewMode.next()) }) {
                                Icon(
                                    when (viewMode) {
                                        ViewMode.LIST -> Icons.Default.ViewList
                                        ViewMode.GRID_2 -> Icons.Default.GridView
                                        ViewMode.GRID_3 -> Icons.Default.Apps
                                    }, null, tint = Color.White
                                )
                            }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Default.Sort, null, tint = Color.White)
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    SortMode.entries.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m.label, fontWeight = if (m == sortMode) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = { viewModel.setSortMode(m); showSortMenu = false })
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrownHeader)
                )
            },
            floatingActionButton = {
                if (listType != NoteListType.TRASH) {
                    FloatingActionButton(
                        onClick = { newNoteName = ""; showCreateDialog = true },
                        containerColor = Accent, contentColor = Color.White
                    ) { Icon(Icons.Default.Add, "Новая заметка") }
                }
            },
            bottomBar = {
                if (existingColors.size > 1) {
                    Surface(color = Color(0xFF2A2A2A), tonalElevation = 4.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "All" chip
                            Box(
                                Modifier.size(28.dp).clip(CircleShape)
                                    .background(if (colorFilter == null) Accent else Color(0xFF555555))
                                    .clickable { colorFilter = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Circle, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            existingColors.forEach { c ->
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape)
                                        .background(c.color)
                                        .then(if (c == colorFilter) Modifier.border(2.dp, Accent, CircleShape) else Modifier.border(1.dp, Color(0xFF555555), CircleShape))
                                        .clickable { colorFilter = if (colorFilter == c) null else c }
                                )
                            }
                        }
                    }
                }
            },
            containerColor = DarkBg
        ) { padding ->
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Description, null, Modifier.size(64.dp), tint = Color(0xFF555555))
                        Spacer(Modifier.height(16.dp))
                        Text("Нет заметок", fontSize = 18.sp, color = Color(0xFF777777))
                    }
                }
            } else {
                val onNoteClick: (Note) -> Unit = { note ->
                    if (selectionMode) {
                        if (note.id in selectedIds) selectedIds.remove(note.id) else selectedIds.add(note.id)
                        if (selectedIds.isEmpty()) selectionMode = false
                    } else {
                        onNavigateToEditor(note.id)
                    }
                }
                val onNoteLongClick: (Note) -> Unit = { note ->
                    showContextMenu = note
                }
                when (viewMode) {
                    ViewMode.LIST -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filtered, key = { it.id }) { note ->
                            val isSelected = note.id in selectedIds
                            Box {
                                NoteListItem(note, onClick = { onNoteClick(note) }, onLongClick = { onNoteLongClick(note) })
                                if (selectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onNoteClick(note) },
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp),
                                        colors = CheckboxDefaults.colors(checkedColor = Accent, uncheckedColor = Color(0xFF888888))
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        val cols = if (viewMode == ViewMode.GRID_2) 2 else 3
                        LazyVerticalGrid(GridCells.Fixed(cols), Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtered, key = { it.id }) { note ->
                                val isSelected = note.id in selectedIds
                                Box {
                                    NoteCard(note, onClick = { onNoteClick(note) }, onLongClick = { onNoteLongClick(note) })
                                    if (selectionMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { onNoteClick(note) },
                                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 2.dp),
                                            colors = CheckboxDefaults.colors(checkedColor = Accent, uncheckedColor = Color(0xFF888888))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Context menu — light theme
    showContextMenu?.let { note ->
        AlertDialog(
            onDismissRequest = { showContextMenu = null },
            containerColor = Color(0xFFF5F5F5),
            titleContentColor = Color(0xFF333333),
            title = { Text(note.title.ifBlank { "Без заголовка" }, maxLines = 1, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (listType == NoteListType.TRASH) {
                        TextButton(onClick = { viewModel.restore(note.id); showContextMenu = null }) { Text("Восстановить", color = Color(0xFF333333)) }
                        TextButton(onClick = { viewModel.permanentlyDelete(note); showContextMenu = null }) { Text("Удалить навсегда", color = Color.Red) }
                    } else {
                        TextButton(onClick = { showContextMenu = null; onNavigateToEditor(note.id) }) { Text("Открыть", color = Color(0xFF333333)) }
                        TextButton(onClick = { viewModel.toggleFavorite(note.id); showContextMenu = null }) {
                            Text(if (note.isFavorite) "Убрать из избранного" else "В избранное", color = Color(0xFF333333))
                        }
                        TextButton(onClick = { viewModel.togglePin(note.id); showContextMenu = null }) {
                            Text(if (note.isPinned) "Открепить" else "Закрепить", color = Color(0xFF333333))
                        }
                        TextButton(onClick = { viewModel.duplicateNote(note); showContextMenu = null }) { Text("Создать копию", color = Color(0xFF333333)) }
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, note.title)
                                putExtra(Intent.EXTRA_TEXT, note.content)
                            }
                            context.startActivity(Intent.createChooser(intent, "Поделиться"))
                            showContextMenu = null
                        }) { Text("Поделиться", color = Color(0xFF333333)) }
                        TextButton(onClick = {
                            showContextMenu = null
                            selectionMode = true
                            selectedIds.clear()
                            selectedIds.add(note.id)
                        }) { Text("Выделить", color = Color(0xFF333333)) }
                        TextButton(onClick = { viewModel.softDelete(note.id); showContextMenu = null }) { Text("Удалить", color = Color.Red) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showContextMenu = null }) { Text("Закрыть", color = Color(0xFF666666)) } }
        )
    }

    // Create note dialog — light theme
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFFF5F5F5),
            titleContentColor = Color(0xFF333333),
            title = { Text("Создать заметку") },
            text = {
                OutlinedTextField(
                    value = newNoteName,
                    onValueChange = { newNoteName = it },
                    label = { Text("Название", color = Color(0xFF666666)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF333333),
                        unfocusedTextColor = Color(0xFF333333),
                        cursorColor = Color(0xFF333333),
                        focusedBorderColor = Color(0xFF666666),
                        unfocusedBorderColor = Color(0xFFAAAAAA),
                        focusedLabelColor = Color(0xFF666666),
                        unfocusedLabelColor = Color(0xFF888888)
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newNoteName.trim().ifBlank { "Без названия" }
                    showCreateDialog = false
                    val shouldOpen = viewModel.openNoteAfterCreate.value
                    viewModel.createNote(name) { id -> if (shouldOpen) onNavigateToEditor(id) }
                }) { Text("Создать") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Отмена") } }
        )
    }
}
