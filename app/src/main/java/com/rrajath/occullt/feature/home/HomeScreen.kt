package com.rrajath.occullt.feature.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.component.ContinuePill
import com.rrajath.occullt.ui.component.GiantButton
import com.rrajath.occullt.ui.component.SourceMode
import com.rrajath.occullt.ui.component.SourceSwitcher
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.LocalExtendedColorScheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onContinueSession: (Int, String?) -> Unit,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExtendedColorScheme.current
    var sourceMode by remember { mutableStateOf(SourceMode.Hybrid) }
    var hasSession by remember { mutableStateOf(false) }
    var lastPhotoIndex by remember { mutableStateOf(0) }
    var lastFolderUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settingsRepository) {
        launch {
            settingsRepository.sourceMode.collectLatest { mode ->
                sourceMode = mode
            }
        }
        launch {
            settingsRepository.lastPhotoIndex.collectLatest { index ->
                lastPhotoIndex = index
                hasSession = index > 0
            }
        }
        launch {
            settingsRepository.lastFolderUri.collectLatest { uri ->
                lastFolderUri = uri
        }
    }
}

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CullMark()
                CircleIcon(
                    onClick = onNavigateToSettings,
                    icon = {
                        androidx.compose.material3.Icon(
                            imageVector = CullIcons.Settings,
                            contentDescription = "Settings",
                            tint = colors.fg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Keep only the",
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
                    color = colors.fg,
                    fontStyle = FontStyle.Italic,
                    fontSize = 54.sp,
                    lineHeight = 50.sp
                )
            )
            Text(
                text = "best.",
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
                    color = colors.accent,
                    fontStyle = FontStyle.Italic,
                    fontSize = 54.sp,
                    lineHeight = 50.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quickly skim through images, pin references, and compare. Delete the rest.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                    color = colors.fgDim,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.fillMaxWidth(0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            SourceSwitcher(
                selected = sourceMode,
                onSelectionChanged = { mode ->
                    sourceMode = mode
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (hasSession) {
                ContinuePill(
                    thumbnailContent = {},
                    label = "Continue where you left off",
                    onClick = { onContinueSession(lastPhotoIndex, lastFolderUri) },
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            GiantButton(
                title = "Library",
                subtitle = "Browse your photos",
                onClick = onNavigateToLibrary,
                accentBackground = true,
                minHeight = 160.dp,
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = CullIcons.Arrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            GiantButton(
                title = "Albums",
                subtitle = "Coming soon",
                onClick = {},
                accentBackground = false,
                enabled = false,
                minHeight = 118.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CULL V1.0.0",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                    color = colors.fgFaint,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CullMark(
    modifier: Modifier = Modifier,
) {
    val colors = LocalExtendedColorScheme.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cull",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
                color = colors.fg,
                fontStyle = FontStyle.Italic,
                fontSize = 28.sp,
                lineHeight = 28.sp
            )
        )
        Text(
            text = ">>",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
                color = colors.accent,
                fontStyle = FontStyle.Italic,
                fontSize = 28.sp,
                lineHeight = 28.sp
            )
        )
    }
}
