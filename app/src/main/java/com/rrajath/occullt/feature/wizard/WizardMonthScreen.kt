package com.rrajath.occullt.feature.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rrajath.occullt.core.database.WizardSegmentState
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.grouping.stackKey
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.component.DoneLabel
import com.rrajath.occullt.ui.component.MarkDonePill
import com.rrajath.occullt.ui.component.StackCard
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.ThemeColors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WizardMonthScreen(
    monthKey: String,
    onNavigateBack: () -> Unit,
    onStackClick: (Int) -> Unit,
    reloadTrigger: StateFlow<Int>,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: WizardMonthViewModel = viewModel(
        factory = WizardMonthViewModelFactory(
            context = LocalContext.current.applicationContext,
            monthKey = monthKey,
            settingsRepository = settingsRepository,
        )
    )
    val state by viewModel.state.collectAsState()
    val colors = ThemeColors.current
    val scope = rememberCoroutineScope()
    val doneKeys by settingsRepository.doneStackKeys.collectAsState(initial = emptySet())
    var showCompleteSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.onEnter()
        viewModel.loadStacks()
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = colors.fg,
                        fontSize = 22.sp
                    )
                )
                val stateLabel = when (state.segmentState) {
                    WizardSegmentState.COMPLETE -> "Complete"
                    WizardSegmentState.IN_PROGRESS -> "In progress"
                    WizardSegmentState.NOT_STARTED -> "Not started"
                }
                val subtitle = if (state.isLoading) {
                    "Loading..."
                } else {
                    "${state.photoCount} photos · ${state.stacks.size} stacks · $stateLabel"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 12.sp
                    )
                )
            }
            when (state.segmentState) {
                WizardSegmentState.IN_PROGRESS -> {
                    MarkDonePill(onClick = { showCompleteSheet = true })
                }
                WizardSegmentState.COMPLETE -> {
                    DoneLabel(label = "Complete")
                }
                WizardSegmentState.NOT_STARTED -> {}
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    StatsRow(
                        photoCount = state.photoCount,
                        markedCount = state.markedCount,
                        deletedCount = state.deletedCount,
                    )
                }

                if (state.stacks.isEmpty()) {
                    item {
                        Text(
                            text = "No stacks in this period",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.fgFaint,
                                fontSize = 13.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = state.stacks,
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
                }

                if (state.segmentState == WizardSegmentState.IN_PROGRESS) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.wizardComplete)
                                .clickable { showCompleteSheet = true }
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Mark ${state.title} as complete",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = colors.wizardCompleteOn,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showCompleteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCompleteSheet = false },
            sheetState = sheetState,
            containerColor = colors.bgElev,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Mark as complete?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = colors.fg,
                        fontStyle = FontStyle.Italic,
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = "${state.title} will be marked complete. You can still cull photos from this period anytime via the Library.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.fgDim,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.wizardComplete)
                        .clickable {
                            scope.launch {
                                viewModel.markComplete()
                                showCompleteSheet = false
                                onNavigateBack()
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mark as complete",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = colors.wizardCompleteOn,
                            fontSize = 15.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, colors.lineStrong, RoundedCornerShape(14.dp))
                        .clickable { showCompleteSheet = false }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keep culling",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = colors.fg,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    photoCount: Int,
    markedCount: Int,
    deletedCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        StatCell(value = photoCount, label = "Photos", modifier = Modifier.weight(1f))
        StatCell(value = markedCount, label = "Marked", modifier = Modifier.weight(1f))
        StatCell(value = deletedCount, label = "Deleted", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgElev)
            .border(1.dp, colors.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                color = colors.fg,
                fontSize = 18.sp
            )
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = colors.fgFaint,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
