package com.rrajath.occullt.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.feature.home.HomeScreen
import com.rrajath.occullt.feature.library.LibraryScreen
import com.rrajath.occullt.feature.settings.SettingsScreen
import com.rrajath.occullt.feature.viewer.ViewerScreen

@Composable
fun OcculltNavHost(
    modifier: Modifier = Modifier,
    settingsRepository: SettingsRepository,
) {
    val navController = rememberNavController()

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
                    navController.navigate(Route.Viewer(photoIndex = index, folderUri = folderUri))
                }
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
                    navController.popBackStack()
                },
                settingsRepository = settingsRepository
            )
        }

        composable<Route.Settings> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
