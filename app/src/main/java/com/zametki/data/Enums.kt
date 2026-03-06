package com.zametki.data

import androidx.compose.ui.graphics.Color

enum class SheetColor(val color: Color, val label: String) {
    // Light pastel (6)
    WHITE(Color(0xFFFFFDF5), "Белый"),
    LIGHT_YELLOW(Color(0xFFFFF9C4), "Жёлтый"),
    LIGHT_PINK(Color(0xFFFFE4EC), "Розовый"),
    LIGHT_MINT(Color(0xFFE0F7E9), "Мятный"),
    LIGHT_BLUE(Color(0xFFE3F2FD), "Голубой"),
    LIGHT_LAVENDER(Color(0xFFEDE7F6), "Лавандовый"),
    // Slightly brighter pastel (6)
    WARM_CREAM(Color(0xFFF5E6CA), "Кремовый"),
    WARM_PEACH(Color(0xFFFFCCBC), "Персиковый"),
    WARM_SAGE(Color(0xFFC8E6C9), "Шалфей"),
    WARM_SKY(Color(0xFFB3E5FC), "Небесный"),
    WARM_CORAL(Color(0xFFFFAB91), "Коралловый"),
    WARM_LILAC(Color(0xFFD1C4E9), "Сиреневый");

    val textColor: Color get() = Color(0xFF333333)
    val lineColor: Color get() = Color(0xFF8B7355).copy(alpha = 0.2f)
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
    LIST, GRID_3, GRID_4;

    fun next(): ViewMode = when (this) {
        LIST -> GRID_3
        GRID_3 -> GRID_4
        GRID_4 -> LIST
    }
}
