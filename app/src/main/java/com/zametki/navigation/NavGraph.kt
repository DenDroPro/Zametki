package com.zametki.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zametki.ui.NoteViewModel
import com.zametki.ui.screens.editor.EditorScreen
import com.zametki.ui.screens.home.HomeScreen
import com.zametki.ui.screens.home.NoteListType
import com.zametki.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController, viewModel: NoteViewModel) {
    val goHome: () -> Unit = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
    val goFav: () -> Unit = { navController.navigate(Routes.FAVORITES) { popUpTo(Routes.HOME) } }
    val goPinned: () -> Unit = { navController.navigate(Routes.PINNED) { popUpTo(Routes.HOME) } }
    val goTrash: () -> Unit = { navController.navigate(Routes.TRASH) { popUpTo(Routes.HOME) } }
    val goSettings: () -> Unit = { navController.navigate(Routes.SETTINGS) { popUpTo(Routes.HOME) } }
    val goEditor: (Long) -> Unit = { id -> navController.navigate(Routes.editor(id)) }

    val isDark by viewModel.isDarkTheme.collectAsState()

    // Collect all notes for prev/next navigation in editor
    // Use stable ID set as key so sort order doesn't change when notes are edited (updatedAt changes)
    val allNotes by viewModel.allNotes.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val noteIdSet = remember(allNotes) { allNotes.map { it.id }.toSet() }
    val sortedNotes = remember(noteIdSet, sortMode) { viewModel.sortNotes(allNotes, sortMode) }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(viewModel, NoteListType.ALL, "Все заметки", isDark,
                onNavigateToEditor = goEditor, onNavigateBack = {},
                onNavigateToAll = goHome, onNavigateToFavorites = goFav,
                onNavigateToPinned = goPinned, onNavigateToTrash = goTrash,
                onNavigateToSettings = goSettings)
        }
        composable(Routes.FAVORITES) {
            HomeScreen(viewModel, NoteListType.FAVORITES, "Избранное", isDark,
                onNavigateToEditor = goEditor, onNavigateBack = { navController.popBackStack() },
                onNavigateToAll = goHome, onNavigateToFavorites = {},
                onNavigateToPinned = goPinned, onNavigateToTrash = goTrash,
                onNavigateToSettings = goSettings)
        }
        composable(Routes.PINNED) {
            HomeScreen(viewModel, NoteListType.PINNED, "Закреплённые", isDark,
                onNavigateToEditor = goEditor, onNavigateBack = { navController.popBackStack() },
                onNavigateToAll = goHome, onNavigateToFavorites = goFav,
                onNavigateToPinned = {}, onNavigateToTrash = goTrash,
                onNavigateToSettings = goSettings)
        }
        composable(Routes.TRASH) {
            HomeScreen(viewModel, NoteListType.TRASH, "Корзина", isDark,
                onNavigateToEditor = goEditor, onNavigateBack = { navController.popBackStack() },
                onNavigateToAll = goHome, onNavigateToFavorites = goFav,
                onNavigateToPinned = goPinned, onNavigateToTrash = {},
                onNavigateToSettings = goSettings)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(viewModel, isDark, onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.EDITOR, arguments = listOf(navArgument("noteId") { type = NavType.LongType })) { entry ->
            val noteId = entry.arguments?.getLong("noteId") ?: 0L
            val currentIndex = sortedNotes.indexOfFirst { it.id == noteId }
            EditorScreen(viewModel, noteId, isDark, onNavigateBack = { navController.popBackStack() },
                onNavigatePrev = {
                    if (currentIndex > 0) {
                        val prevId = sortedNotes[currentIndex - 1].id
                        navController.navigate(Routes.editor(prevId)) {
                            popUpTo(Routes.EDITOR) { inclusive = true }
                        }
                    }
                },
                onNavigateNext = {
                    if (currentIndex >= 0 && currentIndex < sortedNotes.size - 1) {
                        val nextId = sortedNotes[currentIndex + 1].id
                        navController.navigate(Routes.editor(nextId)) {
                            popUpTo(Routes.EDITOR) { inclusive = true }
                        }
                    }
                })
        }
    }
}
