package com.zametki.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromSheetColor(v: SheetColor): String = v.name
    @TypeConverter fun toSheetColor(v: String): SheetColor = try { SheetColor.valueOf(v) } catch (_: Exception) { SheetColor.WHITE }
}

@Database(entities = [Note::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
