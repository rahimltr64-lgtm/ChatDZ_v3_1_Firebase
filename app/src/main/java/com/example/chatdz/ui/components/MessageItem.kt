package com.example.chatdz.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatdz.R
import com.example.chatdz.models.Message
import com.example.chatdz.models.MessageStatus
import com.example.chatdz.models.MessageType
import com.example.chatdz.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    dateFormat: SimpleDateFormat,
    currentlyPlayingUri: String?,
    playbackSpeed: Float = 1.0f,
    searchQuery: String = "",
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (Message) -> Unit = {},
    onVotePoll: (Message, Int) -> Unit = { _, _ -> },
    onTogglePlayAudio: (String) -> Unit,
    onCycleSpeed: () -> Unit = {},
    onSeekAudio: (Float) -> Unit = {},
    onImageClick: (ImageBitmap) -> Unit,
    onLongClick: () -> Unit,
    onReactionClick: (String) -> Unit
) {

    val context = LocalContext.current
    val alignment = if (message.isOwn) Alignment.End else Alignment.Start

    val isSearchMatched = searchQuery.isNotBlank() && (
        message.text.contains(searchQuery, ignoreCase = true) ||
        (message.fileName != null && message.fileName.contains(searchQuery, ignoreCase = true)) ||
        (message.locationName != null && message.locationName.contains(searchQuery, ignoreCase = true))
    )

    val bubbleColor = if (message.isDeleted) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else if (message.isOwn) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (message.isDeleted) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } else if (message.isOwn) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Cached bitmap for image messages
    var imageBitmap by remember(message.mediaUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(message.mediaUri) {
        if (message.type == MessageType.IMAGE && message.mediaUri != null) {
            imageBitmap = ImageUtils.loadBitmap(message.mediaUri, context)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (message.isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode && !message.isOwn) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect(message) },
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Column(
            horizontalAlignment = alignment
        ) {
            // Sender name for incoming messages
            if (!message.isOwn && !message.isDeleted) {
                Text(
                    text = message.sender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            // Message Bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isOwn) 16.dp else 4.dp,
                            bottomEnd = if (message.isOwn) 4.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, Color(0xFF00A884), RoundedCornerShape(16.dp))
                        } else if (isSearchMatched) {
                            Modifier.border(2.dp, Color(0xFFFFB300), RoundedCornerShape(16.dp))
                        } else Modifier
                    )
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelect(message)
                            } else {
                                when (message.type) {
                                    MessageType.IMAGE -> {
                                        if (imageBitmap != null) {
                                            onImageClick(imageBitmap!!)
                                        }
                                    }
                                    MessageType.LOCATION -> {
                                        if (message.latitude != null && message.longitude != null) {
                                            try {
                                                val geoUri = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}(${message.locationName ?: "موقع"})")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                                context.startActivity(mapIntent)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        },
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(min = 90.dp, max = 300.dp)
            ) {
                Column {
                if (message.isDeleted) {
                    Text(
                        text = "🚫 تم حذف هذه الرسالة",
                        color = textColor,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    // Replying quote box
                    if (message.replyTo != null) {
                        val replyQuoteColor = if (message.isOwn) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        }
                        val accentBarColor = if (message.isOwn) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(replyQuoteColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(accentBarColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = message.replyTo.sender,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (message.isOwn) textColor else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = when (message.replyTo.type) {
                                        MessageType.IMAGE -> "📷 صورة"
                                        MessageType.VOICE -> "🎙️ تسجيل صوتي"
                                        MessageType.DOCUMENT -> "📄 مستند"
                                        MessageType.LOCATION -> "📍 موقع"
                                        else -> message.replyTo.text
                                    },
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Content according to message type
                    when (message.type) {
                        MessageType.IMAGE -> {
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap!!,
                                    contentDescription = "صورة مرسلة",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📷 جاري تحميل الصورة...", color = textColor, fontSize = 12.sp)
                                }
                            }

                            if (message.text.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    color = textColor,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        MessageType.VOICE -> {
                            VoiceMessagePlayer(
                                audioUri = message.mediaUri ?: "",
                                durationSeconds = message.audioDurationSeconds,
                                isPlaying = currentlyPlayingUri == message.mediaUri,
                                isOwn = message.isOwn,
                                playbackSpeed = playbackSpeed,
                                onTogglePlay = onTogglePlayAudio,
                                onCycleSpeed = onCycleSpeed,
                                onSeekPercent = onSeekAudio
                            )
                        }

                        MessageType.DOCUMENT -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (message.isOwn) Color.White.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (message.isOwn) Color.White.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.primaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_file),
                                        contentDescription = "ملف",
                                        tint = if (message.isOwn) textColor else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName ?: "مستند",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = message.fileSize ?: "ملف مرفق",
                                        fontSize = 11.sp,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        MessageType.LOCATION -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (message.isOwn) Color.White.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_location),
                                        contentDescription = "موقع",
                                        tint = if (message.isOwn) Color(0xFFFFD54F) else Color(0xFFE53935),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = message.locationName ?: "موقع جغرافي",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "إحداثيات: ${message.latitude ?: 0.0}, ${message.longitude ?: 0.0}",
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "📍 انقر للفتح في خرائط Google",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (message.isOwn) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        MessageType.POLL -> {
                            val poll = message.pollData
                            if (poll != null) {
                                val totalVotes = poll.options.sumOf { it.votes }.coerceAtLeast(0)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_poll),
                                            contentDescription = null,
                                            tint = if (message.isOwn) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = poll.question,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    poll.options.forEach { opt ->
                                        val percent = if (totalVotes > 0) (opt.votes.toFloat() / totalVotes.toFloat()) else 0f
                                        val percentInt = (percent * 100).toInt()
                                        val hasVoted = opt.voters.contains("me")

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (message.isOwn) Color.White.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .clickable { onVotePoll(message, opt.id) }
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (hasVoted) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_check),
                                                            contentDescription = "تم التصويت",
                                                            tint = if (message.isOwn) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                    Text(
                                                        text = opt.text,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (hasVoted) FontWeight.Bold else FontWeight.Normal,
                                                        color = textColor
                                                    )
                                                }
                                                Text(
                                                    text = "$percentInt% (${opt.votes})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = textColor.copy(alpha = 0.8f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            LinearProgressIndicator(
                                                progress = percent,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = if (message.isOwn) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
                                                trackColor = textColor.copy(alpha = 0.2f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "إجمالي الأصوات: $totalVotes • انقر للتصويت",
                                        fontSize = 11.sp,
                                        color = textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }

                        MessageType.TEXT -> {
                            Text(
                                text = message.text,
                                color = textColor,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Footer row: Pinned icon + Star + Edited tag + Expiring Timer + Timestamp + Status ticks
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.expiresAt != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_timer),
                            contentDescription = "ذاتية الاختفاء",
                            tint = if (message.isOwn) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    if (message.isPinned) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pin),
                            contentDescription = "مثبتة",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    if (message.isStarred) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = "مميزة بنجمة",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    if (message.isEdited) {
                        Text(
                            text = "(معدلة)",
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(message.timestamp)),
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )


                    // Message status ticks (only for own messages)
                    if (message.isOwn && !message.isDeleted) {
                        when (message.status) {
                            MessageStatus.SENT -> {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "تم الإرسال",
                                    tint = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_double),
                                    contentDescription = "تم الاستلام",
                                    tint = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.READ -> {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_double),
                                    contentDescription = "تمت القراءة",
                                    tint = Color(0xFF29B6F6),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reactions row below the bubble
        if (message.reactions.isNotEmpty() && !message.isDeleted) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.forEach { (emoji, count) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { onReactionClick(emoji) },
                                onLongClick = onLongClick
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 12.sp)
                            if (count > 1) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = count.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isSelectionMode && message.isOwn) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect(message) },
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
}

