package com.zametki.data

import androidx.compose.ui.graphics.Color

enum class SheetColor(val color: Color, val label: String) {
    WHITE(Color(0xFFFFFDF6), "Белый"),
    GREEN(Color(0xFF9DD1A1), "Зелёный"),
    BLUE(Color(0xFFB3E5FC), "Голубой"),
    PEACH(Color(0xFFFFAB91), "Персиковый"),
    PURPLE(Color(0xFFCFAEE3), "Сиреневый"),
    YELLOW(Color(0xFFE6DC97), "Жёлтый"),
    KRAFT(Color(0xFF998564), "Крафт");

    val textColor: Color get() = if (this == KRAFT) Color(0xFFFFFFFF) else Color(0xFF333333)
    val lineColor: Color get() = if (this == KRAFT) Color(0xFFBBA882).copy(alpha = 0.3f) else Color(0xFF8B7355).copy(alpha = 0.2f)
}

enum class SortMode(val label: String) {
    UPDATED_DESC("По дате изменения ↓"),
    UPDATED_ASC("По дате изменения ↑"),
    CREATED_DESC("По дате создания ↓"),
    CREATED_ASC("По дате создания ↑"),
    NAME_ASC("По названию А-Я"),
    NAME_DESC("По названию Я-А"),
    COLOR("По цвету")
}

enum class ViewMode {
    LIST, GRID_2, GRID_3;

    fun next(): ViewMode = when (this) {
        LIST -> GRID_2
        GRID_2 -> GRID_3
        GRID_3 -> LIST
    }
}
