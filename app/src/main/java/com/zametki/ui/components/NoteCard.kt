package com.zametki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zametki.data.Note
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onLongClick: () -> Unit) {
    val bgColor = note.sheetColor.color
    val textColor = note.sheetColor.textColor
    val lineColor = note.sheetColor.lineColor
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Faint lined paper background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineSpacing = 18.dp.toPx()
                val titleAreaH = 38.dp.toPx()
                // Faint content lines
                var y = titleAreaH + lineSpacing
                while (y < size.height) {
                    drawLine(lineColor.copy(alpha = 0.15f), Offset(8.dp.toPx(), y), Offset(size.width - 8.dp.toPx(), y), strokeWidth = 0.5.dp.toPx())
                    y += lineSpacing
                }
                // Title separator — more visible
                drawLine(lineColor.copy(alpha = 0.4f), Offset(8.dp.toPx(), titleAreaH), Offset(size.width - 8.dp.toPx(), titleAreaH), strokeWidth = 0.8.dp.toPx())
            }
            Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                // Title
                Text(
                    text = note.title.ifBlank { "Без заголовка" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                Spacer(Modifier.height(6.dp))
                // Preview text
                Text(
                    text = note.preview.ifBlank { "" },
                    fontSize = 10.sp,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor.copy(alpha = 0.7f),
                    lineHeight = 14.sp
                )
            }
            // Bottom row: date + icons
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(note.updatedAt)),
                    fontSize = 9.sp, color = textColor.copy(alpha = 0.5f)
                )
                Row {
                    if (note.isPinned) Icon(Icons.Default.PushPin, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                    if (note.isFavorite) Icon(Icons.Default.Favorite, null, tint = Color(0xFFE91E63).copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(note: Note, onClick: () -> Unit, onLongClick: () -> Unit) {
    val bgColor = note.sheetColor.color
    val textColor = note.sheetColor.textColor
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title.ifBlank { "Без заголовка" }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor)
                Text(note.preview.ifBlank { "Пустая заметка" }, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(note.updatedAt)), fontSize = 10.sp, color = textColor.copy(alpha = 0.4f))
                Row {
                    if (note.isPinned) Icon(Icons.Default.PushPin, null, tint = textColor.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                    if (note.isFavorite) Icon(Icons.Default.Favorite, null, tint = Color(0xFFE91E63), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
