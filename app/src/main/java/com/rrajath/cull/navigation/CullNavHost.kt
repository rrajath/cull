package com.rrajath.cull.navigation

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
import com.rrajath.cull.core.database.WizardSegmentDb
import com.rrajath.cull.core.datastore.SettingsRepository
import com.rrajath.cull.feature.home.HomeScreen
import com.rrajath.cull.feature.library.LibraryScreen
import com.rrajath.cull.feature.settings.SettingsScreen
import com.rrajath.cull.feature.stacks.StackGridScreen
import com.rrajath.cull.feature.stacks.StacksScreen
import com.rrajath.cull.feature.viewer.ViewerScreen
import com.rrajath.cull.feature.wizard.WizardMonthScreen
import com.rrajath.cull.feature.wizard.WizardScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SessionViewModel : androidx.lifecycle.ViewModel() {
    var lastPhotoIndex by mutableStateOf(0)
    var lastFolderUri by mutableStateOf<String?>(null)
    var markedIds by mutableStateOf<Set<String>>(emptySet())
    var pinnedId by mutableStateOf<String?>(null)

    // Wizard segment the user is currently culling within; deletions are
    // attributed to this segment's deleted count
    var activeWizardMonthKey by mutableStateOf<String?>(null)

    private val _reloadTrigger = MutableStateFlow(0)
    val reloadTrigger: StateFlow<Int> = _reloadTrigger.asStateFlow()

    fun triggerReload() {
        _reloadTrigger.value++
    }

    fun saveSession(index: Int, folderUri: String?, settingsRepository: SettingsRepository) {
        viewModelScope.launch {
            settingsRepository.setLastPhotoIndex(index, folderUri ?: "")
        }
    }

    fun recordWizardDeletes(context: android.content.Context, deletedCount: Int) {
        val monthKey = activeWizardMonthKey ?: return
        if (deletedCount <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            WizardSegmentDb.getInstance(context).incrementDeletedCount(monthKey, deletedCount)
        }
    }
}

@Composable
fun CullNavHost(
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
                onNavigateToWizard = {
                    navController.navigate(Route.Wizard)
                },
                settingsRepository = settingsRepository
            )
        }

        composable<Route.Wizard> {
            sessionViewModel.activeWizardMonthKey = null
            WizardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMonthClick = { monthKey ->
                    navController.navigate(Route.WizardMonth(monthKey = monthKey))
                },
                settingsRepository = settingsRepository
            )
        }

        composable<Route.WizardMonth> { backStackEntry ->
            val wizardMonthRoute = backStackEntry.toRoute<Route.WizardMonth>()
            sessionViewModel.activeWizardMonthKey = wizardMonthRoute.monthKey
            WizardMonthScreen(
                monthKey = wizardMonthRoute.monthKey,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStackClick = { stackIndex ->
                    navController.navigate(Route.StackGrid(stackIndex = stackIndex))
                },
                reloadTrigger = sessionViewModel.reloadTrigger,
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
                onNavigateToStacks = {
                    navController.navigate(Route.Stacks)
                },
                reloadTrigger = sessionViewModel.reloadTrigger,
                settingsRepository = settingsRepository
            )
        }

        composable<Route.Stacks> {
            StacksScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStackClick = { stackIndex ->
                    navController.navigate(Route.StackGrid(stackIndex = stackIndex))
                },
                reloadTrigger = sessionViewModel.reloadTrigger,
                settingsRepository = settingsRepository
            )
        }

        composable<Route.StackGrid> { backStackEntry ->
            val stackGridRoute = backStackEntry.toRoute<Route.StackGrid>()
            StackGridScreen(
                stackIndex = stackGridRoute.stackIndex,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPhotoClick = { index, folderUri ->
                    navController.navigate(Route.Viewer(photoIndex = index, folderUri = folderUri))
                },
                reloadTrigger = sessionViewModel.reloadTrigger,
                settingsRepository = settingsRepository
            )
        }

        composable<Route.Viewer> { backStackEntry ->
            val viewerRoute = backStackEntry.toRoute<Route.Viewer>()
            val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
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
                onDeleteCompleted = { deletedCount ->
                    sessionViewModel.recordWizardDeletes(context, deletedCount)
                    sessionViewModel.triggerReload()
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
