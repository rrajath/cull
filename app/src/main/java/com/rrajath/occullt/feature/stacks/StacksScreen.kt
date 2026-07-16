package com.rrajath.occullt.feature.stacks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.grouping.stackKey
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.component.StackCard
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.ThemeColors
import kotlinx.coroutines.flow.StateFlow

@Composable
fun StacksScreen(
    onNavigateBack: () -> Unit,
    onStackClick: (Int) -> Unit,
    reloadTrigger: StateFlow<Int>,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: StacksViewModel = viewModel(
        factory = StacksViewModelFactory(LocalContext.current.applicationContext, settingsRepository)
    )
    val state by viewModel.state.collectAsState()
    val colors = ThemeColors.current
    val doneKeys by settingsRepository.doneStackKeys.collectAsState(initial = emptySet())

    LaunchedEffect(Unit) {
        viewModel.loadGroups()
    }

    LaunchedEffect(Unit) {
        reloadTrigger.collect { value ->
            if (value > 0) viewModel.onReloadTrigger(value)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIcon(
                onClick = onNavigateBack,
                icon = {
                    Icon(
                        imageVector = CullIcons.Arrow,
                        contentDescription = "Back",
                        tint = colors.fg,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Column {
                Text(
                    text = "Stacks",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        color = colors.fg,
                        fontSize = 28.sp
                    )
                )
                val subtitle = when {
                    state.isLoading -> "Loading..."
                    state.isEmpty -> "No bursts found"
                    else -> "${state.groups.size} stacks"
                }
                Text(
                    text = subtitle,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 12.sp
                    )
                )
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colors.accent,
                    modifier = Modifier.size(46.dp),
                    strokeWidth = 3.dp
                )
            }
        } else if (state.isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = CullIcons.Stacks,
                        contentDescription = null,
                        tint = colors.fgFaint,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No photo bursts found",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                            color = colors.fgDim,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = "Try adjusting the grouping window in Settings",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                            color = colors.fgFaint,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = state.groups,
                    key = { index, _ -> "stack_$index" }
                ) { index, stack ->
                    // hashing all photo ids is not free — compute once per stack,
                    // not on every recomposition
                    val key = remember(stack) { stackKey(stack.photos) }
                    StackCard(
                        stack = stack,
                        onClick = { onStackClick(index) },
                        isDone = doneKeys.contains(key)
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
