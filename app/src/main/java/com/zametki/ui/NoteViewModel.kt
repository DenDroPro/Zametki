package com.zametki.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zametki.ZametkiApplication
import com.zametki.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ZametkiApplication.database.noteDao()

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

    private val _sortMode = MutableStateFlow(SortMode.UPDATED_DESC)
    val sortMode: StateFlow<SortMode> = _sortMode

    private val _viewMode = MutableStateFlow(ViewMode.GRID_3)
    val viewMode: StateFlow<ViewMode> = _viewMode

    fun loadNote(id: Long) {
        viewModelScope.launch {
            dao.getNoteById(id).collect { _currentNote.value = it }
        }
    }

    fun createNote(title: String = "", onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = dao.insert(Note(title = title.ifBlank { "" }))
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

    fun setSortMode(mode: SortMode) { _sortMode.value = mode }
    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }

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
