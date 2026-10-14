package com.example.chatdz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatdz.R
import com.example.chatdz.models.ChatConversation
import com.example.chatdz.models.Message
import com.example.chatdz.models.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MediaTab {
    PHOTOS, DOCS, AUDIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    conversation: ChatConversation,
    messages: List<Message>,
    onBack: () -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onSearchRequest: () -> Unit,
    onSetDisappearingHours: (Int) -> Unit,
    onExportChat: () -> Unit,
    onClearChat: () -> Unit,
    onSetPin: () -> Unit,
    onOpenMediaMessage: (Message) -> Unit
) {
    var selectedMediaTab by remember { mutableStateOf(MediaTab.PHOTOS) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val photoMessages = remember(messages) {
        messages.filter { it.type == MessageType.IMAGE && it.mediaUri != null }
    }
    val docMessages = remember(messages) {
        messages.filter { it.type == MessageType.DOCUMENT }
    }
    val audioMessages = remember(messages) {
        messages.filter { it.type == MessageType.VOICE }
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (conversation.isGroup) "معلومات المجموعة" else "معلومات جهة الاتصال",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Profile Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(conversation.avatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.name.take(1),
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = conversation.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (conversation.isGroup) {
                            "مجموعة دردشة • ${conversation.members.size.coerceAtLeast(3)} أعضاء"
                        } else {
                            if (conversation.isOnline) "متصل الآن 🟢" else "آخر ظهور مؤخراً"
                        },
                        fontSize = 14.sp,
                        color = if (conversation.isOnline && !conversation.isGroup) Color(0xFF00A884) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Quick Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionItem(
                            iconRes = R.drawable.ic_call,
                            label = "صوتي",
                            onClick = onStartAudioCall
                        )
                        QuickActionItem(
                            iconRes = R.drawable.ic_videocall,
                            label = "فيديو",
                            onClick = onStartVideoCall
                        )
                        QuickActionItem(
                            iconRes = R.drawable.ic_search,
                            label = "بحث",
                            onClick = {
                                onBack()
                                onSearchRequest()
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Description / Bio Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (conversation.isGroup) "وصف المجموعة:" else "النبذة التعريفية:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (conversation.description.isNotBlank()) conversation.description
                            else if (conversation.isGroup) "مجموعة لمناقشة المستجدات والتواصل السريع 🇩🇿"
                            else "متاح للتواصل والمراسلة الفورية عبر ChatDZ",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Settings & Privacy Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Disappearing messages
                        InfoRowItem(
                            title = "الرسائل ذاتية الاختفاء ⏱️",
                            subtitle = when (conversation.disappearingHours) {
                                0 -> "متوقفة"
                                1 -> "بعد 1 ساعة"
                                24 -> "بعد 24 ساعة"
                                168 -> "بعد 7 أيام"
                                else -> "بعد ${conversation.disappearingHours} ساعة"
                            },
                            iconRes = R.drawable.ic_timer,
                            onClick = { showDisappearingDialog = true }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                        // PIN Lock
                        InfoRowItem(
                            title = "قفل المحادثة برمز PIN 🔒",
                            subtitle = if (conversation.pinLock != null) "مفعل (محمية برمز)" else "غير مفعل",
                            iconRes = R.drawable.ic_lock,
                            onClick = { onSetPin() }
                        )
                    }
                }
            }

            // Group Members (if group)
            if (conversation.isGroup) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "أعضاء المجموعة (${conversation.members.size}) 👥",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "إضافة عضو +",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            conversation.members.forEachIndexed { idx, member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00897B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = member.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (idx == 0 || member.contains("المشرف")) {
                                            Text(
                                                text = "مشرف المجموعة (Admin)",
                                                fontSize = 11.sp,
                                                color = Color(0xFF00A884)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Shared Media Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "الوسائط والمستندات المشتركة 📁",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tab selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedMediaTab == MediaTab.PHOTOS,
                                onClick = { selectedMediaTab = MediaTab.PHOTOS },
                                label = { Text("الصور (${photoMessages.size})") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedMediaTab == MediaTab.DOCS,
                                onClick = { selectedMediaTab = MediaTab.DOCS },
                                label = { Text("المستندات (${docMessages.size})") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedMediaTab == MediaTab.AUDIO,
                                onClick = { selectedMediaTab = MediaTab.AUDIO },
                                label = { Text("الصوت (${audioMessages.size})") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (selectedMediaTab) {
                            MediaTab.PHOTOS -> {
                                if (photoMessages.isEmpty()) {
                                    Text(
                                        text = "لا توجد صور مشتركة في هذه المحادثة حتى الآن",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        photoMessages.take(6).forEach { msg ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable { onOpenMediaMessage(msg) }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_image),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (msg.text.isNotBlank()) msg.text else "صورة فوتوغرافية",
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = dateFormat.format(Date(msg.timestamp)),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            MediaTab.DOCS -> {
                                if (docMessages.isEmpty()) {
                                    Text(
                                        text = "لا توجد ملفات أو مستندات مشتركة",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        docMessages.forEach { msg ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable { onOpenMediaMessage(msg) }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_file),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = msg.fileName ?: "مستند",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = "${msg.fileSize ?: "ملف"} • ${dateFormat.format(Date(msg.timestamp))}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            MediaTab.AUDIO -> {
                                if (audioMessages.isEmpty()) {
                                    Text(
                                        text = "لا توجد تسجيلات صوتية مشتركة",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        audioMessages.forEach { msg ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_mic),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "تسجيل صوتي (${msg.audioDurationSeconds} ثانية)",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = dateFormat.format(Date(msg.timestamp)),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Actions (Export, Clear) Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        InfoRowItem(
                            title = "تصدير المحادثة (Export Chat) 📤",
                            subtitle = "مشاركة سجل المحادثة كملف نصي كامل",
                            onClick = { onExportChat() }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                        InfoRowItem(
                            title = "مسح سجل الرسائل (Clear Chat) 🗑️",
                            subtitle = "حذف كافة الرسائل في هذه المحادثة فقط",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { showClearConfirmDialog = true }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Disappearing Dialog
    if (showDisappearingDialog) {
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("مؤقت الرسائل ذاتية الاختفاء ⏱️") },
            text = {
                Column {
                    Text("حدد المدة الزمنية التي تختفي بعدها الرسائل الجديدة تلقائياً:")
                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        0 to "إيقاف (لا تختفي أبداً)",
                        1 to "ساعة واحدة ⏳",
                        24 to "24 ساعة (يوم واحد) 📅",
                        168 to "7 أيام (أسبوع) 🗓️"
                    ).forEach { (hours, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetDisappearingHours(hours)
                                    showDisappearingDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = conversation.disappearingHours == hours,
                                onClick = {
                                    onSetDisappearingHours(hours)
                                    showDisappearingDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisappearingDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Clear Confirm Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("مسح المحادثة بالكامل؟") },
            text = { Text("هل أنت متأكد من رغبتك في حذف جميع الرسائل داخل '${conversation.name}'؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearChat()
                        showClearConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، مسح الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun QuickActionItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoRowItem(
    title: String,
    subtitle: String? = null,
    iconRes: Int? = null,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

