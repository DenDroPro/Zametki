package com.zametki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BrownHeader = Color(0xFF5D4037)
val DarkBg = Color(0xFF1A1A1A)
val DarkSurface = Color(0xFF2A2A2A)
val Accent = Color(0xFFD2691E)

@Composable
fun AppDrawerContent(
    selectedItem: String,
    onAll: () -> Unit,
    onFavorites: () -> Unit,
    onPinned: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = DarkSurface,
        modifier = Modifier.width(280.dp)
    ) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp).background(BrownHeader),
            contentAlignment = Alignment.BottomStart
        ) {
            Text("Заметки", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        DrawerItem(Icons.Default.Description, "Все заметки", selectedItem == "all", onAll)
        DrawerItem(Icons.Default.Favorite, "Избранное", selectedItem == "favorites", onFavorites)
        DrawerItem(Icons.Default.PushPin, "Закреплённые", selectedItem == "pinned", onPinned)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color(0xFF444444))
        DrawerItem(Icons.Default.Delete, "Корзина", selectedItem == "trash", onTrash)
        DrawerItem(Icons.Default.Settings, "Настройки", selectedItem == "settings", onSettings)
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Accent.copy(alpha = 0.15f) else Color.Transparent
    val tint = if (selected) Accent else Color(0xFFBBBBBB)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp)).background(bg)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = if (selected) Accent else Color(0xFFDDDDDD))
    }
}
