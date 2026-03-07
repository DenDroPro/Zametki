package com.zametki.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zametki.ZametkiApplication
import com.zametki.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ZametkiApplication.database.noteDao()
    private val prefs = application.getSharedPreferences("zametki_prefs", Context.MODE_PRIVATE)

    val allNotes: StateFlow<List<Note>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val favoriteNotes: StateFlow<List<Note>> = dao.getFavoriteNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val pinnedNotes: StateFlow<List<Note>> = dao.getPinnedNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val deletedNotes: StateFlow<List<Note>> = dao.getDeletedNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote
    private var loadNoteJob: Job? = null

    private val _sortMode = MutableStateFlow(
        SortMode.entries.getOrNull(prefs.getInt("sort_mode", 0)) ?: SortMode.UPDATED_DESC
    )
    val sortMode: StateFlow<SortMode> = _sortMode

    private val _viewMode = MutableStateFlow(
        ViewMode.entries.getOrNull(prefs.getInt("view_mode", 1)) ?: ViewMode.GRID_2
    )
    val viewMode: StateFlow<ViewMode> = _viewMode

    // Default settings persisted in SharedPreferences
    private val _defaultFontSize = MutableStateFlow(prefs.getInt("default_font_size", 16))
    val defaultFontSize: StateFlow<Int> = _defaultFontSize

    private val _defaultSheetColor = MutableStateFlow(
        SheetColor.entries.getOrNull(prefs.getInt("default_sheet_color", 0)) ?: SheetColor.WHITE
    )
    val defaultSheetColor: StateFlow<SheetColor> = _defaultSheetColor

    private val _defaultLineOpacity = MutableStateFlow(prefs.getFloat("default_line_opacity", 0.5f))
    val defaultLineOpacity: StateFlow<Float> = _defaultLineOpacity

    private val _openNoteAfterCreate = MutableStateFlow(prefs.getBoolean("open_note_after_create", false))
    val openNoteAfterCreate: StateFlow<Boolean> = _openNoteAfterCreate

    fun loadNote(id: Long) {
        loadNoteJob?.cancel()
        _currentNote.value = null
        loadNoteJob = viewModelScope.launch {
            dao.getNoteById(id).collect { _currentNote.value = it }
        }
    }

    fun createNote(title: String = "", onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = dao.insert(Note(
                title = title.ifBlank { "" },
                sheetColor = _defaultSheetColor.value,
                fontSize = _defaultFontSize.value,
                lineOpacity = _defaultLineOpacity.value
            ))
            onCreated(id)
        }
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            dao.insert(note.copy(
                id = 0,
                title = "${note.title} — копия",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            dao.update(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun softDelete(id: Long) { viewModelScope.launch { dao.softDelete(id) } }
    fun restore(id: Long) { viewModelScope.launch { dao.restore(id) } }
    fun permanentlyDelete(note: Note) { viewModelScope.launch { dao.delete(note) } }
    fun toggleFavorite(id: Long) { viewModelScope.launch { dao.toggleFavorite(id) } }
    fun togglePin(id: Long) { viewModelScope.launch { dao.togglePin(id) } }
    fun changeColor(id: Long, color: SheetColor) { viewModelScope.launch { dao.changeColor(id, color) } }
    fun emptyTrash() { viewModelScope.launch { dao.emptyTrash() } }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
        prefs.edit().putInt("sort_mode", mode.ordinal).apply()
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        prefs.edit().putInt("view_mode", mode.ordinal).apply()
    }

    fun setDefaultFontSize(size: Int) {
        _defaultFontSize.value = size
        prefs.edit().putInt("default_font_size", size).apply()
    }

    fun setDefaultSheetColor(color: SheetColor) {
        _defaultSheetColor.value = color
        prefs.edit().putInt("default_sheet_color", color.ordinal).apply()
    }

    fun setDefaultLineOpacity(opacity: Float) {
        _defaultLineOpacity.value = opacity
        prefs.edit().putFloat("default_line_opacity", opacity).apply()
    }

    fun setOpenNoteAfterCreate(enabled: Boolean) {
        _openNoteAfterCreate.value = enabled
        prefs.edit().putBoolean("open_note_after_create", enabled).apply()
    }

    /**
     * Export all notes as JSON string for backup
     */
    suspend fun exportNotesJson(): String {
        val notes = allNotes.value
        val arr = JSONArray()
        for (n in notes) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("title", n.title)
            o.put("content", n.content)
            o.put("preview", n.preview)
            o.put("formatting", n.formatting)
            o.put("sheetColor", n.sheetColor.name)
            o.put("fontSize", n.fontSize)
            o.put("lineOpacity", n.lineOpacity.toDouble())
            o.put("isFavorite", n.isFavorite)
            o.put("isPinned", n.isPinned)
            o.put("isDeleted", n.isDeleted)
            o.put("createdAt", n.createdAt)
            o.put("updatedAt", n.updatedAt)
            arr.put(o)
        }
        return arr.toString()
    }

    /**
     * Import notes from JSON backup string
     */
    fun importNotesFromJson(json: String) {
        viewModelScope.launch {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val color = try { SheetColor.valueOf(o.getString("sheetColor")) } catch (_: Exception) { SheetColor.WHITE }
                    val note = Note(
                        title = o.optString("title", ""),
                        content = o.optString("content", ""),
                        preview = o.optString("preview", ""),
                        formatting = o.optString("formatting", ""),
                        sheetColor = color,
                        fontSize = o.optInt("fontSize", 16),
                        lineOpacity = o.optDouble("lineOpacity", 0.15).toFloat(),
                        isFavorite = o.optBoolean("isFavorite", false),
                        isPinned = o.optBoolean("isPinned", false),
                        isDeleted = o.optBoolean("isDeleted", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                    dao.insert(note)
                }
            } catch (_: Exception) {}
        }
    }

    fun sortNotes(notes: List<Note>, mode: SortMode): List<Note> {
        val pinned = notes.filter { it.isPinned }
        val rest = notes.filter { !it.isPinned }
        val sorted = when (mode) {
            SortMode.UPDATED_DESC -> rest.sortedByDescending { it.updatedAt }
            SortMode.UPDATED_ASC -> rest.sortedBy { it.updatedAt }
            SortMode.CREATED_DESC -> rest.sortedByDescending { it.createdAt }
            SortMode.CREATED_ASC -> rest.sortedBy { it.createdAt }
            SortMode.NAME_ASC -> rest.sortedBy { it.title.lowercase() }
            SortMode.NAME_DESC -> rest.sortedByDescending { it.title.lowercase() }
            SortMode.COLOR -> rest.sortedBy { it.sheetColor.ordinal }
        }
        return pinned.sortedByDescending { it.updatedAt } + sorted
    }
}
