package com.rrajath.occullt.feature.viewer

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rrajath.occullt.core.datastore.LocalPhotoRepository
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.model.PhotoItem
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.LocalExtendedColorScheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    photoIndex: Int,
    folderUri: String?,
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER")
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: ViewerViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val colors = LocalExtendedColorScheme.current
    val context = LocalContext.current

    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var isLoadingPhotos by remember { mutableStateOf(true) }

    LaunchedEffect(folderUri) {
        if (folderUri != null) {
            val uri = Uri.parse(folderUri)
            val repo = LocalPhotoRepository(context)
            photos = repo.getPhotosFromFolder(uri)
            isLoadingPhotos = false
        }
    }

    if (isLoadingPhotos || photos.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = colors.accent,
                modifier = Modifier.size(46.dp),
                strokeWidth = 3.dp,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = photoIndex.coerceIn(0, photos.size - 1)) { photos.size }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentIndex(pagerState.currentPage)
        viewModel.setLoading(true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]
            ViewerPhotoPage(
                photo = photo,
                index = page,
                totalCount = photos.size,
                isPinned = state.pinnedIndex == page,
                isLongPressing = state.isLongPressing && state.pinnedIndex != null,
                isShowingPinned = state.isLongPressing && state.pinnedIndex != null,
                pinnedPhoto = state.pinnedIndex?.let { photos.getOrNull(it) },
                scale = state.scale,
                offsetX = state.offsetX,
                offsetY = state.offsetY,
                isLoading = state.isLoading,
                onToggleHud = { viewModel.toggleHud() },
                onLongPress = {
                    if (state.pinnedIndex != page) {
                        viewModel.setLongPressing(true)
                    }
                },
                onRelease = {
                    viewModel.setLongPressing(false)
                },
                onSetPinned = {
                    val currentIndex = pagerState.currentPage
                    viewModel.setPinnedIndex(if (state.pinnedIndex == currentIndex) null else currentIndex)
                },
                onZoomChanged = { s, ox, oy ->
                    viewModel.setZoom(s, ox, oy)
                },
                onResetZoom = { viewModel.resetZoom() },
                onLoadingChanged = { viewModel.setLoading(it) }
            )
        }

        AnimatedVisibility(
            visible = state.isHudOpen && !state.isLongPressing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            HudPill(
                markedCount = 0,
                isPinned = state.pinnedIndex != null,
                onTogglePin = {
                    val currentIndex = pagerState.currentPage
                    viewModel.setPinnedIndex(if (state.pinnedIndex == currentIndex) null else currentIndex)
                },
                onConfirm = {}
            )
        }

        AnimatedVisibility(
            visible = !state.isHudOpen && !state.isLongPressing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.bgElev.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Tap for HUD",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ViewerPhotoPage(
    photo: PhotoItem,
    index: Int,
    totalCount: Int,
    isPinned: Boolean,
    isLongPressing: Boolean,
    isShowingPinned: Boolean,
    pinnedPhoto: PhotoItem?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    isLoading: Boolean,
    onToggleHud: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
    onSetPinned: () -> Unit,
    onZoomChanged: (Float, Float, Float) -> Unit,
    onResetZoom: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
) {
    val colors = LocalExtendedColorScheme.current

    val displayPhoto = if (isShowingPinned && pinnedPhoto != null) pinnedPhoto else photo

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onToggleHud,
                onLongClick = {
                    if (!isPinned) {
                        onLongPress()
                    }
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (!isPinned) {
                            onLongPress()
                        }
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(displayPhoto.uri)
                    .crossfade(false)
                    .listener(
                        onSuccess = { _, _ -> onLoadingChanged(false) },
                        onError = { _, _ -> onLoadingChanged(false) }
                    )
                    .build(),
                contentDescription = displayPhoto.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.accent,
                        modifier = Modifier.size(46.dp),
                        strokeWidth = 3.dp,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 200f
                    )
                )
                .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${String.format("%04d", index + 1)} / $totalCount",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                )
            }

            if (isPinned) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.pin.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = CullIcons.Pin,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "PINNED",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (scale > 1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .combinedClickable(
                            onClick = onResetZoom
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Reset Both Zooms",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        ),
                        startY = 0f,
                        endY = 300f
                    )
                )
        )
    }
}

@Composable
private fun HudPill(
    markedCount: Int,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExtendedColorScheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.bgElev.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Local only",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.danger.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CullIcons.X,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "$markedCount marked",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 13.sp
                    )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPinned) colors.pin else colors.bgElev.copy(alpha = 0.85f)
                        )
                        .combinedClickable(onClick = onTogglePin),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CullIcons.Pin,
                        contentDescription = if (isPinned) "Unpin" else "Pin",
                        tint = if (isPinned) Color.White else colors.fgDim,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.accent)
                        .combinedClickable(onClick = onConfirm)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Confirm",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}
