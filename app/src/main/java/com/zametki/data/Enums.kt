package com.zametki.data

import androidx.compose.ui.graphics.Color

enum class SheetColor(val color: Color, val label: String) {
    // Default
    WHITE(Color(0xFFFFFDF5), "Белый"),
    // Pastel (10)
    PASTEL_PINK(Color(0xFFFFF0F5), "Розовый"),
    PASTEL_PEACH(Color(0xFFFFF5EE), "Персиковый"),
    PASTEL_YELLOW(Color(0xFFFFFDE7), "Жёлтый"),
    PASTEL_MINT(Color(0xFFF0FFF0), "Мятный"),
    PASTEL_BLUE(Color(0xFFF0F8FF), "Голубой"),
    PASTEL_LAVENDER(Color(0xFFF5F0FF), "Лавандовый"),
    PASTEL_CREAM(Color(0xFFFFFDD0), "Кремовый"),
    PASTEL_SAGE(Color(0xFFE8F5E9), "Шалфей"),
    PASTEL_SKY(Color(0xFFE1F5FE), "Небесный"),
    PASTEL_CORAL(Color(0xFFFFE4E1), "Коралловый"),
    // Vibrant (10)
    VIBRANT_RED(Color(0xFFEF5350), "Красный"),
    VIBRANT_ORANGE(Color(0xFFFF7043), "Оранжевый"),
    VIBRANT_YELLOW(Color(0xFFFFEE58), "Жёлтый"),
    VIBRANT_GREEN(Color(0xFF66BB6A), "Зелёный"),
    VIBRANT_TEAL(Color(0xFF26A69A), "Бирюзовый"),
    VIBRANT_BLUE(Color(0xFF42A5F5), "Синий"),
    VIBRANT_INDIGO(Color(0xFF5C6BC0), "Индиго"),
    VIBRANT_PURPLE(Color(0xFFAB47BC), "Фиолетовый"),
    VIBRANT_PINK(Color(0xFFEC407A), "Розовый"),
    VIBRANT_BROWN(Color(0xFF8D6E63), "Коричневый");

    val isDark: Boolean get() = name.startsWith("VIBRANT")
    val textColor: Color get() = if (isDark) Color.White else Color(0xFF333333)
    val lineColor: Color get() = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.15f)
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

enum class ViewMode(val label: String) {
    LIST("Список"),
    GRID_3("Сетка 3"),
    GRID_4("Сетка 4")
}
