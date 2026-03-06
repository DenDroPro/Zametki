package com.zametki.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val preview: String = "",
    val formatting: String = "",
    val sheetColor: SheetColor = SheetColor.WHITE,
    val fontSize: Int = 16,
    val lineOpacity: Float = 0.15f,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
