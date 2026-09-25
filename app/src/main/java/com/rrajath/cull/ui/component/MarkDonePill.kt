package com.rrajath.cull.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrajath.cull.ui.icon.CullIcons
import com.rrajath.cull.ui.theme.ThemeColors

@Composable
fun MarkDonePill(
    onClick: () -> Unit,
    label: String = "Mark done",
) {
    val colors = ThemeColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.wizardComplete)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CullIcons.Check,
            contentDescription = null,
            tint = colors.wizardCompleteOn,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = colors.wizardCompleteOn,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
fun DoneLabel(label: String = "Done") {
    val colors = ThemeColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CullIcons.Check,
            contentDescription = null,
            tint = colors.wizardComplete,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = colors.wizardComplete,
                fontSize = 12.sp
            )
        )
    }
}
