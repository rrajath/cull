package com.rrajath.occullt.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.network.ImmichApi
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.component.SectionLabel
import com.rrajath.occullt.ui.component.SliderRow
import com.rrajath.occullt.ui.component.SourceMode
import com.rrajath.occullt.ui.component.ToggleRow
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.LocalExtendedColorScheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExtendedColorScheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceMode by remember { mutableStateOf(SourceMode.Local) }
    var libraryFolderUri by remember { mutableStateOf<String?>(null) }
    var immichUrl by remember { mutableStateOf("") }
    var immichApiKey by remember { mutableStateOf("") }
    var longPressThreshold by remember { mutableStateOf(220) }
    var dryRun by remember { mutableStateOf(false) }
    var mirrorDeletes by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(true) }
    var accentHue by remember { mutableStateOf(40) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settingsRepository) {
        launch {
            settingsRepository.sourceMode.collectLatest { mode ->
                sourceMode = mode
            }
        }
        launch {
            settingsRepository.libraryFolderUri.collectLatest { uri ->
                libraryFolderUri = uri
            }
        }
        launch {
            settingsRepository.immichUrl.collectLatest { url ->
                immichUrl = url ?: ""
            }
        }
        launch {
            settingsRepository.immichApiKey.collectLatest { key ->
                immichApiKey = key ?: ""
            }
        }
        launch {
            settingsRepository.longPressThreshold.collectLatest { threshold ->
                longPressThreshold = threshold
            }
        }
        launch {
            settingsRepository.dryRun.collectLatest { value ->
                dryRun = value
            }
        }
        launch {
            settingsRepository.mirrorDeletes.collectLatest { value ->
                mirrorDeletes = value
            }
        }
        launch {
            settingsRepository.darkTheme.collectLatest { value ->
                darkTheme = value
            }
        }
        launch {
            settingsRepository.accentHue.collectLatest { hue ->
                accentHue = hue
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val uriString = it.toString()
            scope.launch {
                settingsRepository.setLibraryFolderUri(uriString)
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Text(
                text = "Settings",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    color = colors.fg,
                    fontSize = 28.sp
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Section(text = "Source Mode") {
                SourceModeCard(
                    title = "Local",
                    description = "Photos from device storage",
                    selected = sourceMode == SourceMode.Local,
                    onClick = {
                        scope.launch {
                            settingsRepository.setSourceMode(SourceMode.Local)
                        }
                    }
                )
                SourceModeCard(
                    title = "Immich",
                    description = "Photos from Immich server",
                    selected = sourceMode == SourceMode.Immich,
                    onClick = {
                        scope.launch {
                            settingsRepository.setSourceMode(SourceMode.Immich)
                        }
                    }
                )
                SourceModeCard(
                    title = "Local + Immich",
                    description = "Merge both sources",
                    selected = sourceMode == SourceMode.Hybrid,
                    onClick = {
                        scope.launch {
                            settingsRepository.setSourceMode(SourceMode.Hybrid)
                        }
                    }
                )
            }

            if (sourceMode == SourceMode.Local || sourceMode == SourceMode.Hybrid) {
                Section(text = "Library Folder") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.bgElev)
                            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                            .clickable { folderPickerLauncher.launch(null) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Select Folder",
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                    color = colors.fg,
                                    fontSize = 14.sp
                                )
                            )
                            if (libraryFolderUri != null) {
                                Text(
                                    text = libraryFolderUri!!,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                        color = colors.fgDim,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "No folder selected",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                        color = colors.fgFaint,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (sourceMode == SourceMode.Immich || sourceMode == SourceMode.Hybrid) {
                Section(text = "Immich Connection") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.bgElev)
                            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsInputRow(
                            label = "Server URL",
                            value = immichUrl,
                            onValueChange = { newValue ->
                                immichUrl = newValue
                                scope.launch {
                                    settingsRepository.setImmichUrl(newValue)
                                }
                            },
                            placeholder = "https://immich.example.com"
                        )
                        SettingsInputRow(
                            label = "API Key",
                            value = immichApiKey,
                            onValueChange = { newValue ->
                                immichApiKey = newValue
                                scope.launch {
                                    settingsRepository.setImmichApiKey(newValue)
                                }
                            },
                            placeholder = "Enter API key"
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.accentSoft)
                                    .clickable {
                                        if (immichUrl.isBlank() || immichApiKey.isBlank()) {
                                            connectionStatus = "Please enter both Server URL and API Key"
                                            return@clickable
                                        }
                                        connectionStatus = "Testing..."
                                        scope.launch {
                                            try {
                                                val api = ImmichApi(immichUrl, immichApiKey)
                                                val result = api.getServerAbout()
                                                result.fold(
                                                    onSuccess = { info ->
                                                        connectionStatus = "Connected to Immich ${info.version}"
                                                    },
                                                    onFailure = { error ->
                                                        connectionStatus = "Connection failed: ${error.message}"
                                                    }
                                                )
                                            } catch (e: Exception) {
                                                connectionStatus = "Error: ${e.message}"
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Test Connection",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                        color = colors.accent,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            connectionStatus?.let { status ->
                AlertDialog(
                    onDismissRequest = { connectionStatus = null },
                    title = {
                        Text(
                            text = if (status == "Testing...") "Testing Connection" else "Connection Result",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                                color = colors.fg
                            )
                        )
                    },
                    text = {
                        Text(
                            text = status,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                color = colors.fgDim
                            )
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { connectionStatus = null }
                        ) {
                            Text("OK", color = colors.accent)
                        }
                    },
                    containerColor = colors.bgElev,
                    tonalElevation = 0.dp
                )
            }

            Section(text = "Culling") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgElev)
                        .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SliderRow(
                        label = "Long-press threshold",
                        value = longPressThreshold,
                        onValueChange = { newValue ->
                            longPressThreshold = newValue
                            scope.launch {
                                settingsRepository.setLongPressThreshold(newValue)
                            }
                        }
                    )
                    ToggleRow(
                        label = "Dry-run mode",
                        subtitle = "Show deletion preview without actual deletion",
                        checked = dryRun,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                settingsRepository.setDryRun(newValue)
                            }
                        }
                    )
                    if (sourceMode == SourceMode.Hybrid) {
                        ToggleRow(
                            label = "Mirror deletes to Immich",
                            subtitle = "Delete from Immich when deleting locally",
                            checked = mirrorDeletes,
                            onCheckedChange = { newValue ->
                                scope.launch {
                                    settingsRepository.setMirrorDeletes(newValue)
                                }
                            }
                        )
                    }
                }
            }

            Section(text = "Appearance") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgElev)
                        .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ToggleRow(
                        label = "Dark theme",
                        checked = darkTheme,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                settingsRepository.setDarkTheme(newValue)
                            }
                        }
                    )
                    Column {
                        Text(
                            text = "Accent color",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                color = colors.fg,
                                fontSize = 14.sp
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(0, 40, 120, 200, 280).forEach { hue ->
                                val isSelected = accentHue == hue
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Color.hsl(hue.toFloat(), 0.7f, 0.55f)
                                        )
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    2.dp,
                                                    colors.fg,
                                                    CircleShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            accentHue = hue
                                            scope.launch {
                                                settingsRepository.setAccentHue(hue)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(
    text: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalExtendedColorScheme.current
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionLabel(text = text)
        content()
    }
}

@Composable
private fun SourceModeCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalExtendedColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(colors.accentSoft)
                        .border(1.dp, colors.accent, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                        .background(colors.bgElev)
                        .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = if (selected) colors.accent else colors.fg,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = description,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (selected) {
                Icon(
                    imageVector = CullIcons.Check,
                    contentDescription = "Selected",
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val colors = LocalExtendedColorScheme.current
    Column {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                color = colors.fg,
                fontSize = 13.sp
            )
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgFaint,
                        fontSize = 13.sp
                    )
                )
            },
            singleLine = true,
            textStyle = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                color = colors.fg,
                fontSize = 13.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line,
                focusedContainerColor = colors.bg,
                unfocusedContainerColor = colors.bg,
                cursorColor = colors.accent,
                focusedTextColor = colors.fg,
                unfocusedTextColor = colors.fg
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
