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

data class AppColors(
    val header: Color,
    val background: Color,
    val surface: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val iconDefault: Color,
    val divider: Color,
    val emptyIcon: Color,
    val uncheckedBox: Color,
    val bottomBarInactive: Color
)

val LightColors = AppColors(
    header = Color(0xFFBCA89F),
    background = Color(0xFFEDE5DB),
    surface = Color(0xFFE2D8CC),
    accent = Color(0xFF9C7B65),
    textPrimary = Color(0xFF4A3728),
    textSecondary = Color(0xFF8B7B6E),
    textMuted = Color(0xFFB0A396),
    iconDefault = Color(0xFF8B7B6E),
    divider = Color(0xFFD5CCC3),
    emptyIcon = Color(0xFFCBC2B9),
    uncheckedBox = Color(0xFFB0A396),
    bottomBarInactive = Color(0xFFCBC2B9)
)

val DarkColors = AppColors(
    header = Color(0xFF5D4E45),
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF2A2A2A),
    accent = Color(0xFFBCA89F),
    textPrimary = Color(0xFFE8E0D8),
    textSecondary = Color(0xFF999083),
    textMuted = Color(0xFF666666),
    iconDefault = Color(0xFFBBBBBB),
    divider = Color(0xFF444444),
    emptyIcon = Color(0xFF555555),
    uncheckedBox = Color(0xFF888888),
    bottomBarInactive = Color(0xFF555555)
)

// Legacy compat vars — updated via applyTheme()
var BrownHeader = LightColors.header
var DarkBg = LightColors.background
var DarkSurface = LightColors.surface
var Accent = LightColors.accent

fun applyTheme(isDark: Boolean) {
    val t = if (isDark) DarkColors else LightColors
    BrownHeader = t.header; DarkBg = t.background; DarkSurface = t.surface; Accent = t.accent
}

fun themeColors(isDark: Boolean): AppColors = if (isDark) DarkColors else LightColors

@Composable
fun AppDrawerContent(
    selectedItem: String,
    isDark: Boolean,
    onAll: () -> Unit,
    onFavorites: () -> Unit,
    onPinned: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit
) {
    val t = themeColors(isDark)
    ModalDrawerSheet(
        drawerContainerColor = t.surface,
        modifier = Modifier.width(280.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp).background(t.header),
            contentAlignment = Alignment.BottomStart
        ) {
            Text("Заметки", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = t.textPrimary,
                modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        DrawerItem(Icons.Default.Description, "Все заметки", selectedItem == "all", t, onAll)
        DrawerItem(Icons.Default.Favorite, "Избранное", selectedItem == "favorites", t, onFavorites)
        DrawerItem(Icons.Default.PushPin, "Закреплённые", selectedItem == "pinned", t, onPinned)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = t.divider)
        DrawerItem(Icons.Default.Delete, "Корзина", selectedItem == "trash", t, onTrash)
        DrawerItem(Icons.Default.Settings, "Настройки", selectedItem == "settings", t, onSettings)
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, selected: Boolean, t: AppColors, onClick: () -> Unit) {
    val bg = if (selected) t.accent.copy(alpha = 0.15f) else Color.Transparent
    val tint = if (selected) t.accent else t.iconDefault
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp)).background(bg)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = if (selected) t.accent else t.textPrimary)
    }
}
