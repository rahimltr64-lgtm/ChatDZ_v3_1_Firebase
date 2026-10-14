package com.example.chatdz.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.chatdz.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firebase repository for real-time chat between friends.
 * Uses:
 * - Firebase Auth (Anonymous + display name)
 * - Cloud Firestore for users, chats, messages
 * - Firebase Storage for media (images, voice, documents)
 */
class FirebaseChatRepository private constructor() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUid: String?
        get() = auth.currentUser?.uid

    // ==================== AUTH ====================

    suspend fun signInAnonymously(displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: return Result.failure(Exception("User is null"))
            // Save profile
            val profile = hashMapOf(
                "uid" to user.uid,
                "name" to displayName,
                "bio" to "مرحباً! أنا أستخدم ChatDZ 🇩🇿",
                "statusText" to "متاح",
                "avatarColor" to listOf(0xFF008069, 0xFF128C7E, 0xFF25D366, 0xFF34B7F1, 0xFF9C27B0, 0xFFE91E63).random(),
                "createdAt" to FieldValue.serverTimestamp(),
                "lastSeen" to FieldValue.serverTimestamp(),
                "isOnline" to true
            )
            db.collection("users").document(user.uid).set(profile).await()
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signInAnonymously failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(name: String? = null, bio: String? = null, statusText: String? = null, avatarColor: Long? = null): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val updates = mutableMapOf<String, Any>()
            name?.let { updates["name"] = it }
            bio?.let { updates["bio"] = it }
            statusText?.let { updates["statusText"] = it }
            avatarColor?.let { updates["avatarColor"] = it }
            updates["lastSeen"] = FieldValue.serverTimestamp()
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setOnlineStatus(isOnline: Boolean) {
        val uid = currentUid ?: return
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "setOnlineStatus failed", e)
        }
    }

    suspend fun getUserProfile(uid: String = currentUid ?: ""): UserProfile? {
        if (uid.isEmpty()) return null
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                UserProfile(
                    name = doc.getString("name") ?: "مستخدم",
                    bio = doc.getString("bio") ?: "",
                    phone = doc.getString("phone") ?: "",
                    statusText = doc.getString("statusText") ?: "متاح",
                    avatarColor = (doc.getLong("avatarColor") ?: 0xFF008069)
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "getUserProfile failed", e)
            null
        }
    }

    // ==================== CONVERSATIONS ====================

    /**
     * Create a new private chat or group.
     * For private: pass friendUid.
     * For group: pass list of member uids + name.
     */
    suspend fun createChat(
        name: String,
        isGroup: Boolean = false,
        memberUids: List<String> = emptyList(),
        avatarColor: Long = 0xFF008069
    ): Result<String> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val members = (memberUids + uid).distinct()
            val chatId = db.collection("chats").document().id
            val chatData = hashMapOf(
                "id" to chatId,
                "name" to name,
                "isGroup" to isGroup,
                "avatarColor" to avatarColor,
                "members" to members,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastMessage" to "",
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "lastMessageSender" to "",
                "description" to if (isGroup) "مجموعة دردشة" else ""
            )
            db.collection("chats").document(chatId).set(chatData).await()

            // Also add chat reference under each member for easy querying
            for (member in members) {
                db.collection("users").document(member)
                    .collection("myChats").document(chatId)
                    .set(mapOf(
                        "chatId" to chatId,
                        "joinedAt" to FieldValue.serverTimestamp()
                    )).await()
            }
            Result.success(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "createChat failed", e)
            Result.failure(e)
        }
    }

    /**
     * Join a chat by ID (simple invite by sharing chat ID or code).
     */
    suspend fun joinChat(chatId: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val chatRef = db.collection("chats").document(chatId)
            val chat = chatRef.get().await()
            if (!chat.exists()) return Result.failure(Exception("المحادثة غير موجودة"))

            chatRef.update("members", FieldValue.arrayUnion(uid)).await()
            db.collection("users").document(uid)
                .collection("myChats").document(chatId)
                .set(mapOf("chatId" to chatId, "joinedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMyConversations(): Flow<List<ChatConversation>> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Listen to chats where current user is a member
        val registration = db.collection("chats")
            .whereArrayContains("members", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeMyConversations error", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ChatConversation(
                            id = doc.id,
                            name = doc.getString("name") ?: "محادثة",
                            avatarColor = doc.getLong("avatarColor") ?: 0xFF008069,
                            isGroup = doc.getBoolean("isGroup") ?: false,
                            lastMessage = doc.getString("lastMessage") ?: "",
                            lastMessageTime = doc.getTimestamp("lastMessageTime")?.toDate()?.time
                                ?: System.currentTimeMillis(),
                            unreadCount = 0, // can be improved later
                            isOnline = true,
                            description = doc.getString("description") ?: "",
                            members = (doc.get("members") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "parse conversation failed", e)
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { registration.remove() }
    }

    // ==================== MESSAGES ====================

    fun observeMessages(chatId: String, limit: Long = 100): Flow<List<Message>> = callbackFlow {
        val registration = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeMessages error", error)
                    return@addSnapshotListener
                }
                val uid = currentUid
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val typeStr = doc.getString("type") ?: "TEXT"
                        val type = try { MessageType.valueOf(typeStr) } catch (_: Exception) { MessageType.TEXT }
                        val statusStr = doc.getString("status") ?: "SENT"
                        val status = try { MessageStatus.valueOf(statusStr) } catch (_: Exception) { MessageStatus.SENT }

                        val replyMap = doc.get("replyTo") as? Map<*, *>
                        val replyTo = if (replyMap != null) {
                            MessageReplyInfo(
                                id = replyMap["id"] as? String ?: "",
                                sender = replyMap["sender"] as? String ?: "",
                                text = replyMap["text"] as? String ?: "",
                                type = try {
                                    MessageType.valueOf(replyMap["type"] as? String ?: "TEXT")
                                } catch (_: Exception) { MessageType.TEXT }
                            )
                        } else null

                        val reactions = (doc.get("reactions") as? Map<*, *>)?.mapNotNull { (k, v) ->
                            (k as? String)?.let { it to ((v as? Long)?.toInt() ?: 0) }
                        }?.toMap() ?: emptyMap()

                        val pollMap = doc.get("pollData") as? Map<*, *>
                        val pollData = if (pollMap != null) {
                            val options = (pollMap["options"] as? List<*>)?.mapNotNull { opt ->
                                val o = opt as? Map<*, *> ?: return@mapNotNull null
                                PollOption(
                                    id = (o["id"] as? Long)?.toInt() ?: 0,
                                    text = o["text"] as? String ?: "",
                                    votes = (o["votes"] as? Long)?.toInt() ?: 0,
                                    voters = (o["voters"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                )
                            } ?: emptyList()
                            PollData(
                                question = pollMap["question"] as? String ?: "",
                                options = options,
                                allowMultiple = pollMap["allowMultiple"] as? Boolean ?: false,
                                isClosed = pollMap["isClosed"] as? Boolean ?: false
                            )
                        } else null

                        Message(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            sender = doc.getString("senderName") ?: doc.getString("sender") ?: "?",
                            timestamp = doc.getTimestamp("timestamp")?.toDate()?.time
                                ?: doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            isOwn = doc.getString("senderUid") == uid,
                            type = type,
                            mediaUri = doc.getString("mediaUri"),
                            audioDurationSeconds = (doc.getLong("audioDurationSeconds") ?: 0L).toInt(),
                            replyTo = replyTo,
                            reactions = reactions,
                            isDeleted = doc.getBoolean("isDeleted") ?: false,
                            status = status,
                            isEdited = doc.getBoolean("isEdited") ?: false,
                            isPinned = doc.getBoolean("isPinned") ?: false,
                            fileName = doc.getString("fileName"),
                            fileSize = doc.getString("fileSize"),
                            latitude = doc.getDouble("latitude"),
                            longitude = doc.getDouble("longitude"),
                            locationName = doc.getString("locationName"),
                            isStarred = doc.getBoolean("isStarred") ?: false,
                            pollData = pollData,
                            expiresAt = doc.getLong("expiresAt")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "parse message failed", e)
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(
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
        pollData: PollData? = null,
        expiresAt: Long? = null
    ): Result<String> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        val profile = getUserProfile(uid)
        val senderName = profile?.name ?: "مستخدم"

        return try {
            val msgId = UUID.randomUUID().toString()
            val msgData = mutableMapOf<String, Any?>(
                "id" to msgId,
                "text" to text,
                "senderUid" to uid,
                "senderName" to senderName,
                "timestamp" to FieldValue.serverTimestamp(),
                "type" to type.name,
                "status" to MessageStatus.SENT.name,
                "isDeleted" to false,
                "isEdited" to false,
                "isPinned" to false,
                "isStarred" to false,
                "audioDurationSeconds" to audioDurationSeconds,
                "reactions" to emptyMap<String, Int>()
            )
            mediaUri?.let { msgData["mediaUri"] = it }
            fileName?.let { msgData["fileName"] = it }
            fileSize?.let { msgData["fileSize"] = it }
            latitude?.let { msgData["latitude"] = it }
            longitude?.let { msgData["longitude"] = it }
            locationName?.let { msgData["locationName"] = it }
            expiresAt?.let { msgData["expiresAt"] = it }

            if (replyTo != null) {
                msgData["replyTo"] = mapOf(
                    "id" to replyTo.id,
                    "sender" to replyTo.sender,
                    "text" to replyTo.text,
                    "type" to replyTo.type.name
                )
            }

            if (pollData != null) {
                msgData["pollData"] = mapOf(
                    "question" to pollData.question,
                    "allowMultiple" to pollData.allowMultiple,
                    "isClosed" to pollData.isClosed,
                    "options" to pollData.options.map { opt ->
                        mapOf(
                            "id" to opt.id,
                            "text" to opt.text,
                            "votes" to opt.votes,
                            "voters" to opt.voters
                        )
                    }
                )
            }

            // Remove nulls
            val cleanData = msgData.filterValues { it != null }

            db.collection("chats").document(chatId)
                .collection("messages").document(msgId)
                .set(cleanData).await()

            // Update last message on chat
            val lastText = when (type) {
                MessageType.TEXT -> text.take(80)
                MessageType.IMAGE -> "📷 صورة"
                MessageType.VOICE -> "🎤 رسالة صوتية"
                MessageType.DOCUMENT -> "📄 ملف"
                MessageType.LOCATION -> "📍 موقع"
                MessageType.POLL -> "📊 استطلاع: ${pollData?.question?.take(40) ?: ""}"
            }
            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage" to lastText,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "lastMessageSender" to senderName
                )
            ).await()

            Result.success(msgId)
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateMessage(chatId: String, messageId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String, forEveryone: Boolean = true): Result<Unit> {
        return if (forEveryone) {
            updateMessage(chatId, messageId, mapOf("isDeleted" to true, "text" to "تم حذف هذه الرسالة"))
        } else {
            // soft local only - for now same
            updateMessage(chatId, messageId, mapOf("isDeleted" to true, "text" to "تم حذف هذه الرسالة"))
        }
    }

    // ==================== MEDIA UPLOAD ====================

    /**
     * Upload media to Firebase Storage.
     * If Storage is not enabled / fails, returns failure so caller can fallback to local-only.
     */
    suspend fun uploadMedia(chatId: String, localUri: Uri, type: MessageType, fileName: String? = null): Result<String> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val extension = when (type) {
                MessageType.IMAGE -> "jpg"
                MessageType.VOICE -> "m4a"
                MessageType.DOCUMENT -> fileName?.substringAfterLast('.') ?: "bin"
                else -> "bin"
            }
            val path = "chats/$chatId/${type.name.lowercase()}/${uid}_${System.currentTimeMillis()}.$extension"
            val ref = storage.reference.child(path)
            ref.putFile(localUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "uploadMedia failed (Storage may not be enabled)", e)
            // Return failure so the app can still send text messages without media
            Result.failure(Exception("رفع الملفات غير متاح حالياً (Storage غير مفعّل). يمكنك إرسال نصوص واستطلاعات."))
        }
    }

    // ==================== HELPERS ====================

    suspend fun searchUsersByName(query: String, limit: Int = 20): List<Pair<String, UserProfile>> {
        if (query.isBlank()) return emptyList()
        return try {
            // Note: Firestore needs composite indexes for complex queries.
            // For simplicity we do a simple prefix-ish search (client-side filter after limited fetch)
            val snapshot = db.collection("users")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(limit.toLong())
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                val uid = doc.id
                if (uid == currentUid) return@mapNotNull null
                val profile = UserProfile(
                    name = doc.getString("name") ?: return@mapNotNull null,
                    bio = doc.getString("bio") ?: "",
                    statusText = doc.getString("statusText") ?: "",
                    avatarColor = doc.getLong("avatarColor") ?: 0xFF008069
                )
                uid to profile
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchUsers failed", e)
            emptyList()
        }
    }

    fun signOut() {
        auth.signOut()
    }

    companion object {
        private const val TAG = "FirebaseChatRepo"
        @Volatile private var INSTANCE: FirebaseChatRepository? = null

        fun getInstance(): FirebaseChatRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseChatRepository().also { INSTANCE = it }
            }
        }
    }
}
