package com.zametki.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zametki.data.*
import com.zametki.ui.NoteViewModel
import com.zametki.ui.components.*
import kotlinx.coroutines.launch

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
    var showViewMenu by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf<Note?>(null) }

    val filtered = remember(sorted, searchQuery) {
        if (searchQuery.isBlank()) sorted
        else sorted.filter { it.title.contains(searchQuery, true) || it.content.contains(searchQuery, true) }
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
                        if (showSearch) {
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
                        if (!showSearch) {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, null, tint = Color.White)
                            }
                            Box {
                                IconButton(onClick = { showViewMenu = true }) {
                                    Icon(if (viewMode == ViewMode.LIST) Icons.Default.ViewList else Icons.Default.GridView, null, tint = Color.White)
                                }
                                DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
                                    ViewMode.entries.forEach { m ->
                                        DropdownMenuItem(text = { Text(m.label) }, onClick = { viewModel.setViewMode(m); showViewMenu = false })
                                    }
                                }
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
                        onClick = { viewModel.createNote { id -> onNavigateToEditor(id) } },
                        containerColor = Accent, contentColor = Color.White
                    ) { Icon(Icons.Default.Add, "Новая заметка") }
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
                when (viewMode) {
                    ViewMode.LIST -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filtered, key = { it.id }) { note ->
                            NoteListItem(note, onClick = { onNavigateToEditor(note.id) }, onLongClick = { showContextMenu = note })
                        }
                    }
                    else -> {
                        val cols = if (viewMode == ViewMode.GRID_3) 3 else 4
                        LazyVerticalGrid(GridCells.Fixed(cols), Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtered, key = { it.id }) { note ->
                                NoteCard(note, onClick = { onNavigateToEditor(note.id) }, onLongClick = { showContextMenu = note })
                            }
                        }
                    }
                }
            }
        }
    }

    // Context menu
    val ctx = LocalContext.current
    showContextMenu?.let { note ->
        AlertDialog(
            onDismissRequest = { showContextMenu = null },
            title = { Text(note.title.ifBlank { "Без заголовка" }, maxLines = 1) },
            text = {
                Column {
                    if (listType == NoteListType.TRASH) {
                        TextButton(onClick = { viewModel.restore(note.id); showContextMenu = null }) { Text("Восстановить") }
                        TextButton(onClick = { viewModel.permanentlyDelete(note); showContextMenu = null }) { Text("Удалить навсегда", color = Color.Red) }
                    } else {
                        TextButton(onClick = { showContextMenu = null; onNavigateToEditor(note.id) }) { Text("Открыть") }
                        TextButton(onClick = { viewModel.toggleFavorite(note.id); showContextMenu = null }) {
                            Text(if (note.isFavorite) "Убрать из избранного" else "В избранное")
                        }
                        TextButton(onClick = { viewModel.togglePin(note.id); showContextMenu = null }) {
                            Text(if (note.isPinned) "Открепить" else "Закрепить")
                        }
                        TextButton(onClick = { viewModel.duplicateNote(note); showContextMenu = null }) { Text("Создать копию") }
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, note.title)
                                putExtra(Intent.EXTRA_TEXT, note.content)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Поделиться"))
                            showContextMenu = null
                        }) { Text("Поделиться") }
                        TextButton(onClick = { viewModel.softDelete(note.id); showContextMenu = null }) { Text("Удалить", color = Color.Red) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showContextMenu = null }) { Text("Закрыть") } }
        )
    }
}
