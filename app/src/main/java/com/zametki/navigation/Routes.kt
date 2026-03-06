package com.zametki.navigation

object Routes {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val PINNED = "pinned"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{noteId}"
    fun editor(noteId: Long) = "editor/$noteId"
}
