package com.example.chatdz.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConvFilterTab {
    ALL, UNREAD, GROUPS, ARCHIVED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    conversations: List<ChatConversation>,
    isDarkMode: Boolean,
    isAppLocked: Boolean,
    addChatSecurityCode: String = "1962",
    onToggleTheme: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onToggleAppLock: () -> Unit,
    onOpenUserProfile: () -> Unit = {},
    onChangeAddChatCode: (String) -> Unit = {},
    onSelectConversation: (ChatConversation) -> Unit,
    onAddNewConversation: () -> Unit,
    onRenameConversation: (ChatConversation, String) -> Unit,
    onSetPinConversation: (ChatConversation, String) -> Unit,
    onRemovePinConversation: (ChatConversation) -> Unit,
    onTogglePinConversation: (ChatConversation) -> Unit,
    onToggleArchiveConversation: (ChatConversation) -> Unit,
    onToggleReadConversation: (ChatConversation) -> Unit,
    onDeleteConversation: (ChatConversation) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(ConvFilterTab.ALL) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var showVerifyAddCodeDialog by remember { mutableStateOf(false) }
    var showChangeAddCodeDialog by remember { mutableStateOf(false) }

    var selectedForOptions by remember { mutableStateOf<ChatConversation?>(null) }

    var conversationToRename by remember { mutableStateOf<ChatConversation?>(null) }
    var conversationToSetPin by remember { mutableStateOf<ChatConversation?>(null) }
    var conversationToDelete by remember { mutableStateOf<ChatConversation?>(null) }
    var lockedConversationToUnlock by remember { mutableStateOf<ChatConversation?>(null) }

    val unreadCount = conversations.count { !it.isArchived && it.unreadCount > 0 }
    val archivedCount = conversations.count { it.isArchived }

    val filteredConversations = conversations.filter { conv ->
        val matchesSearch = if (searchQuery.isBlank()) true else {
            conv.name.contains(searchQuery, ignoreCase = true) ||
            conv.lastMessage.contains(searchQuery, ignoreCase = true)
        }
        val matchesTab = when (selectedTab) {
            ConvFilterTab.ALL -> !conv.isArchived
            ConvFilterTab.UNREAD -> !conv.isArchived && conv.unreadCount > 0
            ConvFilterTab.GROUPS -> !conv.isArchived && conv.isGroup
            ConvFilterTab.ARCHIVED -> conv.isArchived
        }
        matchesSearch && matchesTab
    }.sortedWith(
        compareByDescending<ChatConversation> { it.isPinned }
            .thenByDescending { it.lastMessageTime }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث في المحادثات...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ChatDZ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "🇩🇿 الجزائر",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            painter = painterResource(id = if (isSearchActive) R.drawable.ic_close else R.drawable.ic_search),
                            contentDescription = "بحث",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // User Profile
                    IconButton(onClick = onOpenUserProfile) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person),
                            contentDescription = "الملف الشخصي",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Security Code Settings
                    IconButton(onClick = { showChangeAddCodeDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_key),
                            contentDescription = "رمز حماية الإضافة",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Dark/Light theme toggle
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_dark_mode),
                            contentDescription = "الوضع الليلي",
                            tint = if (isDarkMode) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Wallpaper picker
                    IconButton(onClick = onOpenWallpaperPicker) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_palette),
                            contentDescription = "تخصيص الخلفية",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // App lock toggle
                    IconButton(onClick = onToggleAppLock) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock),
                            contentDescription = "قفل التطبيق",
                            tint = if (isAppLocked) Color(0xFF81C784) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showVerifyAddCodeDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "محادثة جديدة")
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedTab == ConvFilterTab.ALL,
                    onClick = { selectedTab = ConvFilterTab.ALL },
                    label = { Text("الكل", fontSize = 12.sp) }
                )

                FilterChip(
                    selected = selectedTab == ConvFilterTab.UNREAD,
                    onClick = { selectedTab = ConvFilterTab.UNREAD },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("غير مقروءة", fontSize = 12.sp)
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($unreadCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )

                FilterChip(
                    selected = selectedTab == ConvFilterTab.GROUPS,
                    onClick = { selectedTab = ConvFilterTab.GROUPS },
                    label = { Text("المجموعات", fontSize = 12.sp) }
                )

                FilterChip(
                    selected = selectedTab == ConvFilterTab.ARCHIVED,
                    onClick = { selectedTab = ConvFilterTab.ARCHIVED },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("المؤرشفة 📦", fontSize = 12.sp)
                            if (archivedCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($archivedCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )
            }

            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == ConvFilterTab.ARCHIVED) "لا توجد محادثات مؤرشفة" else "لا توجد محادثات مطابقة",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(
                        items = filteredConversations,
                        key = { it.id }
                    ) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            timeFormatted = timeFormat.format(Date(conversation.lastMessageTime)),
                            onClick = {
                                if (conversation.pinLock != null) {
                                    lockedConversationToUnlock = conversation
                                } else {
                                    onSelectConversation(conversation)
                                }
                            },
                            onLongClick = {
                                selectedForOptions = conversation
                            }
                        )
                        Divider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // Conversation PIN Unlock Dialog
        if (lockedConversationToUnlock != null) {
            AppLockDialog(
                correctPin = lockedConversationToUnlock!!.pinLock ?: "",
                onSuccess = {
                    val conv = lockedConversationToUnlock!!
                    lockedConversationToUnlock = null
                    onSelectConversation(conv)
                },
                onDismiss = { lockedConversationToUnlock = null }
            )
        }

        // Options Dialog (Rename, PIN, Delete)
        if (selectedForOptions != null) {
            ConversationOptionsDialog(
                conversation = selectedForOptions!!,
                onDismiss = { selectedForOptions = null },
                onRename = { target -> conversationToRename = target },
                onSetPin = { target -> conversationToSetPin = target },
                onRemovePin = { target -> onRemovePinConversation(target) },
                onTogglePin = { target -> onTogglePinConversation(target) },
                onToggleArchive = { target -> onToggleArchiveConversation(target) },
                onToggleRead = { target -> onToggleReadConversation(target) },
                onDelete = { target -> conversationToDelete = target }
            )
        }

        // Rename Dialog
        if (conversationToRename != null) {
            val toRename = conversationToRename!!
            RenameConversationDialog(
                currentName = toRename.name,
                onDismiss = { conversationToRename = null },
                onConfirm = { newName ->
                    onRenameConversation(toRename, newName)
                }
            )
        }

        // Set PIN Dialog
        if (conversationToSetPin != null) {
            val toSetPin = conversationToSetPin!!
            SetConversationPinDialog(
                conversationName = toSetPin.name,
                onDismiss = { conversationToSetPin = null },
                onConfirm = { pin ->
                    onSetPinConversation(toSetPin, pin)
                }
            )
        }

        // Delete Confirmation Dialog
        if (conversationToDelete != null) {
            val toDelete = conversationToDelete!!
            AlertDialog(
                onDismissRequest = { conversationToDelete = null },
                title = { Text("حذف المحادثة") },
                text = { Text("هل أنت متأكد من رغبتك في حذف محادثة \"${toDelete.name}\" مع كافة رسائلها نهائياً؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteConversation(toDelete)
                            conversationToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف نهائي")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { conversationToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Verify Add Chat Code Dialog
        if (showVerifyAddCodeDialog) {
            VerifyAddChatCodeDialog(
                expectedCode = addChatSecurityCode,
                onDismiss = { showVerifyAddCodeDialog = false },
                onSuccess = {
                    showVerifyAddCodeDialog = false
                    onAddNewConversation()
                },
                onChangeCodeRequest = {
                    showVerifyAddCodeDialog = false
                    showChangeAddCodeDialog = true
                }
            )
        }

        // Change Add Chat Code Dialog
        if (showChangeAddCodeDialog) {
            ChangeAddChatCodeDialog(
                currentExpectedCode = addChatSecurityCode,
                onDismiss = { showChangeAddCodeDialog = false },
                onConfirmNewCode = { newCode ->
                    onChangeAddChatCode(newCode)
                }
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItem(
    conversation: ChatConversation,
    timeFormatted: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online badge
        Box {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(conversation.avatarColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.name.take(1),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name, Lock indicator and Last Message
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = conversation.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (conversation.pinLock != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock),
                            contentDescription = "محمية برمز دخول",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (conversation.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pin),
                            contentDescription = "مثبتة في القمة",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (conversation.isArchived) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_archive),
                            contentDescription = "مؤرشفة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (conversation.pinLock != null) "🔒 محادثة محمية برمز دخول" else conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // More options icon
        IconButton(onClick = onLongClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = "خيارات المحادثة",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
