package com.example.chatdz.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.chatdz.R
import com.example.chatdz.models.MessageReplyInfo
import com.example.chatdz.models.MessageType
import com.example.chatdz.utils.FileUtils
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.util.Locale

val ALGERIAN_QUICK_PHRASES = listOf(
    "🇩🇿 واش راك خويا؟",
    "☕ صحا فطورك وعشاك",
    "✨ يعطيك الصحة خويا العزيز",
    "📍 وين راك لاحق؟",
    "🤲 في أمان الله وتهلى في روحك",
    "❤️ الحمد لله ربي يحفظك",
    "🏃‍♂️ راني جاي دوك دوك",
    "👍 مكان حتى مشكل يا خويا",
    "🔥 راك مسقمها اليوم!",
    "🌟 الله يبارك ما شاء الله",
    "🤝 مرحباً بك ديما",
    "😴 تصبح على خير"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (Uri, String) -> Unit,
    onSendDocument: (fileName: String, fileSize: String, localPath: String) -> Unit,
    onSendLocation: (lat: Double, lng: Double, name: String) -> Unit,
    replyTo: MessageReplyInfo?,
    onCancelReply: () -> Unit,
    onStartRecording: () -> Boolean,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onOpenCreatePoll: () -> Unit = {}
) {

    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showAlgerianPhrases by remember { mutableStateOf(false) }

    // Launcher for picking images (passes optional caption if user typed text)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSendImage(uri, value.trim())
            onValueChange("")
        }
    }

    // Launcher for picking documents
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val meta = FileUtils.copyUriToInternalStorage(context, uri)
            if (meta != null) {
                onSendDocument(meta.name, meta.sizeFormatted, meta.localPath)
            } else {
                Toast.makeText(context, "تعذر قراءة الملف المرفق", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher for audio recording
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = onStartRecording()
            if (started) {
                isRecording = true
                recordingDuration = 0
            }
        } else {
            Toast.makeText(context, "يلزم منح إذن الميكروفون لتسجيل الصوت", Toast.LENGTH_SHORT).show()
        }
    }

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    // Blinking animation for recording dot
    val infiniteTransition = rememberInfiniteTransition()
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Reply Preview Banner
            AnimatedVisibility(
                visible = replyTo != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (replyTo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الرد على ${replyTo.sender}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = when (replyTo.type) {
                                    MessageType.IMAGE -> "📷 صورة"
                                    MessageType.VOICE -> "🎙️ تسجيل صوتي"
                                    MessageType.DOCUMENT -> "📄 مستند"
                                    MessageType.LOCATION -> "📍 موقع"
                                    else -> replyTo.text
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "إلغاء الرد",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Expandable Attachment Options Menu
            AnimatedVisibility(
                visible = showAttachmentMenu && !isRecording,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo option
                    AttachmentItem(
                        iconRes = R.drawable.ic_image,
                        title = "صورة",
                        containerColor = Color(0xFFE91E63)
                    ) {
                        showAttachmentMenu = false
                        try {
                            imagePickerLauncher.launch("image/*")
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح معرض الصور", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Document option
                    AttachmentItem(
                        iconRes = R.drawable.ic_file,
                        title = "مستند",
                        containerColor = Color(0xFF5C6BC0)
                    ) {
                        showAttachmentMenu = false
                        try {
                            documentPickerLauncher.launch("*/*")
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح متصفح الملفات", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Location option
                    AttachmentItem(
                        iconRes = R.drawable.ic_location,
                        title = "الموقع",
                        containerColor = Color(0xFF43A047)
                    ) {
                        showAttachmentMenu = false
                        // Send current landmark location (الجزائر العاصمة 🇩🇿)
                        onSendLocation(36.7538, 3.0588, "الجزائر العاصمة، الجزائر 🇩🇿")
                        Toast.makeText(context, "تمت مشاركة موقعك الجغرافي", Toast.LENGTH_SHORT).show()
                    }

                    // Poll option
                    AttachmentItem(
                        iconRes = R.drawable.ic_poll,
                        title = "استطلاع",
                        containerColor = Color(0xFFFF9800)
                    ) {
                        showAttachmentMenu = false
                        onOpenCreatePoll()
                    }
                }
            }


            // Algerian Phrases Quick Bar 🇩🇿
            AnimatedVisibility(
                visible = showAlgerianPhrases && !isRecording,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ALGERIAN_QUICK_PHRASES) { phrase ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onSendMessage(phrase)
                                    showAlgerianPhrases = false
                                }
                        ) {
                            Text(
                                text = phrase,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Input Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording) {
                    // Recording Mode UI
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulsing red recording dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = dotAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Duration timer
                        val minutes = recordingDuration / 60
                        val seconds = recordingDuration % 60
                        Text(
                            text = String.format(Locale.getDefault(), "تسجيل: %02d:%02d", minutes, seconds),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Cancel recording
                        IconButton(
                            onClick = {
                                isRecording = false
                                onCancelRecording()
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "إلغاء التسجيل",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Finish & Send Recording
                    FilledIconButton(
                        onClick = {
                            isRecording = false
                            onStopRecording()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_stop),
                            contentDescription = "إرسال التسجيل",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    // Normal Input Mode UI

                    // Algerian Quick Phrases toggle button 🇩🇿
                    IconButton(
                        onClick = {
                            showAlgerianPhrases = !showAlgerianPhrases
                            if (showAlgerianPhrases) showAttachmentMenu = false
                        }
                    ) {
                        Text("🇩🇿", fontSize = 18.sp)
                    }

                    // Attachment expander button
                    IconButton(
                        onClick = {
                            showAttachmentMenu = !showAttachmentMenu
                            if (showAttachmentMenu) showAlgerianPhrases = false
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = if (showAttachmentMenu) R.drawable.ic_close else R.drawable.ic_image),
                            contentDescription = "المرفقات",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Text Field
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        placeholder = { Text("اكتب رسالة...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )

                    // Send or Mic button
                    if (value.trim().isNotEmpty()) {
                        FilledIconButton(
                            onClick = {
                                val text = value.trim()
                                if (text.isNotEmpty()) {
                                    onSendMessage(text)
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال")
                        }
                    } else {
                        // Mic Button for Voice Recording
                        FilledIconButton(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    val started = onStartRecording()
                                    if (started) {
                                        isRecording = true
                                        recordingDuration = 0
                                    }
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mic),
                                contentDescription = "تسجيل صوتي",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentItem(
    iconRes: Int,
    title: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
