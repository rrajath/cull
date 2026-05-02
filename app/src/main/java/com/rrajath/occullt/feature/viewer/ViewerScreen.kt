package com.rrajath.occullt.feature.viewer

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    photoIndex: Int,
    folderUri: String?,
    onNavigateBack: () -> Unit,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: ViewerViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val colors = LocalExtendedColorScheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    LaunchedEffect(settingsRepository) {
        settingsRepository.longPressThreshold.collectLatest { threshold ->
            viewModel.setLongPressThreshold(threshold)
        }
    }

    LaunchedEffect(settingsRepository) {
        settingsRepository.markedIds.collectLatest { ids ->
            viewModel.setMarkedIds(ids)
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
        val currentPhoto = photos.getOrNull(pagerState.currentPage)
        if (currentPhoto != null) {
            viewModel.setIsMarked(state.markedIds.contains(currentPhoto.id))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = state.scale <= 1f
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
                pinnedScale = state.pinnedScale,
                pinnedOffsetX = state.pinnedOffsetX,
                pinnedOffsetY = state.pinnedOffsetY,
                isLoading = state.isLoading,
                isMarked = state.isMarked,
                swipeOffsetY = state.swipeOffsetY,
                onToggleHud = { viewModel.toggleHud() },
                onLongPress = {
                    if (state.pinnedIndex == page) {
                        Toast.makeText(
                            context,
                            "Cannot compare a pinned image against itself",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
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
                onLoadingChanged = { viewModel.setLoading(it) },
                onMarkToggle = {
                    viewModel.toggleMarked(photo.id)
                    scope.launch {
                        settingsRepository.setMarkedIds(viewModel.state.value.markedIds)
                    }
                },
                onSwipeOffsetChanged = { offset ->
                    viewModel.setSwipeOffsetY(offset)
                },
                onSwipeRelease = { offset ->
                    if (kotlin.math.abs(offset) >= 60f) {
                        viewModel.toggleMarked(photo.id)
                        scope.launch {
                            settingsRepository.setMarkedIds(viewModel.state.value.markedIds)
                        }
                    }
                    viewModel.resetSwipeOffset()
                }
            )
        }

        AnimatedVisibility(
            visible = state.isHudOpen && !state.isLongPressing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            HudPill(
                markedCount = state.markedIds.size,
                isPinned = state.pinnedIndex != null,
                onTogglePin = {
                    val currentIndex = pagerState.currentPage
                    viewModel.setPinnedIndex(if (state.pinnedIndex == currentIndex) null else currentIndex)
                },
                onConfirm = {
                    viewModel.showDeleteDialog()
                }
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

        if (state.isMarked) {
            MarkedBanner(
                onUndo = {
                    val currentPhoto = photos.getOrNull(pagerState.currentPage)
                    if (currentPhoto != null) {
                        viewModel.undoMark(currentPhoto.id)
                        scope.launch {
                            settingsRepository.setMarkedIds(viewModel.state.value.markedIds)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        if (state.showDeleteDialog) {
            DeleteConfirmationDialog(
                markedCount = state.markedIds.size,
                onDismiss = { viewModel.hideDeleteDialog() },
                onConfirm = {
                    scope.launch {
                        viewModel.setDeleting(true)
                        val dryRun = settingsRepository.dryRun.first()
                        if (!dryRun) {
                            val contentResolver = context.contentResolver
                            val urisToDelete = state.markedIds.mapNotNull { id ->
                                photos.find { it.id == id }?.uri
                            }
                            deletePhotosViaMediaStore(contentResolver, urisToDelete)
                        }
                        delay(800)
                        viewModel.setDeleting(false)
                        viewModel.setDeleteSuccess(true)
                        if (!dryRun) {
                            viewModel.clearMarkedIds()
                            settingsRepository.setMarkedIds(emptySet())
                        } else {
                            viewModel.hideDeleteDialog()
                            Toast.makeText(
                                context,
                                "Dry run mode enabled. No photos were deleted.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                isDeleting = state.isDeleting,
                deleteSuccess = state.deleteSuccess
            )
        }
    }

    LaunchedEffect(folderUri) {
        settingsRepository.lastPhotoIndex.collectLatest { index ->
            if (index > 0 && folderUri != null) {
                settingsRepository.lastFolderUri.collectLatest { savedUri ->
                    if (savedUri == folderUri) {
                        viewModel.setCurrentIndex(index)
                    }
                }
            }
        }
    }
}

private fun deletePhotosViaMediaStore(contentResolver: ContentResolver, uris: List<Uri>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val pendingIntent = MediaStore.createTrashRequest(contentResolver, uris, true)
            pendingIntent.send()
        } catch (e: Exception) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                pendingIntent.send()
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    } else {
        uris.forEach { uri ->
            try {
                contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
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
    pinnedScale: Float,
    pinnedOffsetX: Float,
    pinnedOffsetY: Float,
    isLoading: Boolean,
    isMarked: Boolean,
    swipeOffsetY: Float,
    onToggleHud: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
    onSetPinned: () -> Unit,
    onZoomChanged: (Float, Float, Float) -> Unit,
    onResetZoom: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onMarkToggle: () -> Unit,
    onSwipeOffsetChanged: (Float) -> Unit,
    onSwipeRelease: (Float) -> Unit,
) {
    val colors = LocalExtendedColorScheme.current
    val context = LocalContext.current
    var pressStartTime by remember { mutableLongStateOf(0L) }

    val displayPhoto = if (isShowingPinned && pinnedPhoto != null) pinnedPhoto else photo
    val currentScale = if (isShowingPinned) pinnedScale else scale
    val currentOffsetX = if (isShowingPinned) pinnedOffsetX else offsetX
    val currentOffsetY = if (isShowingPinned) pinnedOffsetY else offsetY

    val grayscale by animateFloatAsState(
        targetValue = if (isMarked) 1f else 0f,
        label = "grayscale"
    )
    val brightness by animateFloatAsState(
        targetValue = if (isMarked) 0.85f else 1f,
        label = "brightness"
    )
    val translateY by animateFloatAsState(
        targetValue = if (isMarked) 18f else swipeOffsetY,
        label = "translateY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                translationY = translateY
            )
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
                            pressStartTime = System.currentTimeMillis()
                            onLongPress()
                        }
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f || pan.x != 0f || pan.y != 0f) {
                        val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                        if (newScale > 1f) {
                            val newX = currentOffsetX + pan.x
                            val newY = currentOffsetY + pan.y
                            onZoomChanged(newScale, newX, newY)
                        } else {
                            onZoomChanged(1f, 0f, 0f)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragCancel = {
                        onSwipeRelease(swipeOffsetY)
                    },
                    onDragEnd = {
                        onSwipeRelease(swipeOffsetY)
                    }
                ) { change, dragAmount ->
                    if (currentScale <= 1f) {
                        val newY = swipeOffsetY + dragAmount.y
                        onSwipeOffsetChanged(newY)
                        change.consume()
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = currentScale,
                    scaleY = currentScale,
                    translationX = currentOffsetX,
                    translationY = currentOffsetY,
                    colorFilter = if (isMarked && !isShowingPinned) {
                        ColorFilter.colorMatrix(
                            ColorMatrix(
                                floatArrayOf(
                                    0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                                    0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                                    0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )
                    } else null,
                    alpha = if (isMarked && !isShowingPinned) brightness else 1f
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

@Composable
private fun MarkedBanner(
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExtendedColorScheme.current

    Box(
        modifier = modifier
            .padding(top = 100.dp, end = 16.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.danger.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CullIcons.Trash,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Marked",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontSize = 12.sp
                )
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .combinedClickable(onClick = onUndo)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Undo",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmationDialog(
    markedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isDeleting: Boolean,
    deleteSuccess: Boolean,
) {
    val colors = LocalExtendedColorScheme.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgElev,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.lineStrong)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Delete $markedCount photo${if (markedCount != 1) "s" else ""}?",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    color = colors.fg,
                    fontSize = 24.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.bg)
                    .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Photos on device",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                color = colors.fgDim,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "$markedCount",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                color = colors.fg,
                                fontSize = 13.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Photos on Immich",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                color = colors.fgDim,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "0",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                color = colors.fgFaint,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(colors.bgElev2)
                        .border(1.dp, colors.line, RoundedCornerShape(32.dp))
                        .combinedClickable(
                            enabled = !isDeleting,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                            color = colors.fg,
                            fontSize = 14.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .then(
                            if (isDeleting || deleteSuccess) {
                                Modifier.background(colors.danger.copy(alpha = 0.7f))
                            } else {
                                Modifier.background(colors.danger)
                            }
                        )
                        .combinedClickable(
                            enabled = !isDeleting && !deleteSuccess,
                            onClick = onConfirm
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (deleteSuccess) {
                        Icon(
                            imageVector = CullIcons.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Move to Trash",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HudRail(
    markedCount: Int,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExtendedColorScheme.current

    Column(
        modifier = modifier
            .padding(end = 12.dp, top = 200.dp, bottom = 200.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.bgElev.copy(alpha = 0.9f))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isPinned) colors.pin else colors.bgElev2
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
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accent)
                .combinedClickable(onClick = onConfirm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CullIcons.Check,
                contentDescription = "Confirm",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun HudBar(
    markedCount: Int,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExtendedColorScheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    ),
                    startY = 0f,
                    endY = 200f
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.danger.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CullIcons.X,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "$markedCount marked",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPinned) colors.pin else Color.White.copy(alpha = 0.2f)
                        )
                        .combinedClickable(onClick = onTogglePin),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CullIcons.Pin,
                        contentDescription = if (isPinned) "Unpin" else "Pin",
                        tint = if (isPinned) Color.White else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.accent)
                        .combinedClickable(onClick = onConfirm)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Confirm",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

