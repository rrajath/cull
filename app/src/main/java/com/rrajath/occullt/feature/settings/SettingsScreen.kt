package com.rrajath.occullt.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrajath.occullt.ui.component.CircleIcon
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.LocalExtendedColorScheme

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExtendedColorScheme.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
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

        Text(
            text = "Settings screen coming in Milestone 3",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                color = colors.fgFaint,
                fontSize = 14.sp
            ),
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}
