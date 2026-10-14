package com.example.chatdz.utils

import android.content.Context
import android.util.Log
import com.example.chatdz.models.ChatConversation
import com.example.chatdz.models.Message
import com.example.chatdz.models.MessageReplyInfo
import com.example.chatdz.models.MessageStatus
import com.example.chatdz.models.MessageType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ChatStorage {
    private const val TAG = "ChatStorage"
    private const val PREFS_NAME = "chatdz_settings"
    private const val KEY_DARK_MODE = "pref_dark_mode"
    private const val KEY_WALLPAPER = "pref_wallpaper"
    private const val KEY_APP_LOCKED = "pref_app_locked"
    private const val KEY_APP_PIN = "pref_app_pin"
    private const val KEY_ADD_CHAT_CODE = "pref_add_chat_code"
    private const val KEY_USER_NAME = "pref_user_name"
    private const val KEY_USER_BIO = "pref_user_bio"
    private const val KEY_USER_PHONE = "pref_user_phone"
    private const val KEY_USER_STATUS = "pref_user_status"
    private const val KEY_USER_AVATAR = "pref_user_avatar"
    private const val CONVERSATIONS_FILE = "conversations_list.json"

    // Default conversations
    val DEFAULT_CONVERSATIONS = listOf(
        ChatConversation(
            id = "chat_main",
            name = "مجموعة الجزائر للتقنية 🇩🇿",
            avatarColor = 0xFF00897BL,
            isGroup = true,
            lastMessage = "مرحباً بكم في ChatDZ مع كافة الميزات الجديدة!",
            lastMessageTime = System.currentTimeMillis() - 60000L,
            unreadCount = 0,
            isOnline = true,
            description = "مجموعة نقاش وتطوير التقنيات الحديثة في الجزائر",
            members = listOf("أحمد", "سارة", "كريم", "ياسمين", "أنا (المشرف)")
        ),
        ChatConversation(
            id = "chat_ahmed",
            name = "أحمد بن علي",
            avatarColor = 0xFF1E88E5L,
            isGroup = false,
            lastMessage = "أهلاً يا صديقي، هل جربت الميزات الجديدة؟",
            lastMessageTime = System.currentTimeMillis() - 3600000L,
            unreadCount = 2,
            isOnline = true,
            description = "مطور أندرويد ومهتم بالذكاء الاصطناعي"
        ),
        ChatConversation(
            id = "chat_sarah",
            name = "سارة قدور",
            avatarColor = 0xFFE91E63L,
            isGroup = false,
            lastMessage = "شكراً جزيلاً، الملف ممتاز جداً",
            lastMessageTime = System.currentTimeMillis() - 7200000L,
            unreadCount = 0,
            isOnline = false,
            description = "مصممة واجهات UI/UX"
        ),
        ChatConversation(
            id = "chat_karim",
            name = "كريم الجزائري",
            avatarColor = 0xFFFB8C00L,
            isGroup = false,
            lastMessage = "سأرسل لك الموقع بعد قليل إن شاء الله",
            lastMessageTime = System.currentTimeMillis() - 86400000L,
            unreadCount = 0,
            isOnline = true,
            description = "مهندس شبكات وسحابيات"
        )
    )

    fun hasSavedConversations(context: Context): Boolean {
        val file = File(context.filesDir, CONVERSATIONS_FILE)
        return file.exists() && file.length() > 2
    }

    fun saveConversations(context: Context, list: List<ChatConversation>) {
        try {
            val jsonArray = JSONArray()
            for (conv in list) {
                val obj = JSONObject().apply {
                    put("id", conv.id)
                    put("name", conv.name)
                    put("avatarColor", conv.avatarColor)
                    put("isGroup", conv.isGroup)
                    put("lastMessage", conv.lastMessage)
                    put("lastMessageTime", conv.lastMessageTime)
                    put("unreadCount", conv.unreadCount)
                    put("isOnline", conv.isOnline)
                    if (conv.pinLock != null) put("pinLock", conv.pinLock)
                    put("isPinned", conv.isPinned)
                    put("isArchived", conv.isArchived)
                    put("disappearingHours", conv.disappearingHours)
                    put("description", conv.description)
                    val membersArr = JSONArray()
                    conv.members.forEach { membersArr.put(it) }
                    put("members", membersArr)
                }
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, CONVERSATIONS_FILE)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving conversations", e)
        }
    }

    fun loadConversations(context: Context): List<ChatConversation> {
        val file = File(context.filesDir, CONVERSATIONS_FILE)
        if (!file.exists()) return DEFAULT_CONVERSATIONS

        return try {
            val content = file.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<ChatConversation>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val membersList = mutableListOf<String>()
                if (obj.has("members")) {
                    val arr = obj.getJSONArray("members")
                    for (m in 0 until arr.length()) {
                        membersList.add(arr.getString(m))
                    }
                }
                list.add(
                    ChatConversation(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        avatarColor = obj.getLong("avatarColor"),
                        isGroup = obj.optBoolean("isGroup", false),
                        lastMessage = obj.optString("lastMessage", ""),
                        lastMessageTime = obj.optLong("lastMessageTime", System.currentTimeMillis()),
                        unreadCount = obj.optInt("unreadCount", 0),
                        isOnline = obj.optBoolean("isOnline", true),
                        pinLock = if (obj.has("pinLock")) obj.getString("pinLock") else null,
                        isPinned = obj.optBoolean("isPinned", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        disappearingHours = obj.optInt("disappearingHours", 0),
                        description = obj.optString("description", ""),
                        members = membersList
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading conversations", e)
            emptyList()
        }
    }

    fun deleteConversation(context: Context, chatId: String) {
        clearChat(context, chatId)
        val current = loadConversations(context).filterNot { it.id == chatId }
        saveConversations(context, current)
    }

    fun hasSavedMessages(context: Context, chatId: String = "chat_main"): Boolean {
        val file = File(context.filesDir, "chat_${chatId}.json")
        return file.exists() && file.length() > 2
    }

    fun saveMessages(context: Context, chatId: String = "chat_main", messages: List<Message>) {
        try {
            val jsonArray = JSONArray()
            for (msg in messages) {
                val jsonMsg = JSONObject().apply {
                    put("id", msg.id)
                    put("text", msg.text)
                    put("sender", msg.sender)
                    put("timestamp", msg.timestamp)
                    put("isOwn", msg.isOwn)
                    put("type", msg.type.name)
                    if (msg.mediaUri != null) put("mediaUri", msg.mediaUri)
                    put("audioDurationSeconds", msg.audioDurationSeconds)
                    put("status", msg.status.name)
                    put("isEdited", msg.isEdited)
                    put("isPinned", msg.isPinned)
                    put("isStarred", msg.isStarred)
                    if (msg.expiresAt != null) put("expiresAt", msg.expiresAt)
                    if (msg.fileName != null) put("fileName", msg.fileName)
                    if (msg.fileSize != null) put("fileSize", msg.fileSize)
                    if (msg.latitude != null) put("latitude", msg.latitude)
                    if (msg.longitude != null) put("longitude", msg.longitude)
                    if (msg.locationName != null) put("locationName", msg.locationName)

                    if (msg.pollData != null) {
                        val pollObj = JSONObject().apply {
                            put("question", msg.pollData.question)
                            put("allowMultiple", msg.pollData.allowMultiple)
                            put("isClosed", msg.pollData.isClosed)
                            val optArray = JSONArray()
                            for (opt in msg.pollData.options) {
                                val optObj = JSONObject().apply {
                                    put("id", opt.id)
                                    put("text", opt.text)
                                    put("votes", opt.votes)
                                    val votersArr = JSONArray()
                                    opt.voters.forEach { votersArr.put(it) }
                                    put("voters", votersArr)
                                }
                                optArray.put(optObj)
                            }
                            put("options", optArray)
                        }
                        put("pollData", pollObj)
                    }

                    if (msg.replyTo != null) {
                        val replyJson = JSONObject().apply {
                            put("id", msg.replyTo.id)
                            put("sender", msg.replyTo.sender)
                            put("text", msg.replyTo.text)
                            put("type", msg.replyTo.type.name)
                        }
                        put("replyTo", replyJson)
                    }

                    if (msg.reactions.isNotEmpty()) {
                        val reactionsJson = JSONObject()
                        for ((emoji, count) in msg.reactions) {
                            reactionsJson.put(emoji, count)
                        }
                        put("reactions", reactionsJson)
                    }
                }
                jsonArray.put(jsonMsg)
            }

            val file = File(context.filesDir, "chat_${chatId}.json")
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving messages for $chatId", e)
        }
    }

    fun loadMessages(context: Context, chatId: String = "chat_main"): List<Message> {
        val file = File(context.filesDir, "chat_${chatId}.json")
        if (!file.exists()) {
            // Also check legacy file for chat_main
            if (chatId == "chat_main") {
                val legacy = File(context.filesDir, "chat_history.json")
                if (legacy.exists()) return parseMessagesFile(legacy)
            }
            return emptyList()
        }
        return parseMessagesFile(file)
    }

    private fun parseMessagesFile(file: File): List<Message> {
        return try {
            val content = file.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<Message>()
            val now = System.currentTimeMillis()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val text = obj.optString("text", "")
                val sender = obj.getString("sender")
                val timestamp = obj.getLong("timestamp")
                val isOwn = obj.getBoolean("isOwn")
                val typeName = obj.optString("type", MessageType.TEXT.name)
                val type = try {
                    MessageType.valueOf(typeName)
                } catch (_: Exception) {
                    MessageType.TEXT
                }
                val mediaUri = if (obj.has("mediaUri")) obj.getString("mediaUri") else null
                val audioDuration = obj.optInt("audioDurationSeconds", 0)

                val statusName = obj.optString("status", MessageStatus.READ.name)
                val status = try {
                    MessageStatus.valueOf(statusName)
                } catch (_: Exception) {
                    MessageStatus.READ
                }
                val isEdited = obj.optBoolean("isEdited", false)
                val isPinned = obj.optBoolean("isPinned", false)
                val isStarred = obj.optBoolean("isStarred", false)
                val expiresAt = if (obj.has("expiresAt")) obj.getLong("expiresAt") else null

                // Check disappearing messages expiration
                if (expiresAt != null && expiresAt > 0 && expiresAt < now) {
                    continue
                }

                val fileName = if (obj.has("fileName")) obj.getString("fileName") else null
                val fileSize = if (obj.has("fileSize")) obj.getString("fileSize") else null
                val latitude = if (obj.has("latitude")) obj.getDouble("latitude") else null
                val longitude = if (obj.has("longitude")) obj.getDouble("longitude") else null
                val locationName = if (obj.has("locationName")) obj.getString("locationName") else null

                val pollData = if (obj.has("pollData")) {
                    val pObj = obj.getJSONObject("pollData")
                    val q = pObj.getString("question")
                    val allowM = pObj.optBoolean("allowMultiple", false)
                    val isC = pObj.optBoolean("isClosed", false)
                    val optsArr = pObj.getJSONArray("options")
                    val optsList = mutableListOf<com.example.chatdz.models.PollOption>()
                    for (j in 0 until optsArr.length()) {
                        val oObj = optsArr.getJSONObject(j)
                        val vList = mutableListOf<String>()
                        if (oObj.has("voters")) {
                            val vArr = oObj.getJSONArray("voters")
                            for (k in 0 until vArr.length()) vList.add(vArr.getString(k))
                        }
                        optsList.add(
                            com.example.chatdz.models.PollOption(
                                id = oObj.getInt("id"),
                                text = oObj.getString("text"),
                                votes = oObj.optInt("votes", 0),
                                voters = vList
                            )
                        )
                    }
                    com.example.chatdz.models.PollData(
                        question = q,
                        options = optsList,
                        allowMultiple = allowM,
                        isClosed = isC
                    )
                } else null

                val replyTo = if (obj.has("replyTo")) {
                    val replyObj = obj.getJSONObject("replyTo")
                    val replyTypeName = replyObj.optString("type", MessageType.TEXT.name)
                    val replyType = try {
                        MessageType.valueOf(replyTypeName)
                    } catch (_: Exception) {
                        MessageType.TEXT
                    }
                    MessageReplyInfo(
                        id = replyObj.getString("id"),
                        sender = replyObj.getString("sender"),
                        text = replyObj.optString("text", ""),
                        type = replyType
                    )
                } else null

                val reactions = mutableMapOf<String, Int>()
                if (obj.has("reactions")) {
                    val reactionsObj = obj.getJSONObject("reactions")
                    val keys = reactionsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        reactions[key] = reactionsObj.getInt(key)
                    }
                }

                list.add(
                    Message(
                        id = id,
                        text = text,
                        sender = sender,
                        timestamp = timestamp,
                        isOwn = isOwn,
                        type = type,
                        mediaUri = mediaUri,
                        audioDurationSeconds = audioDuration,
                        replyTo = replyTo,
                        reactions = reactions,
                        status = status,
                        isEdited = isEdited,
                        isPinned = isPinned,
                        fileName = fileName,
                        fileSize = fileSize,
                        latitude = latitude,
                        longitude = longitude,
                        locationName = locationName,
                        isStarred = isStarred,
                        pollData = pollData,
                        expiresAt = expiresAt
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing messages", e)
            emptyList()
        }
    }

    fun clearChat(context: Context, chatId: String) {
        try {
            val file = File(context.filesDir, "chat_${chatId}.json")
            if (file.exists()) file.delete()
            if (chatId == "chat_main") {
                val legacy = File(context.filesDir, "chat_history.json")
                if (legacy.exists()) legacy.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing chat $chatId", e)
        }
    }

    // Settings helpers
    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun getWallpaperIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WALLPAPER, 0)
    }

    fun setWallpaperIndex(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_WALLPAPER, index).apply()
    }

    fun isAppLocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_APP_LOCKED, false)
    }

    fun setAppLocked(context: Context, locked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_APP_LOCKED, locked).apply()
    }

    fun getAppPin(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_PIN, "1234") ?: "1234"
    }

    fun setAppPin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_PIN, pin).apply()
    }

    // Add conversation code verification
    fun getAddChatCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ADD_CHAT_CODE, "1962") ?: "1962"
    }

    fun setAddChatCode(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ADD_CHAT_CODE, code).apply()
    }

    // User Profile
    fun getUserProfile(context: Context): com.example.chatdz.models.UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return com.example.chatdz.models.UserProfile(
            name = prefs.getString(KEY_USER_NAME, "أنا (المستخدم)") ?: "أنا (المستخدم)",
            bio = prefs.getString(KEY_USER_BIO, "مرحباً! أنا أستخدم تطبيق ChatDZ الرائع 🇩🇿") ?: "مرحباً! أنا أستخدم تطبيق ChatDZ الرائع 🇩🇿",
            phone = prefs.getString(KEY_USER_PHONE, "+213 555 123 456") ?: "+213 555 123 456",
            statusText = prefs.getString(KEY_USER_STATUS, "متاح") ?: "متاح",
            avatarColor = prefs.getLong(KEY_USER_AVATAR, 0xFF008069L)
        )
    }

    fun saveUserProfile(context: Context, profile: com.example.chatdz.models.UserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USER_NAME, profile.name)
            .putString(KEY_USER_BIO, profile.bio)
            .putString(KEY_USER_PHONE, profile.phone)
            .putString(KEY_USER_STATUS, profile.statusText)
            .putLong(KEY_USER_AVATAR, profile.avatarColor)
            .apply()
    }
}

