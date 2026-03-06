package com.zametki.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zametki.data.SortMode
import com.zametki.data.ViewMode
import com.zametki.ui.NoteViewModel
import com.zametki.ui.components.Accent
import com.zametki.ui.components.BrownHeader
import com.zametki.ui.components.DarkBg
import com.zametki.ui.components.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: NoteViewModel, onNavigateBack: () -> Unit) {
    val sortMode by viewModel.sortMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    var showSortDialog by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }
    var showClearTrash by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrownHeader)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionTitle("Заметки")
            SettingsRow(Icons.Default.Sort, "Сортировка", sortMode.label) { showSortDialog = true }
            SettingsRow(Icons.Default.ViewModule, "Вид", viewMode.label) { showViewDialog = true }
            SectionTitle("Данные")
            SettingsRow(Icons.Default.DeleteSweep, "Очистить корзину", "Удалить все из корзины", isDestructive = true) { showClearTrash = true }
            SectionTitle("О приложении")
            SettingsRow(Icons.Default.Info, "Версия", "1.0") {}
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showSortDialog) {
        AlertDialog(onDismissRequest = { showSortDialog = false }, title = { Text("Сортировка") }, text = {
            Column { SortMode.entries.forEach { m ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(m == sortMode, { viewModel.setSortMode(m); showSortDialog = false })
                    Spacer(Modifier.width(8.dp)); Text(m.label)
                }
            } }
        }, confirmButton = {})
    }
    if (showViewDialog) {
        AlertDialog(onDismissRequest = { showViewDialog = false }, title = { Text("Вид") }, text = {
            Column { ViewMode.entries.forEach { m ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(m == viewMode, { viewModel.setViewMode(m); showViewDialog = false })
                    Spacer(Modifier.width(8.dp)); Text(m.label)
                }
            } }
        }, confirmButton = {})
    }
    if (showClearTrash) {
        AlertDialog(onDismissRequest = { showClearTrash = false },
            title = { Text("Очистить корзину?") },
            text = { Text("Все заметки из корзины будут удалены безвозвратно.") },
            confirmButton = { TextButton(onClick = { viewModel.emptyTrash(); showClearTrash = false }) { Text("Очистить", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showClearTrash = false }) { Text("Отмена") } })
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Accent,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isDestructive) Color.Red else Accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, color = if (isDestructive) Color.Red else Color(0xFFDDDDDD))
                if (subtitle.isNotBlank()) Text(subtitle, fontSize = 13.sp, color = Color(0xFF888888))
            }
        }
    }
}
