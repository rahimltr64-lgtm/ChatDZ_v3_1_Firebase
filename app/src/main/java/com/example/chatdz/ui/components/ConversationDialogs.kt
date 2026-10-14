package com.example.chatdz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chatdz.R
import com.example.chatdz.models.ChatConversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, pin: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var enablePin by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "إنشاء وتسمية محادثة جديدة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المحادثة / الشخص") },
                    placeholder = { Text("مثال: مشروع العمل، عمر، عائلة...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { enablePin = !enablePin }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = enablePin,
                        onCheckedChange = { enablePin = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حماية المحادثة برمز دخول (PIN)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (enablePin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("رمز الدخول (PIN)") },
                        placeholder = { Text("أدخل 4 أرقام...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim(), if (enablePin && pin.isNotBlank()) pin.trim() else null)
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إنشاء المحادثة")
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationOptionsDialog(
    conversation: ChatConversation,
    onDismiss: () -> Unit,
    onRename: (ChatConversation) -> Unit,
    onSetPin: (ChatConversation) -> Unit,
    onRemovePin: (ChatConversation) -> Unit,
    onTogglePin: (ChatConversation) -> Unit,
    onToggleArchive: (ChatConversation) -> Unit,
    onToggleRead: (ChatConversation) -> Unit,
    onDelete: (ChatConversation) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = conversation.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Pin / Unpin option
                ConversationActionRow(
                    iconRes = R.drawable.ic_pin,
                    title = if (conversation.isPinned) "إلغاء تثبيت المحادثة" else "تثبيت في القمة 📌",
                    color = Color(0xFFFFB300),
                    onClick = {
                        onTogglePin(conversation)
                        onDismiss()
                    }
                )

                // Archive / Unarchive option
                ConversationActionRow(
                    iconRes = R.drawable.ic_archive,
                    title = if (conversation.isArchived) "إلغاء أرشفة المحادثة" else "أرشفة المحادثة 📦",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onToggleArchive(conversation)
                        onDismiss()
                    }
                )

                // Mark as Read / Unread
                ConversationActionRow(
                    iconRes = R.drawable.ic_check_double,
                    title = if (conversation.unreadCount > 0) "تمييز كمقروءة ✓" else "تمييز كغير مقروءة ✉️",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        onToggleRead(conversation)
                        onDismiss()
                    }
                )

                // Rename option
                ConversationActionRow(
                    iconRes = R.drawable.ic_edit,
                    title = "تعديل اسم المحادثة",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onRename(conversation)
                        onDismiss()
                    }
                )

                // PIN lock option
                if (conversation.pinLock != null) {
                    ConversationActionRow(
                        iconRes = R.drawable.ic_lock,
                        title = "إلغاء رمز الدخول للمحادثة",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            onRemovePin(conversation)
                            onDismiss()
                        }
                    )
                    ConversationActionRow(
                        iconRes = R.drawable.ic_lock,
                        title = "تغيير رمز الدخول الحالي",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            onSetPin(conversation)
                            onDismiss()
                        }
                    )
                } else {
                    ConversationActionRow(
                        iconRes = R.drawable.ic_lock,
                        title = "وضع رمز دخول للمحادثة (PIN)",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            onSetPin(conversation)
                            onDismiss()
                        }
                    )
                }

                // Delete option
                ConversationActionRow(
                    iconRes = R.drawable.ic_delete,
                    title = "حذف المحادثة نهائياً",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDelete(conversation)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

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
private fun ConversationActionRow(
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
            .padding(vertical = 12.dp, horizontal = 8.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameConversationDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "تعديل اسم المحادثة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الجديد") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim())
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ التسمية")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetConversationPinDialog(
    conversationName: String,
    onDismiss: () -> Unit,
    onConfirm: (pin: String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "تعيين رمز دخول لـ $conversationName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "لن يمكن فتح هذه المحادثة إلا بإدخال رمز الـ PIN المحدد",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("رمز الدخول (4 أرقام)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (pin.isNotBlank()) {
                                onConfirm(pin.trim())
                                onDismiss()
                            }
                        },
                        enabled = pin.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تعيين الرمز")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyAddChatCodeDialog(
    expectedCode: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onChangeCodeRequest: () -> Unit
) {
    var enteredCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHint by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_key),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "رمز حماية إضافة المحادثات 🔐",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "إضافة محادثات جديدة مقفلة برمز سري. يرجى إدخال الرمز للسماح بالإنشاء:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = enteredCode,
                    onValueChange = {
                        enteredCode = it
                        errorMessage = null
                    },
                    label = { Text("رمز الأمان") },
                    placeholder = { Text("أدخل رمز التحقق...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hint and Change Code Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showHint = !showHint }) {
                        Text(
                            if (showHint) "تلميح: الرمز الافتراضي 1962 🇩🇿" else "عرض التلميح 💡",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(onClick = onChangeCodeRequest) {
                        Text(
                            "تغيير الرمز",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (enteredCode.trim() == expectedCode.trim()) {
                                onSuccess()
                            } else {
                                errorMessage = "الرمز المدخل غير صحيح! حاول مرة أخرى."
                            }
                        },
                        enabled = enteredCode.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تحقق ومتابعة")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeAddChatCodeDialog(
    currentExpectedCode: String,
    onDismiss: () -> Unit,
    onConfirmNewCode: (String) -> Unit
) {
    var oldCode by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "تغيير رمز حماية إضافة المحادثات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = oldCode,
                    onValueChange = { oldCode = it; errorMessage = null },
                    label = { Text("الرمز الحالي") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newCode,
                    onValueChange = { newCode = it; errorMessage = null },
                    label = { Text("الرمز الجديد") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (oldCode.trim() != currentExpectedCode.trim()) {
                                errorMessage = "الرمز الحالي غير صحيح!"
                            } else if (newCode.trim().length < 3) {
                                errorMessage = "الرمز الجديد يجب أن لا يقل عن 3 خانات"
                            } else {
                                onConfirmNewCode(newCode.trim())
                                onDismiss()
                            }
                        },
                        enabled = oldCode.isNotBlank() && newCode.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ الرمز")
                    }
                }
            }
        }
    }
}

