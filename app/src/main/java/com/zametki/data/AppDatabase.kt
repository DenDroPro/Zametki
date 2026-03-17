package com.zametki.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter fun fromSheetColor(v: SheetColor): String = v.name
    @TypeConverter fun toSheetColor(v: String): SheetColor = try { SheetColor.valueOf(v) } catch (_: Exception) { SheetColor.WHITE }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN pinOrder INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [Note::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
