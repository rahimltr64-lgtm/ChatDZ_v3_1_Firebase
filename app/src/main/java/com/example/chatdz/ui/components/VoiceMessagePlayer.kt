package com.example.chatdz.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatdz.R
import java.util.Locale

@Composable
fun VoiceMessagePlayer(
    audioUri: String,
    durationSeconds: Int,
    isPlaying: Boolean,
    isOwn: Boolean,
    playbackSpeed: Float = 1.0f,
    onTogglePlay: (String) -> Unit,
    onCycleSpeed: () -> Unit = {},
    onSeekPercent: (Float) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val contentColor = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val iconBgColor = if (isOwn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    val iconTint = if (isOwn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor)
                .clickable { onTogglePlay(audioUri) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Waveform visualization & duration
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Interactive waveform bars
            val heights = listOf(8, 14, 20, 10, 16, 22, 12, 18, 14, 20, 8, 16, 12, 18, 10, 20)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                heights.forEachIndexed { index, defaultHeight ->
                    val height = if (isPlaying) {
                        val factor = if ((index % 2) == 0) animatedProgress else (1.2f - animatedProgress)
                        (defaultHeight * factor).coerceIn(4f, 22f).dp
                    } else {
                        defaultHeight.dp
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(height)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(contentColor.copy(alpha = if (isPlaying) 0.95f else 0.5f))
                            .clickable {
                                val targetPercent = (index.toFloat() / heights.size).coerceIn(0f, 1f)
                                onSeekPercent(targetPercent)
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Duration text
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Speed Toggle Button (1x / 1.5x / 2x)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (playbackSpeed > 1.0f) {
                if (isOwn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
            } else {
                contentColor.copy(alpha = 0.15f)
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onCycleSpeed)
        ) {
            val speedLabel = when (playbackSpeed) {
                1.5f -> "1.5x"
                2.0f -> "2x"
                else -> "1x"
            }
            Text(
                text = speedLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (playbackSpeed > 1.0f) {
                    if (isOwn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                } else {
                    contentColor
                },
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}
