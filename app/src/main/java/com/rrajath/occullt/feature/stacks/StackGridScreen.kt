package com.rrajath.occullt.feature.stacks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.rrajath.occullt.core.datastore.PhotoCache
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.grouping.stackKey
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.component.DoneLabel
import com.rrajath.occullt.ui.component.MarkDonePill
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.ThemeColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StackGridScreen(
    stackIndex: Int,
    onNavigateBack: () -> Unit,
    onPhotoClick: (Int, String?) -> Unit,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeColors.current
    val scope = rememberCoroutineScope()
    var stackPhotos by remember { mutableStateOf<List<UnifiedPhotoItem>>(emptyList()) }
    var title by remember { mutableStateOf("") }

    val doneKeys by settingsRepository.doneStackKeys.collectAsState(initial = emptySet())
    // Live mark/pin state so changes made in the viewer show immediately on return
    val markedIds by settingsRepository.markedIds.collectAsState(initial = emptySet())
    val pinnedId by settingsRepository.pinnedId.collectAsState(initial = null)
    val currentStackKey = remember(stackPhotos) {
        if (stackPhotos.isEmpty()) null else stackKey(stackPhotos)
    }
    val isDone = currentStackKey != null && doneKeys.contains(currentStackKey)

    LaunchedEffect(stackIndex) {
        val photos = PhotoStackCache.stacks[stackIndex]
        if (photos != null) {
            stackPhotos = photos
            val dateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US)
            val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
            val startDate = Date(photos.first().dateModified)
            val endDate = Date(photos.last().dateModified)
            title = if (SimpleDateFormat("yyyyMMdd", Locale.US).format(startDate)
                == SimpleDateFormat("yyyyMMdd", Locale.US).format(endDate)
            ) {
                "${dateFormat.format(startDate)} - ${timeFormat.format(endDate)}"
            } else {
                "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
            }
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
                    text = "Burst",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        color = colors.fg,
                        fontSize = 28.sp
                    )
                )
                Text(
                    text = title.ifEmpty { "${stackPhotos.size} photos" },
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 12.sp
                    )
                )
            }
            if (currentStackKey != null) {
                if (isDone) {
                    DoneLabel()
                } else {
                    MarkDonePill(onClick = {
                        scope.launch {
                            settingsRepository.addDoneStackKey(currentStackKey)
                            onNavigateBack()
                        }
                    })
                }
            }
        }

        if (stackPhotos.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                itemsIndexed(
                    items = stackPhotos,
                    key = { _, photo -> photo.id }
                ) { index, photo ->
                    val uri = when (photo.source) {
                        PhotoSource.Local -> "mediastore"
                        PhotoSource.Immich -> "immich"
                    }
                    val isMarked = markedIds.contains(photo.id)
                    val isPinned = pinnedId == photo.id

                    Box(
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.line.copy(alpha = 0.3f))
                            .clickable {
                                PhotoCache.setPhotos(stackPhotos)
                                onPhotoClick(index, uri)
                            }
                    ) {
                        val imageUrl = when (photo.source) {
                            PhotoSource.Local -> photo.uri
                            PhotoSource.Immich -> photo.thumbnailUrl ?: photo.previewUrl ?: photo.uri
                        }
                        val requestBuilder = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                        if (photo.source == PhotoSource.Local) {
                            requestBuilder.size(300, 300)
                        }

                        AsyncImage(
                            model = requestBuilder.build(),
                            contentDescription = photo.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = if (isMarked) ColorFilter.colorMatrix(
                                ColorMatrix().apply { setToSaturation(0f) }
                            ) else null
                        )

                        if (isPinned) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(colors.pin),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CullIcons.Pin,
                                    contentDescription = "Pinned",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (isMarked) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(colors.danger),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CullIcons.Trash,
                                    contentDescription = "Marked for deletion",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
