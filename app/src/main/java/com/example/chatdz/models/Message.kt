package com.example.chatdz.models

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    DOCUMENT,
    LOCATION,
    POLL
}

enum class MessageStatus {
    SENT,
    DELIVERED,
    READ
}

data class MessageReplyInfo(
    val id: String,
    val sender: String,
    val text: String,
    val type: MessageType = MessageType.TEXT
)

data class PollOption(
    val id: Int,
    val text: String,
    val votes: Int = 0,
    val voters: List<String> = emptyList()
)

data class PollData(
    val question: String,
    val options: List<PollOption>,
    val allowMultiple: Boolean = false,
    val isClosed: Boolean = false
)

data class UserProfile(
    val name: String = "أنا (المستخدم)",
    val bio: String = "مرحباً! أنا أستخدم تطبيق ChatDZ الرائع 🇩🇿",
    val phone: String = "+213 555 123 456",
    val statusText: String = "متاح",
    val avatarColor: Long = 0xFF008069
)

data class Message(
    val id: String,
    val text: String = "",
    val sender: String,
    val timestamp: Long,
    val isOwn: Boolean,
    val type: MessageType = MessageType.TEXT,
    val mediaUri: String? = null,
    val audioDurationSeconds: Int = 0,
    val replyTo: MessageReplyInfo? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val isDeleted: Boolean = false,
    val status: MessageStatus = MessageStatus.READ,
    val isEdited: Boolean = false,
    val isPinned: Boolean = false,
    val fileName: String? = null,
    val fileSize: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val isStarred: Boolean = false,
    val pollData: PollData? = null,
    val expiresAt: Long? = null
)

data class ChatConversation(
    val id: String,
    val name: String,
    val avatarColor: Long,
    val isGroup: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOnline: Boolean = true,
    val pinLock: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val disappearingHours: Int = 0,
    val description: String = "",
    val members: List<String> = emptyList()
)

