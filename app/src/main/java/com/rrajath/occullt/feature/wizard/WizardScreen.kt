package com.rrajath.occullt.feature.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rrajath.occullt.core.database.WizardSegmentState
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.ThemeColors

@Composable
fun WizardScreen(
    onNavigateBack: () -> Unit,
    onMonthClick: (String) -> Unit,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: WizardViewModel = viewModel(
        factory = WizardViewModelFactory(LocalContext.current.applicationContext, settingsRepository)
    )
    val state by viewModel.state.collectAsState()
    val colors = ThemeColors.current

    LaunchedEffect(Unit) {
        viewModel.load()
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
                    text = "Wizard",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = colors.fg,
                        fontSize = 28.sp
                    )
                )
                val subtitle = when {
                    state.isLoading -> "Loading..."
                    else -> "${state.visibleMonthCount} months · ${state.sourceMode.name}"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 12.sp
                    )
                )
            }
            if (state.isScanning) {
                CircularProgressIndicator(
                    color = colors.fgFaint,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
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
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (state.inProgress.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        SectionLabel("In progress")
                    }
                    items(
                        items = state.inProgress,
                        key = { "inprogress_${it.monthKey}" },
                        span = { GridItemSpan(3) }
                    ) { card ->
                        InProgressCard(
                            card = card,
                            onClick = { onMonthClick(card.monthKey) }
                        )
                    }
                    item(span = { GridItemSpan(3) }) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else if (!state.hasAnyProgress) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = "Tap any month to start culling.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.fgFaint,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                item(span = { GridItemSpan(3) }) {
                    SectionLabel("Coverage map")
                }

                item(span = { GridItemSpan(3) }) {
                    Legend()
                }

                state.years.forEach { yearGroup ->
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = yearGroup.year.toString(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = colors.fgFaint,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            ),
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }
                    items(
                        items = yearGroup.cells,
                        key = { it.monthKey }
                    ) { cell ->
                        MonthCell(
                            cell = cell,
                            onClick = { onMonthClick(cell.monthKey) }
                        )
                    }
                }

                item(span = { GridItemSpan(3) }) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = ThemeColors.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            color = colors.fgFaint,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        ),
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun InProgressCard(
    card: WizardInProgressCard,
    onClick: () -> Unit,
) {
    val colors = ThemeColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgElev)
            .border(1.5.dp, colors.wizardInProgress, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.fg,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = "${card.photoCount} photos · ${lastCulledLabel(card.lastAccessedAt)}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                imageVector = CullIcons.ChevronRight,
                contentDescription = "Continue ${card.title}",
                tint = colors.wizardInProgress,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun Legend() {
    val colors = ThemeColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        LegendItem(label = "Complete") {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.wizardComplete)
            )
        }
        LegendItem(label = "In progress") {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.wizardInProgress)
            )
        }
        LegendItem(label = "Not started") {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .border(1.dp, colors.lineStrong, CircleShape)
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, dot: @Composable () -> Unit) {
    val colors = ThemeColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dot()
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = colors.fgDim,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun MonthCell(
    cell: WizardMonthCell,
    onClick: () -> Unit,
) {
    val colors = ThemeColors.current
    val shape = RoundedCornerShape(12.dp)

    val cellModifier = when (cell.state) {
        WizardSegmentState.COMPLETE -> Modifier
            .clip(shape)
            .background(colors.wizardComplete)
        WizardSegmentState.IN_PROGRESS -> Modifier
            .clip(shape)
            .background(colors.bgElev)
            .border(1.5.dp, colors.wizardInProgress, shape)
        WizardSegmentState.NOT_STARTED -> Modifier
            .clip(shape)
            .background(colors.bgElev)
            .border(1.dp, colors.line, shape)
    }

    val labelColor = when (cell.state) {
        WizardSegmentState.COMPLETE -> colors.wizardCompleteOn
        WizardSegmentState.IN_PROGRESS -> colors.fg
        WizardSegmentState.NOT_STARTED -> colors.fgFaint
    }
    val countColor = when (cell.state) {
        WizardSegmentState.COMPLETE -> colors.wizardCompleteOnDim
        WizardSegmentState.IN_PROGRESS -> colors.fgDim
        WizardSegmentState.NOT_STARTED -> colors.fgFaint
    }

    Column(
        modifier = cellModifier
            .defaultMinSize(minHeight = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp)
    ) {
        Text(
            text = cell.label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = labelColor,
                fontSize = 13.sp
            )
        )
        Text(
            text = "${cell.photoCount} photos",
            style = MaterialTheme.typography.labelLarge.copy(
                color = countColor,
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun lastCulledLabel(lastAccessedAt: Long?): String {
    if (lastAccessedAt == null) return "not culled yet"
    val days = ((System.currentTimeMillis() - lastAccessedAt) / (24 * 60 * 60 * 1000L)).toInt()
    return when {
        days <= 0 -> "last culled today"
        days == 1 -> "last culled 1 day ago"
        else -> "last culled $days days ago"
    }
}
