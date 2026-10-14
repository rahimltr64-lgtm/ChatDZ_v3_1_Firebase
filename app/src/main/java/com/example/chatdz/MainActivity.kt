package com.example.chatdz

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.chatdz.models.ChatConversation
import com.example.chatdz.models.Message
import com.example.chatdz.models.MessageReplyInfo
import com.example.chatdz.models.MessageStatus
import com.example.chatdz.models.MessageType
import com.example.chatdz.models.PollData
import com.example.chatdz.models.PollOption
import com.example.chatdz.models.UserProfile
import com.example.chatdz.ui.components.AppLockDialog
import com.example.chatdz.ui.components.ChatInfoScreen
import com.example.chatdz.ui.components.ConversationsScreen
import com.example.chatdz.ui.components.CreatePollDialog
import com.example.chatdz.ui.components.EditMessageDialog
import com.example.chatdz.ui.components.FullScreenImageViewer
import com.example.chatdz.ui.components.MessageActionDialog
import com.example.chatdz.ui.components.MessageInputBar
import com.example.chatdz.ui.components.MessageItem
import com.example.chatdz.ui.components.NewConversationDialog
import com.example.chatdz.ui.components.PinnedMessageBanner
import com.example.chatdz.ui.components.RenameConversationDialog
import com.example.chatdz.ui.components.SetConversationPinDialog
import com.example.chatdz.ui.components.TypingIndicator
import com.example.chatdz.ui.components.UserProfileDialog
import com.example.chatdz.ui.components.VoipCallDialog
import com.example.chatdz.ui.components.WALLPAPER_THEMES
import com.example.chatdz.ui.components.WallpaperPickerDialog
import com.example.chatdz.utils.AudioRecorderManager
import com.example.chatdz.utils.ChatStorage
import com.example.chatdz.utils.ImageUtils
import com.example.chatdz.firebase.FirebaseChatRepository
import com.example.chatdz.ui.components.AuthScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val messages = mutableStateListOf<Message>()
    private val conversations = mutableStateListOf<ChatConversation>()
    private var activeConversation by mutableStateOf<ChatConversation?>(null)
    private var isDarkMode by mutableStateOf(false)
    private var wallpaperIndex by mutableStateOf(0)
    private var isAppLocked by mutableStateOf(false)
    private var isUnlocked by mutableStateOf(true)
    private var addChatSecurityCode by mutableStateOf("1962")
    private var userProfile by mutableStateOf(UserProfile())

    // Firebase
    private val firebaseRepo = FirebaseChatRepository.getInstance()
    private var isFirebaseReady by mutableStateOf(false)
    private var isSigningIn by mutableStateOf(false)
    private var authError by mutableStateOf<String?>(null)
    private var isOnlineMode by mutableStateOf(false)

    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private lateinit var audioRecorderManager: AudioRecorderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioRecorderManager = AudioRecorderManager(this)

        isDarkMode = ChatStorage.isDarkMode(this)
        wallpaperIndex = ChatStorage.getWallpaperIndex(this)
        isAppLocked = ChatStorage.isAppLocked(this)
        isUnlocked = !isAppLocked
        addChatSecurityCode = ChatStorage.getAddChatCode(this)
        userProfile = ChatStorage.getUserProfile(this)

        // Check if already signed in with Firebase
        if (firebaseRepo.currentUser != null) {
            isFirebaseReady = true
            isOnlineMode = true
            lifecycleScope.launch {
                firebaseRepo.setOnlineStatus(true)
                val profile = firebaseRepo.getUserProfile()
                if (profile != null) userProfile = profile
            }
            // Will be started after composition, but we can call early
            // startFirebaseConversationObserver() is safe to call later
        }

        conversations.clear()
        conversations.addAll(ChatStorage.loadConversations(this))

        setContent {
            // ========== Auth Gate ==========
            if (!isFirebaseReady) {
                AuthScreen(
                    isLoading = isSigningIn,
                    errorMessage = authError,
                    onSignIn = { displayName ->
                        isSigningIn = true
                        authError = null
                        lifecycleScope.launch {
                            val result = firebaseRepo.signInAnonymously(displayName)
                            isSigningIn = false
                            if (result.isSuccess) {
                                isFirebaseReady = true
                                isOnlineMode = true
                                userProfile = UserProfile(name = displayName)
                                firebaseRepo.setOnlineStatus(true)
                                startFirebaseConversationObserver()
                            } else {
                                authError = result.exceptionOrNull()?.message ?: "فشل تسجيل الدخول. تحقق من اتصال الإنترنت وملف google-services.json"
                            }
                        }
                    }
                )
                return@setContent
            }

            // Start real-time conversation listener once authenticated
            LaunchedEffect(isOnlineMode) {
                if (isOnlineMode) {
                    startFirebaseConversationObserver()
                }
            }

            val colorScheme = if (isDarkMode) darkColorScheme(
                primary = Color(0xFF00A884),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF005C4B),
                onPrimaryContainer = Color.White,
                surface = Color(0xFF121B22),
                onSurface = Color(0xFFE9EDEF),
                surfaceVariant = Color(0xFF1F2C34),
                onSurfaceVariant = Color(0xFF8696A0),
                background = Color(0xFF0B141A)
            ) else lightColorScheme(
                primary = Color(0xFF008069),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD9FDD3),
                onPrimaryContainer = Color(0xFF005C4B),
                surface = Color.White,
                onSurface = Color(0xFF111B21),
                surfaceVariant = Color(0xFFF0F2F5),
                onSurfaceVariant = Color(0xFF667781),
                background = Color(0xFFEFEAE2)
            )

            MaterialTheme(colorScheme = colorScheme) {
                var showWallpaperPicker by remember { mutableStateOf(false) }
                var showNewConvDialog by remember { mutableStateOf(false) }
                var showUserProfileDialog by remember { mutableStateOf(false) }
                var showChatInfoScreen by remember { mutableStateOf(false) }
                var showVoipCallDialog by remember { mutableStateOf(false) }
                var isVoipVideoCall by remember { mutableStateOf(false) }
                var showCreatePollDialog by remember { mutableStateOf(false) }
                var forwardMessageToPickConv by remember { mutableStateOf<List<Message>?>(null) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (activeConversation == null) {
                        ConversationsScreen(
                            conversations = conversations,
                            isDarkMode = isDarkMode,
                            isAppLocked = isAppLocked,
                            addChatSecurityCode = addChatSecurityCode,
                            onToggleTheme = {
                                isDarkMode = !isDarkMode
                                ChatStorage.setDarkMode(this@MainActivity, isDarkMode)
                            },
                            onOpenWallpaperPicker = { showWallpaperPicker = true },
                            onToggleAppLock = {
                                isAppLocked = !isAppLocked
                                ChatStorage.setAppLocked(this@MainActivity, isAppLocked)
                                Toast.makeText(
                                    this@MainActivity,
                                    if (isAppLocked) "تم تفعيل قفل التطبيق (رمز PIN: 1234)" else "تم إلغاء قفل التطبيق",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onOpenUserProfile = { showUserProfileDialog = true },
                            onChangeAddChatCode = { newCode ->
                                addChatSecurityCode = newCode
                                ChatStorage.setAddChatCode(this@MainActivity, newCode)
                                Toast.makeText(this@MainActivity, "تم تحديث رمز حماية إضافة المحادثات", Toast.LENGTH_SHORT).show()
                            },
                            onSelectConversation = { conv ->
                                openConversation(conv)
                            },
                            onAddNewConversation = {
                                showNewConvDialog = true
                            },
                            onRenameConversation = { conv, newName ->
                                renameConversation(conv, newName)
                            },
                            onSetPinConversation = { conv, pin ->
                                setConversationPin(conv, pin)
                            },
                            onRemovePinConversation = { conv ->
                                removeConversationPin(conv)
                            },
                            onTogglePinConversation = { conv ->
                                togglePinConversation(conv)
                            },
                            onToggleArchiveConversation = { conv ->
                                toggleArchiveConversation(conv)
                            },
                            onToggleReadConversation = { conv ->
                                toggleReadConversation(conv)
                            },
                            onDeleteConversation = { conv ->
                                deleteConversation(conv)
                            }
                        )
                    } else {
                        ChatDZScreen(
                            conversation = activeConversation!!,
                            messages = messages,
                            dateFormat = dateFormat,
                            wallpaperIndex = wallpaperIndex,
                            audioRecorderManager = audioRecorderManager,
                            onBack = {
                                saveCurrentConversation()
                                activeConversation = null
                            },
                            onSendMessage = { text, replyTo -> sendTextMessage(text, replyTo) },
                            onSendImage = { uri, replyTo, caption -> sendImageMessage(uri, replyTo, caption) },
                            onSendVoice = { path, duration, replyTo -> sendVoiceMessage(path, duration, replyTo) },
                            onSendDocument = { name, size, path, replyTo -> sendDocumentMessage(name, size, path, replyTo) },
                            onSendLocation = { lat, lng, name, replyTo -> sendLocationMessage(lat, lng, name, replyTo) },
                            onDeleteMessage = { id -> deleteMessage(id) },
                            onToggleReaction = { id, emoji -> toggleReaction(id, emoji) },
                            onEditMessage = { id, newText -> editMessage(id, newText) },
                            onTogglePin = { id -> togglePinMessage(id) },
                            onToggleStar = { id -> toggleStarMessage(id) },
                            onExportChat = { activeConversation?.let { exportChat(it) } },
                            onSetDisappearing = { hours -> activeConversation?.let { setDisappearingMessages(it, hours) } },
                            onClearChat = { clearCurrentChat() },
                            onRenameChat = { newName ->
                                activeConversation?.let { renameConversation(it, newName) }
                            },
                            onSetChatPin = { pin ->
                                activeConversation?.let { setConversationPin(it, pin) }
                            },
                            onRemoveChatPin = {
                                activeConversation?.let { removeConversationPin(it) }
                            },
                            onDeleteChat = {
                                activeConversation?.let {
                                    val toDelete = it
                                    activeConversation = null
                                    deleteConversation(toDelete)
                                }
                            },
                            onCopyText = { text -> copyToClipboard(text) },
                            onOpenChatInfo = { showChatInfoScreen = true },
                            onStartAudioCall = {
                                isVoipVideoCall = false
                                showVoipCallDialog = true
                            },
                            onStartVideoCall = {
                                isVoipVideoCall = true
                                showVoipCallDialog = true
                            },
                            onOpenCreatePoll = { showCreatePollDialog = true },
                            onVotePoll = { msg, optId -> votePoll(msg, optId) },
                            onBatchDelete = { ids -> batchDeleteMessages(ids) },
                            onBatchStar = { ids -> batchStarMessages(ids) },
                            onBatchForward = { msgs -> forwardMessageToPickConv = msgs }
                        )
                    }

                    // Chat Info Screen
                    if (showChatInfoScreen && activeConversation != null) {
                        ChatInfoScreen(
                            conversation = activeConversation!!,
                            messages = messages,
                            onBack = { showChatInfoScreen = false },
                            onStartAudioCall = {
                                showChatInfoScreen = false
                                isVoipVideoCall = false
                                showVoipCallDialog = true
                            },
                            onStartVideoCall = {
                                showChatInfoScreen = false
                                isVoipVideoCall = true
                                showVoipCallDialog = true
                            },
                            onSearchRequest = {
                                showChatInfoScreen = false
                            },
                            onSetDisappearingHours = { hours ->
                                activeConversation?.let { setDisappearingMessages(it, hours) }
                            },
                            onExportChat = { activeConversation?.let { exportChat(it) } },
                            onClearChat = { clearCurrentChat() },
                            onSetPin = {
                                showChatInfoScreen = false
                            },
                            onOpenMediaMessage = { msg ->
                                showChatInfoScreen = false
                            }
                        )
                    }

                    // VoIP Call Dialog (Audio & Video)
                    if (showVoipCallDialog && activeConversation != null) {
                        VoipCallDialog(
                            conversation = activeConversation!!,
                            isVideoCall = isVoipVideoCall,
                            onEndCall = { showVoipCallDialog = false }
                        )
                    }

                    // Create Poll Dialog
                    if (showCreatePollDialog) {
                        CreatePollDialog(
                            onDismiss = { showCreatePollDialog = false },
                            onCreatePoll = { pollData ->
                                showCreatePollDialog = false
                                sendPollMessage(pollData, null)
                            }
                        )
                    }

                    // User Profile Dialog
                    if (showUserProfileDialog) {
                        UserProfileDialog(
                            initialProfile = userProfile,
                            onDismiss = { showUserProfileDialog = false },
                            onSaveProfile = { updated ->
                                userProfile = updated
                                ChatStorage.saveUserProfile(this@MainActivity, updated)
                                showUserProfileDialog = false
                                Toast.makeText(this@MainActivity, "تم حفظ الملف الشخصي بنجاح 🇩🇿", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Forward Messages Dialog (Pick Conversation)
                    if (forwardMessageToPickConv != null) {
                        val msgsToFwd = forwardMessageToPickConv!!
                        AlertDialog(
                            onDismissRequest = { forwardMessageToPickConv = null },
                            title = { Text("تحويل إلى محادثة ↗️ (${msgsToFwd.size} رسالة)") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("اختر المحادثة التي تريد تحويل الرسائل إليها:")
                                    conversations.filter { it.id != activeConversation?.id }.forEach { targetConv ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    forwardMessagesTo(msgsToFwd, targetConv)
                                                    forwardMessageToPickConv = null
                                                },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(targetConv.avatarColor)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = targetConv.name.take(1),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = targetConv.name,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { forwardMessageToPickConv = null }) {
                                    Text("إلغاء")
                                }
                            }
                        )
                    }

                    // New Conversation Dialog
                    if (showNewConvDialog) {
                        NewConversationDialog(
                            onDismiss = { showNewConvDialog = false },
                            onConfirm = { name, pin ->
                                createNewNamedConversation(name, pin)
                            }
                        )
                    }

                    // App Lock Dialog (Main App Lock)
                    if (!isUnlocked) {
                        AppLockDialog(
                            correctPin = ChatStorage.getAppPin(this@MainActivity),
                            onSuccess = { isUnlocked = true }
                        )
                    }

                    // Wallpaper Picker Dialog
                    if (showWallpaperPicker) {
                        WallpaperPickerDialog(
                            currentIndex = wallpaperIndex,
                            onDismiss = { showWallpaperPicker = false },
                            onSelectIndex = { index ->
                                wallpaperIndex = index
                                ChatStorage.setWallpaperIndex(this@MainActivity, index)
                            }
                        )
                    }
                }
            }

        }
    }

    private fun openConversation(conv: ChatConversation) {
        activeConversation = conv
        messages.clear()

        if (isOnlineMode) {
            // Real-time messages from Firestore
            startFirebaseMessagesObserver(conv.id)
        } else {
            val loaded = if (ChatStorage.hasSavedMessages(this, conv.id)) {
                ChatStorage.loadMessages(this, conv.id)
            } else {
                val initial = getInitialMessagesFor(conv)
                ChatStorage.saveMessages(this, conv.id, initial)
                initial
            }

            val filtered = if (conv.disappearingHours > 0) {
                val cutoff = System.currentTimeMillis() - (conv.disappearingHours * 3600 * 1000L)
                loaded.filter { it.timestamp >= cutoff }
            } else {
                loaded
            }
            messages.addAll(filtered)
        }
    }

    private fun saveCurrentConversation() {
        activeConversation?.let { conv ->
            ChatStorage.saveMessages(this, conv.id, messages)
            val lastMsg = messages.lastOrNull()
            if (lastMsg != null) {
                val index = conversations.indexOfFirst { it.id == conv.id }
                if (index != -1) {
                    val preview = when (lastMsg.type) {
                        MessageType.IMAGE -> "📷 صورة"
                        MessageType.VOICE -> "🎤 تسجيل صوتي"
                        MessageType.DOCUMENT -> "📄 ${lastMsg.fileName ?: "مستند"}"
                        MessageType.LOCATION -> "📍 موقع جغرافي"
                        else -> lastMsg.text
                    }
                    val updated = conv.copy(
                        lastMessage = preview,
                        lastMessageTime = lastMsg.timestamp,
                        unreadCount = 0
                    )
                    conversations[index] = updated
                    activeConversation = updated
                    ChatStorage.saveConversations(this, conversations)
                }
            }
        }
    }

        private fun createNewNamedConversation(name: String, pin: String?) {
        val colors = listOf(0xFF00897BL, 0xFF1E88E5L, 0xFFE91E63L, 0xFFFB8C00L, 0xFF8E24AAL, 0xFF43A047L)
        val randomColor = colors.random()

        if (isOnlineMode) {
            lifecycleScope.launch {
                val result = firebaseRepo.createChat(
                    name = name,
                    isGroup = true,
                    avatarColor = randomColor
                )
                if (result.isSuccess) {
                    val chatId = result.getOrNull()!!
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "تم إنشاء \"$name\"\nالمعرف: $chatId\nشاركه مع أصدقائك",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "فشل: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            val newConv = ChatConversation(
                id = "chat_${System.currentTimeMillis()}",
                name = name,
                avatarColor = randomColor,
                lastMessage = "محادثة تم إنشاؤها حديثاً",
                lastMessageTime = System.currentTimeMillis(),
                pinLock = pin
            )
            conversations.add(0, newConv)
            ChatStorage.saveConversations(this, conversations)
            Toast.makeText(
                this,
                "تم إنشاء المحادثة \"$name\"" + if (pin != null) " مع حماية PIN" else "",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun renameConversation(conv: ChatConversation, newName: String) {
        val index = conversations.indexOfFirst { it.id == conv.id }
        if (index != -1) {
            val updated = conv.copy(name = newName)
            conversations[index] = updated
            if (activeConversation?.id == conv.id) {
                activeConversation = updated
            }
            ChatStorage.saveConversations(this, conversations)
            Toast.makeText(this, "تم تغيير الاسم إلى: $newName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setConversationPin(conv: ChatConversation, pin: String) {
        val index = conversations.indexOfFirst { it.id == conv.id }
        if (index != -1) {
            val updated = conv.copy(pinLock = pin)
            conversations[index] = updated
            if (activeConversation?.id == conv.id) {
                activeConversation = updated
            }
            ChatStorage.saveConversations(this, conversations)
            Toast.makeText(this, "تم تعيين رمز الدخول للمحادثة بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeConversationPin(conv: ChatConversation) {
        val index = conversations.indexOfFirst { it.id == conv.id }
        if (index != -1) {
            val updated = conv.copy(pinLock = null)
            conversations[index] = updated
            if (activeConversation?.id == conv.id) {
                activeConversation = updated
            }
            ChatStorage.saveConversations(this, conversations)
            Toast.makeText(this, "تم إلغاء قفل المحادثة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteConversation(conv: ChatConversation) {
        val index = conversations.indexOfFirst { it.id == conv.id }
        if (index != -1) {
            conversations.removeAt(index)
            ChatStorage.clearChat(this, conv.id)
            ChatStorage.saveConversations(this, conversations)
            if (activeConversation?.id == conv.id) {
                activeConversation = null
            }
            Toast.makeText(this, "تم حذف المحادثة واختفاؤها من القائمة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePinConversation(conv: ChatConversation) {
        val index = conversations.indexOfFirst { it.id == conv.id }
        if (index != -1) {
            val updated = conv.copy(isPinned = !conv.isPinned)
            conversations[index] = updated
            if (activeConversation?.id == conv.id) {
                activeConversation = updated
            }
            ChatStorage.saveConversations(this, conversations)
            Toast.makeText(
                this,
                if (updated.isPinned) "تم تثبيت المحادثة في القمة 📌" else "تم إلغاء تثبيت المحادثة",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleArchiveConversation(conv: ChatConversation) {
        val index = conversations.indexOfFirst { it.id == conv.id }
        if (index != -1) {
            val updated = conv.copy(isArchived = !conv.isArchived)
            conversations[index] = updated
            if (activeConversation?.id == conv.id) {
                activeConversation = updated
            }
            ChatStorage.saveConversations(this, conversations)
            Toast.makeText(
                this,
                if (updated.isArchived) "تمت أرشفة المحادثة 📦" else "تمت إزالة المحادثة من الأرشيف",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleReadConversation(conv: ChatConversation) {
        val index = conversations.indexOfFirst { it.id == conv.id }
        if (index != -1) {
            val updated = conv.copy(unreadCount = if (conv.unreadCount > 0) 0 else 1)
            conversations[index] = updated
            if (activeConversation?.id == conv.id) {
                activeConversation = updated
            }
            ChatStorage.saveConversations(this, conversations)
            Toast.makeText(
                this,
                if (updated.unreadCount > 0) "تم التمييز كغير مقروءة ✉️" else "تم التمييز كمقروءة ✓",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setDisappearingMessages(conv: ChatConversation, hours: Int) {
        val index = conversations.indexOfFirst { it.id == conv.id }
        if (index != -1) {
            val updated = conv.copy(disappearingHours = hours)
            conversations[index] = updated
            activeConversation = updated
            ChatStorage.saveConversations(this, conversations)
            val msgText = when (hours) {
                24 -> "تم تفعيل الرسائل ذاتية الاختفاء بعد 24 ساعة ⏱️"
                168 -> "تم تفعيل الرسائل ذاتية الاختفاء بعد 7 أيام ⏱️"
                else -> "تم إيقاف الرسائل ذاتية الاختفاء"
            }
            Toast.makeText(this, msgText, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleStarMessage(messageId: String) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val old = messages[index]
            val updated = old.copy(isStarred = !old.isStarred)
            messages[index] = updated
            saveCurrentConversation()
            Toast.makeText(
                this,
                if (updated.isStarred) "تم تمييز الرسالة بنجمة ⭐" else "تم إلغاء تمييز الرسالة بنجمة",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun exportChat(conv: ChatConversation) {
        if (messages.isEmpty()) {
            Toast.makeText(this, "لا توجد رسائل لتصديرها", Toast.LENGTH_SHORT).show()
            return
        }
        val builder = StringBuilder()
        builder.appendLine("--- سجل محادثة ChatDZ 🇩🇿 ---")
        builder.appendLine("المحادثة: ${conv.name}")
        builder.appendLine("تاريخ التصدير: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
        builder.appendLine("---------------------------")
        builder.appendLine()
        for (msg in messages) {
            val time = dateFormat.format(Date(msg.timestamp))
            val sender = msg.sender
            val content = when (msg.type) {
                MessageType.IMAGE -> if (msg.text.isNotBlank()) "📷 [صورة]: ${msg.text}" else "📷 [صورة]"
                MessageType.VOICE -> "🎙️ [تسجيل صوتي: ${msg.audioDurationSeconds} ثانية]"
                MessageType.DOCUMENT -> "📄 [مستند: ${msg.fileName ?: "ملف"}]"
                MessageType.LOCATION -> "📍 [موقع: ${msg.locationName ?: "موقع جغرافي"}]"
                else -> msg.text
            }
            builder.appendLine("[$time] $sender: $content")
        }
        builder.appendLine()
        builder.appendLine("تم تصديرها عبر ChatDZ")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, builder.toString())
            putExtra(Intent.EXTRA_SUBJECT, "سجل محادثة ChatDZ - ${conv.name}")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "مشاركة سجل المحادثة عبر")
        startActivity(shareIntent)
    }

    private fun getInitialMessagesFor(conv: ChatConversation): List<Message> {
        return listOf(
            Message(
                id = "init_1",
                text = "السلام عليكم ورحمة الله، مرحباً بك في ${conv.name}!",
                sender = conv.name,
                timestamp = System.currentTimeMillis() - 300000L,
                isOwn = false,
                reactions = mapOf("👋" to 2)
            ),
            Message(
                id = "init_2",
                text = "وعليكم السلام! يمكنك الآن تسمية هذه المحادثة، قفلها برمز PIN، أو حذفها متى أردت.",
                sender = "أنت",
                timestamp = System.currentTimeMillis() - 180000L,
                isOwn = true,
                status = MessageStatus.READ,
                replyTo = MessageReplyInfo("init_1", conv.name, "السلام عليكم ورحمة الله"),
                reactions = mapOf("🔥" to 3)
            )
        )
    }

    override fun onPause() {
        super.onPause()
        saveCurrentConversation()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveCurrentConversation()
        audioRecorderManager.release()
    }

    private fun calculateExpiresAt(): Long? {
        val hours = activeConversation?.disappearingHours ?: 0
        return if (hours > 0) {
            System.currentTimeMillis() + (hours * 3600 * 1000L)
        } else null
    }

    private fun sendTextMessage(text: String, replyTo: MessageReplyInfo?) {
        val conv = activeConversation
        if (isOnlineMode && conv != null) {
            lifecycleScope.launch {
                sendFirebaseMessage(
                    chatId = conv.id,
                    text = text,
                    type = MessageType.TEXT,
                    replyTo = replyTo
                )
                // The Firestore listener will add the message automatically
            }
        } else {
            val newMsg = Message(
                id = System.currentTimeMillis().toString(),
                text = text,
                sender = "أنت",
                timestamp = System.currentTimeMillis(),
                isOwn = true,
                type = MessageType.TEXT,
                status = MessageStatus.READ,
                replyTo = replyTo,
                expiresAt = calculateExpiresAt()
            )
            messages.add(newMsg)
            saveCurrentConversation()
        }
    }

    private fun sendImageMessage(uri: Uri, replyTo: MessageReplyInfo?, caption: String = "") {
        val conv = activeConversation
        if (isOnlineMode && conv != null) {
            lifecycleScope.launch {
                val upload = firebaseRepo.uploadMedia(conv.id, uri, MessageType.IMAGE)
                if (upload.isSuccess) {
                    sendFirebaseMessage(
                        chatId = conv.id,
                        text = caption,
                        type = MessageType.IMAGE,
                        mediaUri = upload.getOrNull(),
                        replyTo = replyTo
                    )
                } else {
                    // Fallback: save locally + notify
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, upload.exceptionOrNull()?.message ?: "تعذر رفع الصورة", Toast.LENGTH_LONG).show()
                    }
                    val savedPath = ImageUtils.saveUriToInternalStorage(this@MainActivity, uri) ?: uri.toString()
                    val newMsg = Message(
                        id = System.currentTimeMillis().toString(),
                        text = caption,
                        sender = "أنت",
                        timestamp = System.currentTimeMillis(),
                        isOwn = true,
                        type = MessageType.IMAGE,
                        mediaUri = savedPath,
                        status = MessageStatus.READ,
                        replyTo = replyTo,
                        expiresAt = calculateExpiresAt()
                    )
                    messages.add(newMsg)
                    saveCurrentConversation()
                }
            }
        } else {
            val savedPath = ImageUtils.saveUriToInternalStorage(this, uri) ?: uri.toString()
            val newMsg = Message(
                id = System.currentTimeMillis().toString(),
                text = caption,
                sender = "أنت",
                timestamp = System.currentTimeMillis(),
                isOwn = true,
                type = MessageType.IMAGE,
                mediaUri = savedPath,
                status = MessageStatus.READ,
                replyTo = replyTo,
                expiresAt = calculateExpiresAt()
            )
            messages.add(newMsg)
            saveCurrentConversation()
        }
    }

    private fun sendVoiceMessage(filePath: String, durationSeconds: Int, replyTo: MessageReplyInfo?) {
        val conv = activeConversation
        if (isOnlineMode && conv != null) {
            lifecycleScope.launch {
                val localUri = android.net.Uri.fromFile(java.io.File(filePath))
                val upload = firebaseRepo.uploadMedia(conv.id, localUri, MessageType.VOICE)
                if (upload.isSuccess) {
                    sendFirebaseMessage(
                        chatId = conv.id,
                        type = MessageType.VOICE,
                        mediaUri = upload.getOrNull(),
                        audioDurationSeconds = durationSeconds,
                        replyTo = replyTo
                    )
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "تعذر رفع الرسالة الصوتية (Storage غير مفعّل)", Toast.LENGTH_LONG).show()
                    }
                    // Still keep locally
                    val newMsg = Message(
                        id = System.currentTimeMillis().toString(),
                        text = "",
                        sender = "أنت",
                        timestamp = System.currentTimeMillis(),
                        isOwn = true,
                        type = MessageType.VOICE,
                        mediaUri = filePath,
                        audioDurationSeconds = durationSeconds,
                        status = MessageStatus.READ,
                        replyTo = replyTo,
                        expiresAt = calculateExpiresAt()
                    )
                    messages.add(newMsg)
                    saveCurrentConversation()
                }
            }
        } else {
            val newMsg = Message(
                id = System.currentTimeMillis().toString(),
                text = "",
                sender = "أنت",
                timestamp = System.currentTimeMillis(),
                isOwn = true,
                type = MessageType.VOICE,
                mediaUri = filePath,
                audioDurationSeconds = durationSeconds,
                status = MessageStatus.READ,
                replyTo = replyTo,
                expiresAt = calculateExpiresAt()
            )
            messages.add(newMsg)
            saveCurrentConversation()
        }
    }

    private fun sendDocumentMessage(name: String, size: String, path: String, replyTo: MessageReplyInfo?) {
        val conv = activeConversation
        if (isOnlineMode && conv != null) {
            lifecycleScope.launch {
                val localUri = android.net.Uri.fromFile(java.io.File(path))
                val upload = firebaseRepo.uploadMedia(conv.id, localUri, MessageType.DOCUMENT, name)
                if (upload.isSuccess) {
                    sendFirebaseMessage(
                        chatId = conv.id,
                        type = MessageType.DOCUMENT,
                        mediaUri = upload.getOrNull(),
                        fileName = name,
                        fileSize = size,
                        replyTo = replyTo
                    )
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "تعذر رفع الملف (Storage غير مفعّل)", Toast.LENGTH_LONG).show()
                    }
                    val newMsg = Message(
                        id = System.currentTimeMillis().toString(),
                        text = "",
                        sender = "أنت",
                        timestamp = System.currentTimeMillis(),
                        isOwn = true,
                        type = MessageType.DOCUMENT,
                        fileName = name,
                        fileSize = size,
                        mediaUri = path,
                        status = MessageStatus.READ,
                        replyTo = replyTo,
                        expiresAt = calculateExpiresAt()
                    )
                    messages.add(newMsg)
                    saveCurrentConversation()
                }
            }
        } else {
            val newMsg = Message(
                id = System.currentTimeMillis().toString(),
                text = "",
                sender = "أنت",
                timestamp = System.currentTimeMillis(),
                isOwn = true,
                type = MessageType.DOCUMENT,
                fileName = name,
                fileSize = size,
                mediaUri = path,
                status = MessageStatus.READ,
                replyTo = replyTo,
                expiresAt = calculateExpiresAt()
            )
            messages.add(newMsg)
            saveCurrentConversation()
        }
    }

    private fun sendLocationMessage(lat: Double, lng: Double, name: String, replyTo: MessageReplyInfo?) {
        val conv = activeConversation
        if (isOnlineMode && conv != null) {
            lifecycleScope.launch {
                sendFirebaseMessage(
                    chatId = conv.id,
                    type = MessageType.LOCATION,
                    replyTo = replyTo,
                    latitude = lat,
                    longitude = lng,
                    locationName = name
                )
            }
        } else {
            val newMsg = Message(
                id = System.currentTimeMillis().toString(),
                text = "",
                sender = "أنت",
                timestamp = System.currentTimeMillis(),
                isOwn = true,
                type = MessageType.LOCATION,
                latitude = lat,
                longitude = lng,
                locationName = name,
                status = MessageStatus.READ,
                replyTo = replyTo,
                expiresAt = calculateExpiresAt()
            )
            messages.add(newMsg)
            saveCurrentConversation()
        }
    }

    private fun sendPollMessage(pollData: PollData, replyTo: MessageReplyInfo?) {
        val conv = activeConversation
        if (isOnlineMode && conv != null) {
            lifecycleScope.launch {
                sendFirebaseMessage(
                    chatId = conv.id,
                    text = "📊 استطلاع: " + pollData.question,
                    type = MessageType.POLL,
                    replyTo = replyTo,
                    pollData = pollData
                )
            }
        } else {
            val newMsg = Message(
                id = System.currentTimeMillis().toString(),
                text = "📊 استطلاع: " + pollData.question,
                sender = "أنت",
                timestamp = System.currentTimeMillis(),
                isOwn = true,
                type = MessageType.POLL,
                pollData = pollData,
                status = MessageStatus.READ,
                replyTo = replyTo,
                expiresAt = calculateExpiresAt()
            )
            messages.add(newMsg)
            saveCurrentConversation()
        }
    }

    private fun votePoll(message: Message, optionId: Int) {
        val index = messages.indexOfFirst { it.id == message.id }
        if (index != -1) {
            val msg = messages[index]
            val poll = msg.pollData ?: return
            val currentVoter = "me"
            val updatedOptions = poll.options.map { opt ->
                val hasVoted = opt.voters.contains(currentVoter)
                if (opt.id == optionId) {
                    if (hasVoted) {
                        opt.copy(votes = (opt.votes - 1).coerceAtLeast(0), voters = opt.voters - currentVoter)
                    } else {
                        opt.copy(votes = opt.votes + 1, voters = opt.voters + currentVoter)
                    }
                } else {
                    if (!poll.allowMultiple && hasVoted) {
                        opt.copy(votes = (opt.votes - 1).coerceAtLeast(0), voters = opt.voters - currentVoter)
                    } else {
                        opt
                    }
                }
            }
            messages[index] = msg.copy(pollData = poll.copy(options = updatedOptions))
            saveCurrentConversation()
        }
    }

    private fun batchDeleteMessages(ids: Set<String>) {
        messages.removeAll { ids.contains(it.id) }
        saveCurrentConversation()
        Toast.makeText(this, "تم حذف ${ids.size} رسالة", Toast.LENGTH_SHORT).show()
    }

    private fun batchStarMessages(ids: Set<String>) {
        var anyStarred = false
        for (i in 0 until messages.size) {
            if (ids.contains(messages[i].id)) {
                val newStarred = !messages[i].isStarred
                messages[i] = messages[i].copy(isStarred = newStarred)
                anyStarred = true
            }
        }
        if (anyStarred) {
            saveCurrentConversation()
            Toast.makeText(this, "تم تحديث تمييز الرسائل بالنجوم", Toast.LENGTH_SHORT).show()
        }
    }

    private fun forwardMessagesTo(messagesToForward: List<Message>, targetConv: ChatConversation) {
        val targetMessages = if (ChatStorage.hasSavedMessages(this, targetConv.id)) {
            ChatStorage.loadMessages(this, targetConv.id).toMutableList()
        } else {
            getInitialMessagesFor(targetConv).toMutableList()
        }

        for (msg in messagesToForward) {
            val fwdMsg = msg.copy(
                id = (System.currentTimeMillis() + (0..1000).random()).toString(),
                timestamp = System.currentTimeMillis(),
                sender = "أنت",
                isOwn = true,
                status = MessageStatus.READ,
                replyTo = null
            )
            targetMessages.add(fwdMsg)
        }
        ChatStorage.saveMessages(this, targetConv.id, targetMessages)
        Toast.makeText(this, "تم تحويل ${messagesToForward.size} رسالة إلى ${targetConv.name}", Toast.LENGTH_SHORT).show()
    }


    private fun deleteMessage(id: String) {
        val index = messages.indexOfFirst { it.id == id && it.isOwn }
        if (index != -1) {
            messages.removeAt(index)
            saveCurrentConversation()
            Toast.makeText(this, "تم حذف الرسالة نهائياً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editMessage(id: String, newText: String) {
        val index = messages.indexOfFirst { it.id == id && it.isOwn }
        if (index != -1) {
            val msg = messages[index]
            messages[index] = msg.copy(text = newText, isEdited = true)
            saveCurrentConversation()
            Toast.makeText(this, "تم تعديل الرسالة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePinMessage(id: String) {
        val index = messages.indexOfFirst { it.id == id }
        if (index != -1) {
            val msg = messages[index]
            val newPinState = !msg.isPinned
            messages[index] = msg.copy(isPinned = newPinState)
            saveCurrentConversation()
            Toast.makeText(
                this,
                if (newPinState) "تم تثبيت الرسالة في الأعلى" else "تم إلغاء تثبيت الرسالة",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleReaction(id: String, emoji: String) {
        val index = messages.indexOfFirst { it.id == id }
        if (index != -1) {
            val msg = messages[index]
            val currentReactions = msg.reactions.toMutableMap()
            val count = currentReactions[emoji] ?: 0
            if (count > 0) {
                currentReactions.remove(emoji)
            } else {
                currentReactions[emoji] = 1
            }
            messages[index] = msg.copy(reactions = currentReactions)
            saveCurrentConversation()
        }
    }

    private fun clearCurrentChat() {
        activeConversation?.let { conv ->
            messages.clear()
            ChatStorage.clearChat(this, conv.id)
            Toast.makeText(this, "تم مسح كافة الرسائل بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ChatDZ", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "تم نسخ النص", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDZScreen(
    conversation: ChatConversation,
    messages: List<Message>,
    dateFormat: SimpleDateFormat,
    wallpaperIndex: Int,
    audioRecorderManager: AudioRecorderManager,
    onBack: () -> Unit,
    onSendMessage: (String, MessageReplyInfo?) -> Unit,
    onSendImage: (Uri, MessageReplyInfo?, String) -> Unit,
    onSendVoice: (String, Int, MessageReplyInfo?) -> Unit,
    onSendDocument: (String, String, String, MessageReplyInfo?) -> Unit,
    onSendLocation: (Double, Double, String, MessageReplyInfo?) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleStar: (String) -> Unit,
    onExportChat: () -> Unit,
    onSetDisappearing: (Int) -> Unit,
    onClearChat: () -> Unit,
    onRenameChat: (String) -> Unit,
    onSetChatPin: (String) -> Unit,
    onRemoveChatPin: () -> Unit,
    onDeleteChat: () -> Unit,
    onCopyText: (String) -> Unit,
    onOpenChatInfo: () -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onOpenCreatePoll: () -> Unit,
    onVotePoll: (Message, Int) -> Unit,
    onBatchDelete: (Set<String>) -> Unit,
    onBatchStar: (Set<String>) -> Unit,
    onBatchForward: (List<Message>) -> Unit
) {
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedMessageIds = remember { mutableStateListOf<String>() }

    BackHandler {
        if (isMultiSelectMode) {
            isMultiSelectMode = false
            selectedMessageIds.clear()
        } else {
            onBack()
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<MessageReplyInfo?>(null) }
    var actionDialogMessage by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var fullScreenImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }

    // Search in chat state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialogs for chat options
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showDeleteChatConfirmDialog by remember { mutableStateOf(false) }
    var showStarredMessagesDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }

    // Peer typing simulation
    var isPeerTyping by remember { mutableStateOf(false) }

    // Pinned message
    val pinnedMessage = remember(messages) { messages.lastOrNull { it.isPinned } }

    // Wallpaper background color
    val wallpaperTheme = WALLPAPER_THEMES.getOrElse(wallpaperIndex) { WALLPAPER_THEMES[0] }
    val wallpaperColor = if (MaterialTheme.colorScheme.background.red < 0.5f) {
        if (wallpaperTheme.darkColor != Color.Transparent) wallpaperTheme.darkColor else MaterialTheme.colorScheme.background
    } else {
        if (wallpaperTheme.lightColor != Color.Transparent) wallpaperTheme.lightColor else MaterialTheme.colorScheme.background
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedMessageIds.clear()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "إلغاء التحديد",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "${selectedMessageIds.size} محددة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        // Select / Deselect all
                        IconButton(onClick = {
                            if (selectedMessageIds.size == messages.size) {
                                selectedMessageIds.clear()
                                isMultiSelectMode = false
                            } else {
                                selectedMessageIds.clear()
                                selectedMessageIds.addAll(messages.map { it.id })
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check_double),
                                contentDescription = "تحديد الكل",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        // Star/unstar
                        IconButton(onClick = {
                            onBatchStar(selectedMessageIds.toSet())
                            isMultiSelectMode = false
                            selectedMessageIds.clear()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_star),
                                contentDescription = "تمييز بنجمة",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        // Forward
                        IconButton(onClick = {
                            val selectedMsgs = messages.filter { selectedMessageIds.contains(it.id) }
                            onBatchForward(selectedMsgs)
                            isMultiSelectMode = false
                            selectedMessageIds.clear()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_forward),
                                contentDescription = "تحويل الرسائل",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        // Delete
                        IconButton(onClick = {
                            onBatchDelete(selectedMessageIds.toSet())
                            isMultiSelectMode = false
                            selectedMessageIds.clear()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = "حذف المحدد",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("بحث في الرسائل...", fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChatInfo() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(conversation.avatarColor), shape = RoundedCornerShape(19.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = conversation.name.take(1),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = conversation.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        if (conversation.pinLock != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_lock),
                                                contentDescription = "محمية برمز دخول",
                                                tint = Color(0xFFFFD54F),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isPeerTyping) "يكتب الآن..." else if (conversation.isOnline) "متصل الآن" else "غير متصل",
                                        fontSize = 11.sp,
                                        color = if (isPeerTyping) Color(0xFFFFD54F) else if (conversation.isOnline) Color(0xFFB2FF59) else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = onStartAudioCall) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_call),
                                    contentDescription = "مكالمة صوتية",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            IconButton(onClick = onStartVideoCall) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_videocall),
                                    contentDescription = "مكالمة فيديو",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        // Search in chat icon
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

                        // More Options Menu
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "خيارات إضافية",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("معلومات المحادثة والوسائط ℹ️") },
                                    onClick = {
                                        showOptionsMenu = false
                                        onOpenChatInfo()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("تعديل اسم المحادثة") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showRenameDialog = true
                                    }
                                )

                            if (conversation.pinLock != null) {
                                DropdownMenuItem(
                                    text = { Text("إلغاء رمز دخول المحادثة") },
                                    onClick = {
                                        showOptionsMenu = false
                                        onRemoveChatPin()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تغيير رمز الدخول") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showSetPinDialog = true
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("وضع رمز دخول للمحادثة") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showSetPinDialog = true
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text(if (isPeerTyping) "إيقاف مؤشر الكتابة" else "محاكاة 'يكتب الآن'") },
                                onClick = {
                                    isPeerTyping = !isPeerTyping
                                    showOptionsMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("الرسائل المميزة بنجمة ⭐") },
                                onClick = {
                                    showOptionsMenu = false
                                    showStarredMessagesDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    val disappearText = when (conversation.disappearingHours) {
                                        24 -> "الرسائل ذاتية الاختفاء (24 ساعة) ⏱️"
                                        168 -> "الرسائل ذاتية الاختفاء (7 أيام) ⏱️"
                                        else -> "الرسائل ذاتية الاختفاء (معطلة) ⏱️"
                                    }
                                    Text(disappearText)
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showDisappearingDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("تصدير سجل المحادثة 📤") },
                                onClick = {
                                    showOptionsMenu = false
                                    onExportChat()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("مسح رسائل المحادثة", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOptionsMenu = false
                                    showClearConfirmDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("حذف المحادثة نهائياً", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOptionsMenu = false
                                    showDeleteChatConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    },
    bottomBar = {
            MessageInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSendMessage = { text ->
                    onSendMessage(text, replyingTo)
                    inputText = ""
                    replyingTo = null
                },
                onSendImage = { uri, caption ->
                    onSendImage(uri, replyingTo, caption)
                    replyingTo = null
                },
                onSendDocument = { name, size, path ->
                    onSendDocument(name, size, path, replyingTo)
                    replyingTo = null
                },
                onSendLocation = { lat, lng, name ->
                    onSendLocation(lat, lng, name, replyingTo)
                    replyingTo = null
                },
                replyTo = replyingTo,
                onCancelReply = { replyingTo = null },
                onStartRecording = {
                    audioRecorderManager.startRecording()
                },
                onStopRecording = {
                    val result = audioRecorderManager.stopRecording()
                    if (result != null) {
                        onSendVoice(result.first, result.second, replyingTo)
                        replyingTo = null
                    }
                },
                onCancelRecording = {
                    audioRecorderManager.cancelRecording()
                },
                onOpenCreatePoll = onOpenCreatePoll
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(wallpaperColor)
        ) {
            // Pinned Message Banner
            PinnedMessageBanner(
                message = pinnedMessage,
                onClick = {
                    pinnedMessage?.let { pinMsg ->
                        val idx = messages.indexOfFirst { it.id == pinMsg.id }
                        if (idx != -1) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(idx)
                            }
                        }
                    }
                },
                onUnpin = {
                    pinnedMessage?.let { onTogglePin(it.id) }
                }
            )

            // Search matches count indicator
            if (isSearchActive && searchQuery.isNotBlank()) {
                val matchesCount = messages.count {
                    it.text.contains(searchQuery, ignoreCase = true) ||
                    (it.fileName != null && it.fileName.contains(searchQuery, ignoreCase = true))
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "عدد النتائج المطابقة: $matchesCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Messages LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    MessageItem(
                        message = message,
                        dateFormat = dateFormat,
                        currentlyPlayingUri = currentlyPlayingUri,
                        playbackSpeed = audioRecorderManager.currentSpeed,
                        searchQuery = searchQuery,
                        isSelectionMode = isMultiSelectMode,
                        isSelected = selectedMessageIds.contains(message.id),
                        onToggleSelect = { msg ->
                            if (selectedMessageIds.contains(msg.id)) {
                                selectedMessageIds.remove(msg.id)
                                if (selectedMessageIds.isEmpty()) {
                                    isMultiSelectMode = false
                                }
                            } else {
                                selectedMessageIds.add(msg.id)
                            }
                        },
                        onVotePoll = onVotePoll,
                        onTogglePlayAudio = { uri ->
                            if (currentlyPlayingUri == uri) {
                                audioRecorderManager.stopPlayback()
                                currentlyPlayingUri = null
                            } else {
                                audioRecorderManager.playAudio(uri) {
                                    currentlyPlayingUri = null
                                }
                                currentlyPlayingUri = uri
                            }
                        },
                        onCycleSpeed = {
                            audioRecorderManager.cycleSpeed()
                        },
                        onSeekAudio = { percent ->
                            audioRecorderManager.seekToPercent(percent)
                        },
                        onImageClick = { bitmap ->
                            fullScreenImageBitmap = bitmap
                        },
                        onLongClick = {
                            if (!isMultiSelectMode && !message.isDeleted) {
                                actionDialogMessage = message
                            }
                        },
                        onReactionClick = { emoji ->
                            onToggleReaction(message.id, emoji)
                        }
                    )
                }
            }

            // Typing Indicator
            if (isPeerTyping) {
                TypingIndicator(senderName = conversation.name)
            }
        }

        // Action & Emoji Reactions Dialog
        if (actionDialogMessage != null) {
            MessageActionDialog(
                message = actionDialogMessage!!,
                onDismiss = { actionDialogMessage = null },
                onReactionSelect = { emoji ->
                    onToggleReaction(actionDialogMessage!!.id, emoji)
                },
                onReply = {
                    replyingTo = MessageReplyInfo(
                        id = actionDialogMessage!!.id,
                        sender = actionDialogMessage!!.sender,
                        text = actionDialogMessage!!.text,
                        type = actionDialogMessage!!.type
                    )
                },
                onDelete = {
                    onDeleteMessage(actionDialogMessage!!.id)
                },
                onCopy = {
                    onCopyText(actionDialogMessage!!.text)
                },
                onEdit = {
                    editingMessage = actionDialogMessage
                },
                onPinToggle = {
                    onTogglePin(actionDialogMessage!!.id)
                },
                onStarToggle = {
                    onToggleStar(actionDialogMessage!!.id)
                },
                onForward = {
                    val msg = actionDialogMessage!!
                    actionDialogMessage = null
                    onBatchForward(listOf(msg))
                },
                onSelectMultiple = {
                    val msg = actionDialogMessage!!
                    actionDialogMessage = null
                    isMultiSelectMode = true
                    selectedMessageIds.clear()
                    selectedMessageIds.add(msg.id)
                }
            )
        }

        // Edit Message Dialog
        if (editingMessage != null) {
            EditMessageDialog(
                initialText = editingMessage!!.text,
                onDismiss = { editingMessage = null },
                onConfirm = { updatedText ->
                    onEditMessage(editingMessage!!.id, updatedText)
                }
            )
        }

        // Rename Conversation Dialog (From inside chat)
        if (showRenameDialog) {
            RenameConversationDialog(
                currentName = conversation.name,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    onRenameChat(newName)
                }
            )
        }

        // Set/Change PIN Dialog (From inside chat)
        if (showSetPinDialog) {
            SetConversationPinDialog(
                conversationName = conversation.name,
                onDismiss = { showSetPinDialog = false },
                onConfirm = { pin ->
                    onSetChatPin(pin)
                }
            )
        }

        // Clear Chat Confirmation Dialog
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("مسح رسائل المحادثة") },
                text = { Text("هل أنت متأكد من رغبتك في حذف جميع الرسائل في هذه المحادثة نهائياً؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearChat()
                            showClearConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("مسح الآن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Delete Chat Confirmation Dialog (From inside chat)
        if (showDeleteChatConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteChatConfirmDialog = false },
                title = { Text("حذف المحادثة بالكامل") },
                text = { Text("هل أنت متأكد من حذف هذه المحادثة بالكامل؟ سيتم الخروج منها وحذف كافة بياناتها.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteChatConfirmDialog = false
                            onDeleteChat()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف نهائي")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteChatConfirmDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Starred Messages Dialog
        if (showStarredMessagesDialog) {
            val starredList = remember(messages) { messages.filter { it.isStarred } }
            AlertDialog(
                onDismissRequest = { showStarredMessagesDialog = false },
                title = { Text("الرسائل المميزة بنجمة ⭐") },
                text = {
                    if (starredList.isEmpty()) {
                        Text("لا توجد رسائل مميزة بنجمة في هذه المحادثة حتى الآن. يمكنك تمييز أي رسالة بالضغط المطول عليها واختيار 'تمييز بنجمة'.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(starredList, key = { it.id }) { sMsg ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sMsg.sender,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = when (sMsg.type) {
                                                    MessageType.IMAGE -> "📷 صورة" + if (sMsg.text.isNotBlank()) ": ${sMsg.text}" else ""
                                                    MessageType.VOICE -> "🎙️ تسجيل صوتي"
                                                    MessageType.DOCUMENT -> "📄 ${sMsg.fileName ?: "مستند"}"
                                                    MessageType.LOCATION -> "📍 ${sMsg.locationName ?: "موقع"}"
                                                    else -> sMsg.text
                                                },
                                                fontSize = 13.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = { onToggleStar(sMsg.id) }) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_star),
                                                contentDescription = "إلغاء التمييز",
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showStarredMessagesDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }

        // Disappearing Messages Options Dialog
        if (showDisappearingDialog) {
            AlertDialog(
                onDismissRequest = { showDisappearingDialog = false },
                title = { Text("الرسائل ذاتية الاختفاء ⏱️") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("اختر مدة بقاء الرسائل قبل اختفائها تلقائياً:")
                        val currentH = conversation.disappearingHours
                        OutlinedButton(
                            onClick = {
                                onSetDisappearing(0)
                                showDisappearingDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (currentH == 0) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("إيقاف (الوضع العادي)")
                        }
                        OutlinedButton(
                            onClick = {
                                onSetDisappearing(24)
                                showDisappearingDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (currentH == 24) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("بعد 24 ساعة")
                        }
                        OutlinedButton(
                            onClick = {
                                onSetDisappearing(168)
                                showDisappearingDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (currentH == 168) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("بعد 7 أيام")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisappearingDialog = false }) {
                        Text("إلغاء")
                    }
                },
                confirmButton = {}
            )
        }

        // Fullscreen Image Dialog
        if (fullScreenImageBitmap != null) {
            FullScreenImageViewer(
                bitmap = fullScreenImageBitmap,
                onDismiss = { fullScreenImageBitmap = null }
            )
        }
    }

    // ==================== Firebase Helpers ====================
    private fun startFirebaseConversationObserver() {
        if (!isOnlineMode) return
        lifecycleScope.launch {
            firebaseRepo.observeMyConversations().collectLatest { remoteConvs ->
                // Merge with local or replace
                conversations.clear()
                conversations.addAll(remoteConvs)
            }
        }
    }

    private fun startFirebaseMessagesObserver(chatId: String) {
        if (!isOnlineMode) return
        lifecycleScope.launch {
            firebaseRepo.observeMessages(chatId).collectLatest { remoteMsgs ->
                messages.clear()
                messages.addAll(remoteMsgs)
            }
        }
    }

    private suspend fun sendFirebaseMessage(
        chatId: String,
        text: String = "",
        type: MessageType = MessageType.TEXT,
        mediaUri: String? = null,
        audioDurationSeconds: Int = 0,
        replyTo: MessageReplyInfo? = null,
        fileName: String? = null,
        fileSize: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        pollData: PollData? = null
    ) {
        val result = firebaseRepo.sendMessage(
            chatId = chatId,
            text = text,
            type = type,
            mediaUri = mediaUri,
            audioDurationSeconds = audioDurationSeconds,
            replyTo = replyTo,
            fileName = fileName,
            fileSize = fileSize,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            pollData = pollData
        )
        if (result.isFailure) {
            runOnUiThread {
                Toast.makeText(this, "فشل إرسال الرسالة: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}