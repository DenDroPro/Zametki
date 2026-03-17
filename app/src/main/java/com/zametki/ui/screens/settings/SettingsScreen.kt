package com.zametki.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.zametki.R
import com.zametki.data.SheetColor
import com.zametki.data.SortMode
import com.zametki.data.ViewMode
import com.zametki.ui.NoteViewModel
import com.zametki.ui.components.Accent
import com.zametki.ui.components.BrownHeader
import com.zametki.ui.components.DarkBg
import com.zametki.ui.components.DarkSurface
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: NoteViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sortMode by viewModel.sortMode.collectAsState()
    val defaultFontSize by viewModel.defaultFontSize.collectAsState()
    val defaultSheetColor by viewModel.defaultSheetColor.collectAsState()
    val defaultLineOpacity by viewModel.defaultLineOpacity.collectAsState()
    val openAfterCreate by viewModel.openNoteAfterCreate.collectAsState()
    var showSortDialog by remember { mutableStateOf(false) }
    var showClearTrash by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var ydStatus by remember { mutableStateOf("") }

    // JSON file picker for restore
    val jsonFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader()?.readText() ?: ""
                    inputStream?.close()
                    viewModel.importNotesFromJson(json)
                    ydStatus = "Заметки восстановлены из бэкапа!"
                } catch (e: Exception) {
                    ydStatus = "Ошибка восстановления: ${e.message}"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold, color = Color(0xFF4A3728)) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF4A3728)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrownHeader)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionTitle("По умолчанию для новых заметок")

            // Default font size
            Surface(color = Color.Transparent) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatSize, null, tint = Accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Размер шрифта", fontSize = 16.sp, color = Color(0xFF4A3728))
                        Text("$defaultFontSize", fontSize = 13.sp, color = Color(0xFF8B7B6E))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (defaultFontSize > 10) viewModel.setDefaultFontSize(defaultFontSize - 1) }) {
                            Text("—", fontSize = 18.sp, color = Color(0xFF8B7B6E))
                        }
                        Text("$defaultFontSize", fontSize = 16.sp, color = Color(0xFF4A3728), modifier = Modifier.width(30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        IconButton(onClick = { if (defaultFontSize < 30) viewModel.setDefaultFontSize(defaultFontSize + 1) }) {
                            Text("+", fontSize = 18.sp, color = Color(0xFF8B7B6E))
                        }
                    }
                }
            }

            // Default sheet color
            SettingsRow(Icons.Default.Palette, "Цвет бумаги", defaultSheetColor.label) { showColorPicker = true }

            // Default line opacity
            Surface(color = Color.Transparent) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LinearScale, null, tint = Accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Яркость линий", fontSize = 16.sp, color = Color(0xFF4A3728))
                        Text("${(defaultLineOpacity * 100).toInt()}%", fontSize = 13.sp, color = Color(0xFF8B7B6E))
                    }
                }
            }
            Slider(
                value = defaultLineOpacity,
                onValueChange = { viewModel.setDefaultLineOpacity(it) },
                valueRange = 0f..1f,
                modifier = Modifier.padding(horizontal = 56.dp),
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
            )

            // Open note after creation
            Surface(onClick = { viewModel.setOpenNoteAfterCreate(!openAfterCreate) }, color = Color.Transparent) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, null, tint = Accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Открывать после создания", fontSize = 16.sp, color = Color(0xFF4A3728))
                        Text("Сразу открывать новый файл", fontSize = 13.sp, color = Color(0xFF8B7B6E))
                    }
                    Checkbox(
                        checked = openAfterCreate,
                        onCheckedChange = { viewModel.setOpenNoteAfterCreate(it) },
                        colors = CheckboxDefaults.colors(checkedColor = Accent, uncheckedColor = Color(0xFFB0A396))
                    )
                }
            }

            SectionTitle("Заметки")
            SettingsRow(Icons.Default.Sort, "Сортировка", sortMode.label) { showSortDialog = true }

            SectionTitle("Яндекс Диск")
            // Backup — create JSON file and share to YD via Intent
            SettingsRow(Icons.Default.CloudUpload, "Бэкап на Яндекс Диск", "Сохранить все заметки файлом") {
                scope.launch {
                    try {
                        val json = viewModel.exportNotesJson()
                        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                        val fileName = "zametki_backup_$date.json"
                        val cacheDir = File(context.cacheDir, "shared_notes")
                        cacheDir.mkdirs()
                        val file = File(cacheDir, fileName)
                        file.writeText(json)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, fileName)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            setPackage("ru.yandex.disk")
                        }
                        try {
                            context.startActivity(intent)
                            ydStatus = "Файл бэкапа отправлен на Яндекс Диск"
                        } catch (_: Exception) {
                            val chooser = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, fileName)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(chooser, "Сохранить бэкап"))
                        }
                    } catch (e: Exception) {
                        ydStatus = "Ошибка: ${e.message}"
                    }
                }
            }
            // Restore — pick JSON file
            SettingsRow(Icons.Default.CloudDownload, "Восстановить из бэкапа", "Открыть JSON-файл бэкапа") {
                jsonFilePicker.launch(arrayOf("application/json", "*/*"))
            }
            if (ydStatus.isNotBlank()) {
                Text(ydStatus, fontSize = 13.sp, color = Color(0xFF8B7B6E),
                    modifier = Modifier.padding(horizontal = 56.dp, vertical = 4.dp))
            }

            SectionTitle("Данные")
            SettingsRow(Icons.Default.DeleteSweep, "Очистить корзину", "Удалить все из корзины", isDestructive = true) { showClearTrash = true }

            SectionTitle("О приложении")
            SettingsRow(Icons.Default.Info, "Версия", "0.0.1") {}
            SettingsRow(Icons.Default.Person, "Разработчик", "DenDro") {}
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

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Цвет бумаги по умолчанию") },
            text = {
                Column {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SheetColor.entries.forEach { c ->
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(c.color)
                                    .then(if (c == defaultSheetColor) Modifier.border(2.dp, Accent, CircleShape) else Modifier)
                                    .clickable { viewModel.setDefaultSheetColor(c); showColorPicker = false },
                                contentAlignment = Alignment.Center
                            ) {
                                if (c == defaultSheetColor) Icon(Icons.Default.Check, null, tint = c.textColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text("Закрыть") } }
        )
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
                Text(title, fontSize = 16.sp, color = if (isDestructive) Color.Red else Color(0xFF4A3728))
                if (subtitle.isNotBlank()) Text(subtitle, fontSize = 13.sp, color = Color(0xFF8B7B6E))
            }
        }
    }
}
