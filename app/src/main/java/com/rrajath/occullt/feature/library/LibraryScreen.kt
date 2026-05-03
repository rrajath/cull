package com.rrajath.occullt.feature.library

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rrajath.occullt.core.datastore.PhotoCache
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.component.SourceMode
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.LocalExtendedColorScheme
import kotlinx.coroutines.flow.first

@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onPhotoClick: (Int, String?) -> Unit,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(LocalContext.current.applicationContext, settingsRepository)
    )
    val state by viewModel.state.collectAsState()
    val colors = LocalExtendedColorScheme.current
    val context = LocalContext.current

    var permissionGranted by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
        if (granted) {
            viewModel.loadPhotos()
        }
    }

    LaunchedEffect(Unit) {
        val sourceMode = settingsRepository.sourceMode.first()
        if (sourceMode == com.rrajath.occullt.ui.component.SourceMode.Immich) {
            permissionGranted = true
            viewModel.loadPhotos()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            permissionGranted = hasPermission
            if (hasPermission) {
                viewModel.loadPhotos()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
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
            Column {
                Text(
                    text = "Library",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        color = colors.fg,
                        fontSize = 28.sp
                    )
                )
                val subtitle = when {
                    state.isLoading -> "Loading..."
                    state.error != null -> state.error!!
                    else -> {
                        val sourceLabel = when (state.sourceMode) {
                            SourceMode.Local -> "Local"
                            SourceMode.Immich -> "Immich"
                            SourceMode.Hybrid -> "Hybrid"
                        }
                        "${state.photos.size} photos · $sourceLabel"
                    }
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
        } else if (state.error != null && state.photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = CullIcons.Folder,
                        contentDescription = null,
                        tint = colors.fgFaint,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = state.error ?: "No photos found",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                            color = colors.fgDim,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        } else if (state.photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No photos found",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        color = colors.fgDim,
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                itemsIndexed(state.photos) { index, photo ->
                    val isMarked = state.markedIds.contains(photo.id)
                    val isPinned = state.pinnedId == photo.id

                    Box(
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                PhotoCache.setPhotos(state.photos, state.folderUri)
                                val uri = when (photo.source) {
                                    PhotoSource.Local -> "mediastore"
                                    PhotoSource.Immich -> "immich"
                                }
                                onPhotoClick(index, uri)
                            }
                    ) {
                        val imageUrl = when (photo.source) {
                            PhotoSource.Local -> photo.uri
                            PhotoSource.Immich -> photo.previewUrl ?: photo.uri
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(false)
                                .build(),
                            contentDescription = photo.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = if (isMarked) ColorFilter.colorMatrix(
                                androidx.compose.ui.graphics.ColorMatrix().apply {
                                    setToSaturation(0f)
                                }
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

            if (state.markedIds.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 40.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.bgElev.copy(alpha = 0.9f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.markedIds.size} marked",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                color = colors.fgDim,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
