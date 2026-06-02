package com.rrajath.occullt.feature.stacks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.ThemeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StacksScreen(
    onNavigateBack: () -> Unit,
    onStackClick: (Int) -> Unit,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: StacksViewModel = viewModel(
        factory = StacksViewModelFactory(LocalContext.current.applicationContext, settingsRepository)
    )
    val state by viewModel.state.collectAsState()
    val colors = ThemeColors.current

    LaunchedEffect(Unit) {
        viewModel.loadGroups()
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
                    StackCard(
                        stack = stack,
                        onClick = { onStackClick(index) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StackCard(
    stack: PhotoStack,
    onClick: () -> Unit,
) {
    val colors = ThemeColors.current
    val dateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    val startDate = Date(stack.startTime)
    val endDate = Date(stack.endTime)

    val timeRange = if (SimpleDateFormat("yyyyMMdd", Locale.US).format(startDate)
        == SimpleDateFormat("yyyyMMdd", Locale.US).format(endDate)
    ) {
        "${dateFormat.format(startDate)} - ${timeFormat.format(endDate)}"
    } else {
        "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgElev)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val firstPhoto = stack.photos.first()
            val imageUrl = when (firstPhoto.source) {
                PhotoSource.Local -> firstPhoto.uri
                PhotoSource.Immich -> firstPhoto.thumbnailUrl ?: firstPhoto.previewUrl ?: firstPhoto.uri
            }
            val requestBuilder = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
            if (firstPhoto.source == PhotoSource.Local) {
                requestBuilder.size(200, 200)
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.line.copy(alpha = 0.3f))
            ) {
                AsyncImage(
                    model = requestBuilder.build(),
                    contentDescription = firstPhoto.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${stack.photos.size}",
                        color = Color.White,
                        fontSize = 10.sp,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeRange,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = colors.fg,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "${stack.photos.size} photos",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = CullIcons.ChevronRight,
                contentDescription = "View stack",
                tint = colors.fgDim,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
