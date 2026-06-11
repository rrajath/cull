package com.rrajath.occullt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.rrajath.occullt.core.grouping.PhotoStack
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.ui.icon.CullIcons
import com.rrajath.occullt.ui.theme.ThemeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StackCard(
    stack: PhotoStack,
    onClick: () -> Unit,
    isDone: Boolean = false,
) {
    val colors = ThemeColors.current
    val dateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    val startDate = Date(stack.startTime)
    val endDate = Date(stack.endTime)

    val timeRange = if (SimpleDateFormat("yyyyMMdd", Locale.US).format(startDate)
        == SimpleDateFormat("yyyyMMdd", Locale.US).format(endDate)
    ) {
        "${dateFormat.format(startDate)} - ${timeFormat.format(endDate)}"
    } else {
        "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgElev)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .alpha(if (isDone) 0.5f else 1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val firstPhoto = stack.photos.first()
            val imageUrl = when (firstPhoto.source) {
                PhotoSource.Local -> firstPhoto.uri
                PhotoSource.Immich -> firstPhoto.thumbnailUrl ?: firstPhoto.previewUrl ?: firstPhoto.uri
            }
            val requestBuilder = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
            if (firstPhoto.source == PhotoSource.Local) {
                requestBuilder.size(200, 200)
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.line.copy(alpha = 0.3f))
            ) {
                AsyncImage(
                    model = requestBuilder.build(),
                    contentDescription = firstPhoto.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${stack.photos.size}",
                        color = Color.White,
                        fontSize = 10.sp,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.fg,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "${stack.photos.size} photos",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.fgDim,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = CullIcons.ChevronRight,
                contentDescription = "View stack",
                tint = colors.fgDim,
                modifier = Modifier.size(20.dp)
            )
        }

        if (isDone) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(colors.wizardComplete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CullIcons.Check,
                    contentDescription = "Stack done",
                    tint = colors.wizardCompleteOn,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
