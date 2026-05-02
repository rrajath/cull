package com.rrajath.occullt.feature.library

import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.component.ContinuePill
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.LocalExtendedColorScheme

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

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.setFolderUri(it) }
    }

    LaunchedEffect(state.folderUri) {
        if (state.folderUri == null) {
            folderPickerLauncher.launch(null)
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
                if (state.folderUri != null) {
                    Text(
                        text = "${state.photos.size} photos",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                            color = colors.fgDim,
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "Select a folder",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                            color = colors.fgDim,
                            fontSize = 12.sp
                        )
                    )
                }
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
        } else if (state.folderUri == null) {
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
                        text = "Select a folder to browse photos",
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
                    text = "No photos found in this folder",
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
                            .clickable { onPhotoClick(index, state.folderUri) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photo.uri)
                                .crossfade(false)
                                .build(),
                            contentDescription = photo.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = if (isMarked) ColorFilter.tint(Color.Black.copy(alpha = 0.5f)) else null
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
                                    imageVector = CullIcons.X,
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
