package com.rrajath.cull.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrajath.cull.ui.theme.GiantButtonSubtitleStyle
import com.rrajath.cull.ui.theme.GiantButtonTitleStyle
import com.rrajath.cull.ui.theme.ThemeColors

@Composable
fun GiantButton(
    title: String,
    subtitle: String? = null,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentBackground: Boolean = true,
    backgroundColor: Color? = null,
    minHeight: Dp = 160.dp,
) {
    val colors = ThemeColors.current
    val filled = enabled && (backgroundColor != null || accentBackground)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(minHeight)
            .clip(RoundedCornerShape(32.dp))
            .then(
                if (filled) {
                    Modifier.background(backgroundColor ?: colors.accent)
                } else if (enabled) {
                    Modifier
                        .background(colors.bgElev)
                        .border(1.dp, colors.line, RoundedCornerShape(32.dp))
                } else {
                    Modifier
                        .background(colors.bgElev.copy(alpha = 0.55f))
                        .border(1.dp, colors.line.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = GiantButtonTitleStyle.copy(
                        color = if (filled) Color.White else colors.fg,
                        fontStyle = FontStyle.Italic
                    ),
                    fontSize = 44.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = GiantButtonSubtitleStyle.copy(
                            color = if (filled) Color.White.copy(alpha = 0.7f) else colors.fgDim
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) Color.White.copy(alpha = 0.2f) else colors.bgElev2
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }
        }
    }
}

@Composable
fun CircleIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val colors = ThemeColors.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.bgElev)
            .border(1.dp, colors.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun ContinuePill(
    thumbnailContent: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.bgElev)
            .border(1.dp, colors.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgElev2)
        ) {
            thumbnailContent()
        }
        Text(
            text = label,
            style = LabelStyle.copy(color = colors.fgDim),
            fontSize = 13.sp
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = com.rrajath.cull.ui.icon.CullIcons.ChevronRight,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SourceSwitcher(
    selected: SourceMode,
    onSelectionChanged: (SourceMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.bgElev)
            .border(1.dp, colors.line, RoundedCornerShape(999.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SourceMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(colors.accentSoft)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectionChanged(mode) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mode) {
                        SourceMode.Local -> "Local"
                        SourceMode.Immich -> "Immich"
                        SourceMode.Hybrid -> "Hybrid"
                    },
                    style = LabelStyle.copy(
                        color = if (isSelected) colors.accent else colors.fgDim,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

enum class SourceMode {
    Local, Immich, Hybrid
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeColors.current
    Text(
        text = text,
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
            color = colors.fgFaint,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = modifier
    )
}

@Composable
fun Div(
    modifier: Modifier = Modifier,
) {
    val colors = ThemeColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(colors.line)
    )
}

@Composable
fun Row(
    icon: @Composable (() -> Unit)? = null,
    label: String,
    subtitle: String? = null,
    value: String? = null,
    action: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeColors.current
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = LabelStyle.copy(color = colors.fg, fontSize = 14.sp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = LabelStyle.copy(color = colors.fgDim, fontSize = 12.sp)
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = LabelStyle.copy(color = colors.fgDim, fontSize = 12.sp)
            )
        }
        if (action != null) {
            action()
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeColors.current
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = LabelStyle.copy(color = colors.fg, fontSize = 14.sp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = LabelStyle.copy(color = colors.fgDim, fontSize = 12.sp)
                )
            }
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = colors.accent,
                checkedTrackColor = colors.accentSoft
            )
        )
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Int,
    valueSuffix: String = "ms",
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 80f..800f,
) {
    val colors = ThemeColors.current
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = LabelStyle.copy(color = colors.fg, fontSize = 14.sp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentSoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$value $valueSuffix",
                    style = LabelStyle.copy(color = colors.accent, fontSize = 12.sp)
                )
            }
        }
        androidx.compose.material3.Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.bgElev2
            )
        )
    }
}

val LabelStyle = androidx.compose.ui.text.TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
)
