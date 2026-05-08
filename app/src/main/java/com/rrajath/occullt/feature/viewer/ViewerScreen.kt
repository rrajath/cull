package com.rrajath.occullt.feature.viewer

import android.content.ContentResolver
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.rrajath.occullt.OcculltApplication
import com.rrajath.occullt.core.datastore.PhotoCache
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.database.ImmichAssetMappingDb
import com.rrajath.occullt.core.datastore.UnifiedPhotoRepository
import com.rrajath.occullt.core.datastore.UnifiedPhotoRepository.DeleteSummary
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import com.rrajath.occullt.core.network.ImmichApi
import com.rrajath.occullt.core.network.ImmichRepository
import com.rrajath.occullt.ui.component.SourceMode
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.ThemeColors
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
    onDeleteCompleted: () -> Unit,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: ViewerViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val colors = ThemeColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photos by remember { mutableStateOf<List<UnifiedPhotoItem>>(emptyList()) }
    var isLoadingPhotos by remember { mutableStateOf(true) }
    var immichApiKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(folderUri) {
        if (folderUri != null) {
            val sourceMode = settingsRepository.sourceMode.first()
            val immichUrl = settingsRepository.immichUrl.first()
            val key = settingsRepository.immichApiKey.first()

            if (sourceMode != SourceMode.Local && !immichUrl.isNullOrBlank() && !key.isNullOrBlank()) {
                OcculltApplication.setImmichApiKey(key)
                immichApiKey = key
            } else {
                OcculltApplication.setImmichApiKey(null)
                immichApiKey = null
            }

            val cached = PhotoCache.getPhotos()
            if (cached != null) {
                photos = cached
                isLoadingPhotos = false
            } else {
                isLoadingPhotos = true

                val immichRepo = if (sourceMode == SourceMode.Local) {
                    null
                } else {
                    if (!immichUrl.isNullOrBlank() && !key.isNullOrBlank()) {
                        val mappingDb = ImmichAssetMappingDb.getInstance(context)
                        ImmichRepository(ImmichApi(immichUrl, key), mappingDb)
                    } else {
                        null
                    }
                }

                val unifiedRepo = UnifiedPhotoRepository(context, immichRepo)
                val result = unifiedRepo.loadPhotos(sourceMode, folderUri)

                if (result.isSuccess) {
                    photos = result.getOrNull().orEmpty()
                }
                isLoadingPhotos = false
            }
        }
    }

    LaunchedEffect(settingsRepository) {
        settingsRepository.markedIds.collectLatest { ids ->
            viewModel.setMarkedIds(ids)
        }
    }

    LaunchedEffect(settingsRepository) {
        settingsRepository.pinnedId.collectLatest { id ->
            viewModel.setPinnedId(id)
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

    val currentPhoto = photos.getOrNull(pagerState.currentPage)
    val isCurrentPinned = currentPhoto?.let { state.pinnedId == it.id } ?: false

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentIndex(pagerState.currentPage)
        viewModel.setLoading(false)
        viewModel.setIsOnImmich(null)
        val currentPhoto = photos.getOrNull(pagerState.currentPage)
        if (currentPhoto != null) {
            viewModel.setIsMarked(state.markedIds.contains(currentPhoto.id))
            val sourceMode = settingsRepository.sourceMode.first()

            when {
                currentPhoto.isOnImmich -> {
                    viewModel.setIsOnImmich(true)
                }
                sourceMode == SourceMode.Local -> {
                    val immichUrl = settingsRepository.immichUrl.first()
                    val immichApiKey = settingsRepository.immichApiKey.first()
                    if (!immichUrl.isNullOrBlank() && !immichApiKey.isNullOrBlank()) {
                        launch {
                            val mappingDb = ImmichAssetMappingDb.getInstance(context)
                            val mapping = mappingDb.getByFileName(currentPhoto.name)
                            if (mapping != null) {
                                val immichApi = ImmichApi(immichUrl, immichApiKey)
                                val result = immichApi.getAsset(mapping.id)
                                viewModel.setIsOnImmich(result.isSuccess)
                            } else {
                                viewModel.setIsOnImmich(false)
                            }
                        }
                    } else {
                        viewModel.setIsOnImmich(false)
                    }
                }
                currentPhoto.source == PhotoSource.Immich -> {
                    viewModel.setIsOnImmich(true)
                }
                else -> {
                    val immichUrl = settingsRepository.immichUrl.first()
                    val immichApiKey = settingsRepository.immichApiKey.first()
                    if (!immichUrl.isNullOrBlank() && !immichApiKey.isNullOrBlank()) {
                        launch {
                            val mappingDb = ImmichAssetMappingDb.getInstance(context)
                            val mapping = mappingDb.getByFileName(currentPhoto.name)
                            if (mapping != null) {
                                val immichApi = ImmichApi(immichUrl, immichApiKey)
                                val result = immichApi.getAsset(mapping.id)
                                viewModel.setIsOnImmich(result.isSuccess)
                            } else {
                                viewModel.setIsOnImmich(false)
                            }
                        }
                    } else {
                        viewModel.setIsOnImmich(false)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
    }

    HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isCurrentPageZoomed
        ) { page ->
            val photo = photos[page]
            val isPinned = state.pinnedId == photo.id
            val pinnedPhoto = state.pinnedId?.let { pinnedId ->
                photos.find { it.id == pinnedId }
            }

            ViewerPhotoPage(
                photo = photo,
                index = page,
                totalCount = photos.size,
                isPinned = isPinned,
                isLongPressing = state.isLongPressing && state.pinnedId != null && state.pinnedId != photo.id,
                isShowingPinned = state.isLongPressing && state.pinnedId != null && state.pinnedId != photo.id,
                pinnedPhoto = pinnedPhoto,
                isLoading = state.isLoading,
                isMarked = state.isMarked,
                isOnImmich = state.isOnImmich,
                showPinnedBadge = isPinned || (state.isLongPressing && state.pinnedId == pinnedPhoto?.id),
                sourceMode = photos.getOrNull(pagerState.currentPage)?.source ?: PhotoSource.Local,
                immichApiKey = immichApiKey,
                onToggleHud = { viewModel.toggleHud() },
                onLongPress = {
                    if (state.pinnedId == photo.id) {
                        showToast(
                            context,
                            "Cannot compare a pinned image against itself"
                        )
                    } else {
                        viewModel.setLongPressing(true)
                    }
                },
                onLongPressRelease = {
                    viewModel.setLongPressing(false)
                },
                onTogglePin = {
                    val newPinnedId = if (state.pinnedId == photo.id) null else photo.id
                    viewModel.setPinnedId(newPinnedId)
                    scope.launch {
                        settingsRepository.setPinnedId(newPinnedId)
                    }
                },
                onLoadingChanged = { viewModel.setLoading(it) },
                onMarkToggle = {
                    viewModel.toggleMarked(photo.id)
                    scope.launch {
                        settingsRepository.setMarkedIds(viewModel.state.value.markedIds)
                    }
                },
                onZoomStateChanged = { zoomed ->
                    if (page == pagerState.currentPage) {
                        isCurrentPageZoomed = zoomed
                    }
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
                isPinned = isCurrentPinned,
                isMarked = state.isMarked,
                sourceMode = photos.getOrNull(pagerState.currentPage)?.source ?: PhotoSource.Local,
                onTogglePin = {
                    val currentPhoto = photos.getOrNull(pagerState.currentPage)
                    if (currentPhoto != null) {
                        val newPinnedId = if (state.pinnedId == currentPhoto.id) null else currentPhoto.id
                        viewModel.setPinnedId(newPinnedId)
                        scope.launch {
                            settingsRepository.setPinnedId(newPinnedId)
                        }
                    }
                },
                onToggleMark = {
                    val currentPhoto = photos.getOrNull(pagerState.currentPage)
                    if (currentPhoto != null) {
                        viewModel.toggleMarked(currentPhoto.id)
                        scope.launch {
                            settingsRepository.setMarkedIds(viewModel.state.value.markedIds)
                        }
                    }
                },
                onConfirm = {
                    viewModel.showDeleteDialog()
                },
                onClearMarks = {
                    viewModel.clearMarkedIds()
                    scope.launch {
                        settingsRepository.setMarkedIds(emptySet())
                    }
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

        if (state.showDeleteDialog) {
            val localMarkedCount = state.markedIds.count { id ->
                photos.find { it.id == id }?.isOnDevice == true
            }
            val immichMarkedCount = state.markedIds.count { id ->
                photos.find { it.id == id }?.isOnImmich == true
            }

            DeleteConfirmationDialog(
                markedCount = state.markedIds.size,
                localCount = localMarkedCount,
                immichCount = immichMarkedCount,
                onDismiss = { viewModel.hideDeleteDialog() },
                onConfirm = {
                    scope.launch {
                        viewModel.setDeleting(true)
                        val dryRun = settingsRepository.dryRun.first()
                        val mirrorDeletes = settingsRepository.mirrorDeletes.first()

                        var deleteSummary: DeleteSummary? = null

                        if (!dryRun) {
                            val immichUrl = settingsRepository.immichUrl.first()
                            val immichApiKey = settingsRepository.immichApiKey.first()

                            val immichRepo = if (!immichUrl.isNullOrBlank() && !immichApiKey.isNullOrBlank()) {
                                val mappingDb = ImmichAssetMappingDb.getInstance(context)
                                ImmichRepository(ImmichApi(immichUrl, immichApiKey), mappingDb)
                            } else {
                                null
                            }

                            val unifiedRepo = UnifiedPhotoRepository(context, immichRepo)
                            val photosToDelete = state.markedIds.mapNotNull { id ->
                                photos.find { it.id == id }
                            }

                            val result = unifiedRepo.deletePhotos(photosToDelete, mirrorDeletes)
                            deleteSummary = result.getOrNull()
                        }

                        delay(800)
                        viewModel.setDeleting(false)
                        viewModel.setDeleteSuccess(true)

                        if (dryRun) {
                            viewModel.hideDeleteDialog()
                            showToast(
                                context,
                                "Dry run mode enabled. No photos were deleted.",
                                longToast = true
                            )
                        } else {
                            viewModel.clearMarkedIds()
                            settingsRepository.setMarkedIds(emptySet())

                            val summary = deleteSummary
                            if (summary != null) {
                                val parts = mutableListOf<String>()
                                if (summary.localDeleted > 0) {
                                    parts.add("${summary.localDeleted} from device")
                                }
                                if (summary.immichDeleted > 0) {
                                    parts.add("${summary.immichDeleted} from Immich")
                                }
                                if (parts.isNotEmpty()) {
                                    showToast(context, parts.joinToString(", ") + " deleted")
                                }
                                if (summary.immichError != null) {
                                    showToast(
                                        context,
                                        "Failed to delete from Immich: ${summary.immichError}",
                                        longToast = true
                                    )
                                }
                            }
                            viewModel.hideDeleteDialog()
                            onDeleteCompleted()
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

@Composable
private fun ViewerPhotoPage(
    photo: UnifiedPhotoItem,
    index: Int,
    totalCount: Int,
    isPinned: Boolean,
    isLongPressing: Boolean,
    isShowingPinned: Boolean,
    pinnedPhoto: UnifiedPhotoItem?,
    isLoading: Boolean,
    isMarked: Boolean,
    isOnImmich: Boolean?,
    showPinnedBadge: Boolean,
    sourceMode: PhotoSource,
    immichApiKey: String?,
    onToggleHud: () -> Unit,
    onLongPress: () -> Unit,
    onLongPressRelease: () -> Unit,
    onTogglePin: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onMarkToggle: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit,
) {
    val colors = ThemeColors.current

    val displayPhoto = if (isShowingPinned && pinnedPhoto != null) pinnedPhoto else photo

    val brightness by animateFloatAsState(
        targetValue = if (isMarked) 0.85f else 1f,
        label = "brightness"
    )

    val colorMatrix = ColorMatrix().apply { setToSaturation(0f) }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val currentScale by rememberUpdatedState(scale)
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)

    LaunchedEffect(scale) {
        onZoomStateChanged(scale > 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onToggleHud,
                onLongClick = onLongPress,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                hapticFeedbackEnabled = false
            )
            .pointerInput(isLongPressing) {
                if (isLongPressing) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.none { it.pressed }) {
                                onLongPressRelease()
                                break
                }
            }
        }
    }
}
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    colorFilter = if (isMarked) ColorFilter.colorMatrix(colorMatrix) else null
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var zooming = false
                        var lastSpan = 0f
                        var lastCentroid = Offset.Zero
                        while (currentEvent.changes.any { it.pressed }) {
                            val event = awaitPointerEvent()
                            val pointers: List<PointerInputChange> = event.changes.filter { it.pressed }
                            if (pointers.size >= 2) {
                                zooming = true
                                val p1 = pointers[0].position
                                val p2 = pointers[1].position
                                val span = (p1 - p2).getDistance()
                                val centroid = (p1 + p2) / 2f
                                if (lastSpan > 0f) {
                                    val zoom = span / lastSpan
                                    val pan = centroid - lastCentroid
                                    val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                                    if (newScale == 1f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                    scale = newScale
                                }
                                lastSpan = span
                                lastCentroid = centroid
                                pointers.forEach { it.consume() }
                            } else if (zooming || currentScale > 1f) {
                                val pointer = pointers.firstOrNull()
                                if (pointer != null) {
                                    val pan = pointer.positionChange()
                                    if (pan.x != 0f || pan.y != 0f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                        pointer.consume()
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val imageUrl: Any = when (displayPhoto.source) {
                PhotoSource.Local -> displayPhoto.uri
                PhotoSource.Immich -> {
                    val base = displayPhoto.originalUrl ?: displayPhoto.previewUrl ?: displayPhoto.uri.toString()
                    if (!immichApiKey.isNullOrBlank()) {
                        "$base${if (base.contains("?")) "&" else "?"}apiKey=$immichApiKey"
                    } else {
                        base
                    }
                }
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .listener(
                        onSuccess = { _, _ -> onLoadingChanged(false) },
                        onError = { _, result ->
                            android.util.Log.e("ViewerPhotoPage", "Failed to load image: $imageUrl, error: ${result.throwable?.message}")
                            onLoadingChanged(false)
                        }
                    )
                    .build(),
                contentDescription = displayPhoto.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

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

        if (showPinnedBadge) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp, start = 16.dp)
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

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
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

        if (isOnImmich != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isOnImmich) CullIcons.Cloud else CullIcons.CloudOff,
                    contentDescription = if (isOnImmich) "Available on Immich" else "Not on Immich",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
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
    isMarked: Boolean,
    sourceMode: PhotoSource,
    onTogglePin: () -> Unit,
    onToggleMark: () -> Unit,
    onConfirm: () -> Unit,
    onClearMarks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeColors.current

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
                    text = when (sourceMode) {
                        PhotoSource.Local -> "Local only"
                        PhotoSource.Immich -> "Immich"
                    },
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
                        .background(colors.danger.copy(alpha = 0.3f))
                        .combinedClickable(
                            onClick = onClearMarks,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            hapticFeedbackEnabled = false
                        ),
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
                        .combinedClickable(
                            onClick = onTogglePin,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            hapticFeedbackEnabled = false
                        ),
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
                        .background(
                            if (isMarked) colors.danger else colors.bgElev.copy(alpha = 0.85f)
                        )
                        .combinedClickable(
                            onClick = onToggleMark,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            hapticFeedbackEnabled = false
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CullIcons.Trash,
                        contentDescription = if (isMarked) "Unmark" else "Mark for deletion",
                        tint = if (isMarked) Color.White else colors.fgDim,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.accent)
                        .combinedClickable(
                            onClick = onConfirm,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            hapticFeedbackEnabled = false
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmationDialog(
    markedCount: Int,
    localCount: Int,
    immichCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isDeleting: Boolean,
    deleteSuccess: Boolean,
) {
    val colors = ThemeColors.current
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
                            text = "$localCount",
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
                            text = "$immichCount",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                color = colors.fg,
                                fontSize = 13.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
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
    val colors = ThemeColors.current

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
    val colors = ThemeColors.current

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

private fun showToast(context: android.content.Context, message: String, longToast: Boolean = false) {
    val toast = Toast(context)
    val layout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(48, 20, 48, 20)
        val bg = GradientDrawable().apply {
            setColor(0xDD333333.toInt())
            cornerRadius = 32f
        }
        background = bg
    }
    val textView = TextView(context).apply {
        setText(message)
        setTextColor(android.graphics.Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
    }
    layout.addView(textView)
    toast.view = layout
    toast.duration = if (longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    toast.show()
}
