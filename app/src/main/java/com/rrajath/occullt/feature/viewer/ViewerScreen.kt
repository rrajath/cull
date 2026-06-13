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
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.crossfade
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    photoIndex: Int,
    folderUri: String?,
    onNavigateBack: () -> Unit,
    onDeleteCompleted: (deletedCount: Int) -> Unit,
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
    var longPressThreshold by remember { mutableStateOf(220) }

    LaunchedEffect(settingsRepository) {
        settingsRepository.longPressThreshold.collectLatest { threshold ->
            longPressThreshold = threshold
        }
    }

    LaunchedEffect(folderUri) {
        if (folderUri != null) {
            val sourceMode = settingsRepository.sourceMode.first()
            val immichUrl = settingsRepository.immichUrl.first()
            val key = settingsRepository.immichApiKey.first()

            if (sourceMode != SourceMode.Local && !immichUrl.isNullOrBlank() && !key.isNullOrBlank()) {
                OcculltApplication.setImmichApiKey(key)
            } else {
                OcculltApplication.setImmichApiKey(null)
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

    // Cloud-badge lookups answered once per photo; without this every swipe
    // (including swiping back) re-ran a SQLite query plus a network round-trip
    val isOnImmichCache = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentIndex(pagerState.currentPage)
        viewModel.setLoading(false)
        viewModel.setIsOnImmich(null)
        val currentPhoto = photos.getOrNull(pagerState.currentPage)
        if (currentPhoto != null) {
            viewModel.setIsMarked(state.markedIds.contains(currentPhoto.id))

            when {
                currentPhoto.isOnImmich || currentPhoto.source == PhotoSource.Immich -> {
                    viewModel.setIsOnImmich(true)
                }
                else -> {
                    val cached = isOnImmichCache[currentPhoto.id]
                    if (cached != null) {
                        viewModel.setIsOnImmich(cached)
                        return@LaunchedEffect
                    }
                    val immichUrl = settingsRepository.immichUrl.first()
                    val immichApiKey = settingsRepository.immichApiKey.first()
                    if (!immichUrl.isNullOrBlank() && !immichApiKey.isNullOrBlank()) {
                        launch {
                            val onImmich = withContext(Dispatchers.IO) {
                                val mappingDb = ImmichAssetMappingDb.getInstance(context)
                                val mapping = mappingDb.getByFileName(currentPhoto.name)
                                if (mapping != null) {
                                    ImmichApi(immichUrl, immichApiKey).getAsset(mapping.id).isSuccess
                                } else {
                                    false
                                }
                            }
                            isOnImmichCache[currentPhoto.id] = onImmich
                            viewModel.setIsOnImmich(onImmich)
                        }
                    } else {
                        viewModel.setIsOnImmich(false)
                    }
                }
            }
        }
    }

    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
    }

    // Google-Photos-style vertical gestures: drag down to dismiss the viewer,
    // flick up to toggle the deletion mark. The drag is claimed only for a
    // mostly-vertical single-finger movement on an unzoomed photo, so the
    // pager's horizontal swipes, pinch zoom, and long-press compare are untouched.
    val dismissOffsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dismissDistancePx = with(density) { 140.dp.toPx() }
    val markDistancePx = with(density) { 110.dp.toPx() }
    val flingVelocityPx = with(density) { 800.dp.toPx() }

    val toggleMarkBySwipe: () -> Unit = {
        val photo = photos.getOrNull(pagerState.currentPage)
        if (photo != null) {
            viewModel.toggleMarked(photo.id)
            scope.launch {
                settingsRepository.setMarkedIds(viewModel.state.value.markedIds)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val progress = (dismissOffsetY.value / (size.height * 0.6f)).coerceIn(0f, 1f)
                drawRect(Color.Black.copy(alpha = 1f - 0.5f * progress))
            }
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    var dx = 0f
                    var dy = 0f
                    var claimed = false
                    var cancelled = false
                    while (true) {
                        // Observe after the children (Main pass) until the drag is
                        // ours, then intercept in the Initial pass so the pager and
                        // zoom handlers no longer see it
                        val event = awaitPointerEvent(
                            if (claimed) PointerEventPass.Initial else PointerEventPass.Main
                        )
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size > 1) {
                            cancelled = true
                            break
                        }
                        val change = pressed.first()
                        if (!claimed && (change.isConsumed || isCurrentPageZoomed || state.isLongPressing)) {
                            cancelled = true
                            break
                        }
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val delta = change.positionChange()
                        dx += delta.x
                        dy += delta.y
                        if (!claimed) {
                            if (abs(dx) > touchSlop && abs(dx) >= abs(dy)) {
                                // horizontal — the pager's
                                cancelled = true
                                break
                            }
                            if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                                claimed = true
                            }
                        }
                        if (claimed) {
                            change.consume()
                            val target = dismissOffsetY.value + delta.y
                            scope.launch { dismissOffsetY.snapTo(target) }
                        }
                    }
                    if (claimed) {
                        val offset = dismissOffsetY.value
                        val velocityY = velocityTracker.calculateVelocity().y
                        when {
                            !cancelled && (offset > dismissDistancePx ||
                                (offset > touchSlop && velocityY > flingVelocityPx)) -> {
                                // leave the photo where it was dragged while navigating out
                                onNavigateBack()
                            }
                            !cancelled && (offset < -markDistancePx ||
                                (offset < -touchSlop && velocityY < -flingVelocityPx)) -> {
                                toggleMarkBySwipe()
                                scope.launch { dismissOffsetY.animateTo(0f) }
                            }
                            else -> scope.launch { dismissOffsetY.animateTo(0f) }
                        }
                    }
                }
            }
    ) {

    HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = dismissOffsetY.value
                    // up-drag moves with resistance — it marks rather than dismisses
                    translationY = if (offset >= 0f) offset else offset * 0.35f
                    val progress = (offset / size.height).coerceIn(0f, 1f)
                    val shrink = 1f - 0.25f * progress
                    scaleX = shrink
                    scaleY = shrink
                },
            // pre-compose neighbors so their images are already loading when swiped to
            beyondViewportPageCount = 1,
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
                longPressThreshold = longPressThreshold,
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
            visible = state.isHudOpen && !state.isLongPressing && dismissOffsetY.value == 0f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            HudPill(
                markedCount = state.markedIds.size,
                isPinned = isCurrentPinned,
                isMarked = state.isMarked,
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
            visible = !state.isHudOpen && !state.isLongPressing && dismissOffsetY.value == 0f,
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
                        var deletedPhotoCount = 0

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
                            // photo count, not local+immich sum (hybrid photos exist in both)
                            deletedPhotoCount = photosToDelete.size
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
                            onDeleteCompleted(deletedPhotoCount)
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
    onToggleHud: () -> Unit,
    onLongPress: () -> Unit,
    onLongPressRelease: () -> Unit,
    onTogglePin: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onMarkToggle: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit,
    longPressThreshold: Int = 220,
) {
    val colors = ThemeColors.current

    val displayPhoto = if (isShowingPinned && pinnedPhoto != null) pinnedPhoto else photo

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
            .pointerInput(longPressThreshold) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    var dragged = false
                    val released = withTimeoutOrNull(longPressThreshold.toLong()) {
                        var event = awaitPointerEvent()
                        while (event.changes.any { it.pressed }) {
                            val change = event.changes.firstOrNull()
                            if (change != null && (change.position - downPos).getDistance() > touchSlop) {
                                dragged = true
                            }
                            event = awaitPointerEvent()
                        }
                        true
                    }
                    if (released == true && !dragged) {
                        // clean tap — wait briefly for a second one (double tap pins)
                        val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                            awaitFirstDown(requireUnconsumed = false)
                        }
                        if (secondDown == null) {
                            onToggleHud()
                        } else {
                            val secondDownPos = secondDown.position
                            var secondDragged = false
                            val secondReleased = withTimeoutOrNull(longPressThreshold.toLong()) {
                                var event = awaitPointerEvent()
                                while (event.changes.any { it.pressed }) {
                                    val change = event.changes.firstOrNull()
                                    if (change != null && (change.position - secondDownPos).getDistance() > touchSlop) {
                                        secondDragged = true
                                    }
                                    event = awaitPointerEvent()
                                }
                                true
                            }
                            if (secondReleased == true && !secondDragged) {
                                onTogglePin()
                            }
                        }
                    } else if (released == true || dragged) {
                        // finger moved past slop — a swipe (page, zoom-pan, or
                        // vertical mark/dismiss), never a compare hold
                    } else {
                        onLongPress()
                        while (true) {
                            val event = awaitPointerEvent()
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
            // Show the server-resized preview immediately; the multi-MB original
            // is fetched only once the user zooms in. Auth comes from the
            // x-api-key OkHttp interceptor (same as the grid thumbnails).
            var wantsOriginal by remember(displayPhoto.id) { mutableStateOf(false) }
            LaunchedEffect(scale) {
                if (scale > 1f) wantsOriginal = true
            }

            val previewUrl = displayPhoto.previewUrl ?: displayPhoto.originalUrl ?: displayPhoto.uri.toString()
            val imageData: Any = when (displayPhoto.source) {
                PhotoSource.Local -> displayPhoto.uri
                PhotoSource.Immich ->
                    if (wantsOriginal && displayPhoto.originalUrl != null) displayPhoto.originalUrl!! else previewUrl
            }
            // A lower-res version is usually already in Coil's memory cache
            // (grid thumbnail, or the preview when upgrading to the original) —
            // show it instantly instead of a black screen + spinner
            val placeholderKey = when (displayPhoto.source) {
                PhotoSource.Local -> displayPhoto.uri.toString()
                PhotoSource.Immich -> if (wantsOriginal) previewUrl else displayPhoto.thumbnailUrl
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageData)
                    .placeholderMemoryCacheKey(placeholderKey?.let { MemoryCache.Key(it) })
                    .crossfade(150)
                    .listener(
                        onSuccess = { _, _ -> onLoadingChanged(false) },
                        onError = { _, result ->
                            android.util.Log.e("ViewerPhotoPage", "Failed to load image: $imageData, error: ${result.throwable?.message}")
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
