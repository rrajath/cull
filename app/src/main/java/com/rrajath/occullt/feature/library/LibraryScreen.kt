package com.rrajath.occullt.feature.library

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.rrajath.occullt.core.datastore.PhotoCache
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.component.SourceMode
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.ThemeColors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onPhotoClick: (Int, String?) -> Unit,
    onNavigateToStacks: () -> Unit,
    reloadTrigger: StateFlow<Int>,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(LocalContext.current.applicationContext, settingsRepository)
    )
    val state by viewModel.state.collectAsState()
    val colors = ThemeColors.current
    val context = LocalContext.current

    var permissionGranted by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    var showFilterSheet by remember { mutableStateOf(false) }
    var pendingStartDate by remember { mutableStateOf<Long?>(null) }
    var pendingEndDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(showFilterSheet) {
        if (showFilterSheet) {
            pendingStartDate = state.filterStartDate
            pendingEndDate = state.filterEndDate
        }
    }

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

    LaunchedEffect(Unit) {
        reloadTrigger.collectLatest {
            viewModel.loadPhotos(forceReload = true)
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
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
                            if (state.isFilterActive) {
                                "${state.photos.size} photos · Filtered"
                            } else {
                                "${state.photos.size} photos · $sourceLabel"
                            }
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIcon(
                    onClick = { showFilterSheet = true },
                    icon = {
                        Icon(
                            imageVector = CullIcons.Filter,
                            contentDescription = if (state.isFilterActive) "Filter active" else "Filter",
                            tint = if (state.isFilterActive) colors.accent else colors.fg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                CircleIcon(
                    onClick = {
                        PhotoCache.setPhotos(state.photos)
                        onNavigateToStacks()
                    },
                    icon = {
                        Icon(
                            imageVector = CullIcons.Stacks,
                            contentDescription = "Photo stacks",
                            tint = colors.fg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                state = rememberLazyGridState(),
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                itemsIndexed(
                    items = state.photos,
                    key = { _, photo -> photo.id }
                ) { index, photo ->
                    val isMarked = state.markedIds.contains(photo.id)
                    val isPinned = state.pinnedId == photo.id

                    Box(
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.line.copy(alpha = 0.3f))
                            .clickable {
                                PhotoCache.setPhotos(state.photos)
                                val uri = when (photo.source) {
                                    PhotoSource.Local -> "mediastore"
                                    PhotoSource.Immich -> "immich"
                                }
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

                if (state.hasMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.bgElev2)
                                .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                                .clickable(enabled = !state.isLoadingMore) {
                                    viewModel.loadMorePhotos()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator(
                                    color = colors.accent,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Load",
                                        style = com.rrajath.occullt.ui.theme.GiantButtonTitleStyle.copy(
                                            color = colors.accent,
                                            fontSize = 24.sp,
                                        )
                                    )
                                    Text(
                                        text = "More",
                                        style = com.rrajath.occullt.ui.theme.GiantButtonTitleStyle.copy(
                                            color = colors.accent,
                                            fontSize = 24.sp,
                                        )
                                    )
                                }
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

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = colors.bgElev,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Filter Photos",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        color = colors.fg,
                        fontSize = 22.sp
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgElev2)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Date Range",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                            color = colors.fgFaint,
                            fontSize = 11.sp,
                            letterSpacing = 1.6.sp
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.bg)
                            .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                            .clickable { showStartDatePicker = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "From",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                        color = colors.fgFaint,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = pendingStartDate?.let { displayDateFormat.format(Date(it)) } ?: "Select start date",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                        color = if (pendingStartDate != null) colors.fg else colors.fgFaint,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = CullIcons.ChevronRight,
                                contentDescription = null,
                                tint = colors.fgFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.bg)
                            .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                            .clickable { showEndDatePicker = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "To",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                        color = colors.fgFaint,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = pendingEndDate?.let { displayDateFormat.format(Date(it)) } ?: "Select end date",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                        color = if (pendingEndDate != null) colors.fg else colors.fgFaint,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = CullIcons.ChevronRight,
                                contentDescription = null,
                                tint = colors.fgFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.clearDateFilter()
                            showFilterSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.bgElev2,
                            contentColor = colors.fgDim,
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear", fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.setDateFilter(pendingStartDate, pendingEndDate)
                            showFilterSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.weight(1f),
                        enabled = pendingStartDate != null || pendingEndDate != null
                    ) {
                        Text("Apply", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = pendingStartDate
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingStartDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) {
                    Text("OK", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", color = colors.fgDim)
                }
            }
        ) {
            DatePicker(state = datePickerState, title = {}, headline = {})
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = pendingEndDate
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingEndDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) {
                    Text("OK", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel", color = colors.fgDim)
                }
            }
        ) {
            DatePicker(state = datePickerState, title = {}, headline = {})
        }
    }
}
