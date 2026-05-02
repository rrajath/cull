package com.rrajath.occullt.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.feature.home.HomeScreen
import com.rrajath.occullt.feature.library.LibraryScreen
import com.rrajath.occullt.feature.settings.SettingsScreen
import com.rrajath.occullt.feature.viewer.ViewerScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SessionViewModel : androidx.lifecycle.ViewModel() {
    var lastPhotoIndex by mutableStateOf(0)
    var lastFolderUri by mutableStateOf<String?>(null)
    var markedIds by mutableStateOf<Set<String>>(emptySet())
    var pinnedId by mutableStateOf<String?>(null)

    fun saveSession(index: Int, folderUri: String?, settingsRepository: SettingsRepository) {
        viewModelScope.launch {
            settingsRepository.setLastPhotoIndex(index, folderUri ?: "")
        }
    }
}

@Composable
fun OcculltNavHost(
    modifier: Modifier = Modifier,
    settingsRepository: SettingsRepository,
) {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = viewModel()

    var savedPhotoIndex by remember { mutableStateOf(0) }
    var savedFolderUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settingsRepository) {
        launch {
            settingsRepository.lastPhotoIndex.collectLatest { index ->
                savedPhotoIndex = index
            }
        }
        launch {
            settingsRepository.lastFolderUri.collectLatest { uri ->
                savedFolderUri = uri
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier
    ) {
        composable<Route.Home> {
            HomeScreen(
                onNavigateToLibrary = {
                    navController.navigate(Route.Library)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings)
                },
                onContinueSession = { index, folderUri ->
                    navController.navigate(Route.Viewer(photoIndex = index, folderUri = folderUri ?: savedFolderUri))
                },
                settingsRepository = settingsRepository
            )
        }

        composable<Route.Library> {
            LibraryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPhotoClick = { index, folderUri ->
                    navController.navigate(Route.Viewer(photoIndex = index, folderUri = folderUri))
                },
                settingsRepository = settingsRepository
            )
        }

        composable<Route.Viewer> { backStackEntry ->
            val viewerRoute = backStackEntry.toRoute<Route.Viewer>()
            ViewerScreen(
                photoIndex = viewerRoute.photoIndex,
                folderUri = viewerRoute.folderUri,
                onNavigateBack = {
                    sessionViewModel.saveSession(
                        sessionViewModel.lastPhotoIndex,
                        viewerRoute.folderUri,
                        settingsRepository
                    )
                    navController.popBackStack()
                },
                settingsRepository = settingsRepository
            )
        }

        composable<Route.Settings> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                settingsRepository = settingsRepository
            )
        }
    }
}
