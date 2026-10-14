package com.example.chatdz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chatdz.R
import com.example.chatdz.models.Message
import com.example.chatdz.models.MessageType

val AVAILABLE_EMOJIS = listOf("❤️", "👍", "😂", "😮", "😢", "🔥", "👏", "🎉")

@Composable
fun MessageActionDialog(
    message: Message,
    onDismiss: () -> Unit,
    onReactionSelect: (String) -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onPinToggle: (() -> Unit)? = null,
    onStarToggle: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    onSelectMultiple: (() -> Unit)? = null
) {

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = "خيارات الرسالة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Emoji reaction row
                Text(
                    text = "تفاعل سريع:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AVAILABLE_EMOJIS.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    onReactionSelect(emoji)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // Reply action
                ActionRowItem(
                    iconRes = R.drawable.ic_reply,
                    title = "رد على الرسالة",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onReply()
                        onDismiss()
                    }
                )

                // Pin / Unpin action
                if (onPinToggle != null) {
                    ActionRowItem(
                        iconRes = R.drawable.ic_pin,
                        title = if (message.isPinned) "إلغاء تثبيت الرسالة" else "تثبيت الرسالة في الأعلى",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            onPinToggle()
                            onDismiss()
                        }
                    )
                }

                // Edit action (only for own text messages)
                if (message.isOwn && message.type == MessageType.TEXT && onEdit != null) {
                    ActionRowItem(
                        iconRes = R.drawable.ic_edit,
                        title = "تعديل الرسالة",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onEdit()
                            onDismiss()
                        }
                    )
                }

                // Star / Unstar action
                if (onStarToggle != null) {
                    ActionRowItem(
                        iconRes = R.drawable.ic_star,
                        title = if (message.isStarred) "إلغاء تمييز الرسالة بنجمة" else "تمييز الرسالة بنجمة ⭐",
                        color = Color(0xFFFFB300),
                        onClick = {
                            onStarToggle()
                            onDismiss()
                        }
                    )
                }

                // Copy text action
                if (message.text.isNotEmpty()) {
                    ActionRowItem(
                        iconRes = R.drawable.ic_file,
                        title = "نسخ نص الرسالة",
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            onCopy?.invoke()
                            onDismiss()
                        }
                    )
                }

                // Forward action
                if (onForward != null) {
                    ActionRowItem(
                        iconRes = R.drawable.ic_reply,
                        title = "إعادة توجيه الرسالة ↗️",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onForward()
                            onDismiss()
                        }
                    )
                }

                // Multi-select action
                if (onSelectMultiple != null) {
                    ActionRowItem(
                        iconRes = R.drawable.ic_check_double,
                        title = "تحديد رسائل متعددة ☑️",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            onSelectMultiple()
                            onDismiss()
                        }
                    )
                }

                // Delete action (only for own messages)
                if (message.isOwn) {
                    ActionRowItem(
                        iconRes = R.drawable.ic_delete,
                        title = "حذف الرسالة نهائياً",
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            onDelete()
                            onDismiss()
                        }
                    )
                }


                Spacer(modifier = Modifier.height(8.dp))

                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء")
                }
            }
        }
    }
}

@Composable
private fun ActionRowItem(
    iconRes: Int,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
