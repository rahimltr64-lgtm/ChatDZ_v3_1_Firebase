package com.example.chatdz.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chatdz.R
import com.example.chatdz.models.ChatConversation
import kotlinx.coroutines.delay

@Composable
fun VoipCallDialog(
    conversation: ChatConversation,
    isVideoCall: Boolean,
    onEndCall: () -> Unit
) {
    var isConnected by remember { mutableStateOf(false) }
    var callSeconds by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isVideoCameraEnabled by remember { mutableStateOf(isVideoCall) }

    // Pulsing animation for avatar
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Simulate connection after 2.5 seconds
    LaunchedEffect(Unit) {
        delay(2500)
        isConnected = true
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    val formattedTime = remember(callSeconds) {
        val minutes = callSeconds / 60
        val seconds = callSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1B21)),
            contentAlignment = Alignment.Center
        ) {
            // Background subtle gradient/effect
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        text = if (isVideoCall) "مكالمة فيديو مشفرة 🔒" else "مكالمة صوتية مشفرة 🔒",
                        color = Color(0xFF00A884),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = conversation.name,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (!isConnected) "جاري الرنين والاتصال..." else formattedTime,
                        color = if (isConnected) Color.White.copy(alpha = 0.9f) else Color(0xFFFFD54F),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Center Avatar or Simulated Video
                Box(
                    modifier = Modifier
                        .size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideoCall && isVideoCameraEnabled && isConnected) {
                        // Simulated Video Surface
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1E2D36)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_videocall),
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "فيديو مباشر بجودة HD",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // Pulsing Avatar
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .scale(if (!isConnected) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(Color(conversation.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.name.take(1),
                                color = Color.White,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Controls Bottom Bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute button
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color(0xFFE53935) else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mic),
                                contentDescription = "كتم الصوت",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Speaker button
                        IconButton(
                            onClick = { isSpeakerOn = !isSpeakerOn },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isSpeakerOn) Color(0xFF00A884) else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_call),
                                contentDescription = "مكبر الصوت",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Video toggle (if video call)
                        if (isVideoCall) {
                            IconButton(
                                onClick = { isVideoCameraEnabled = !isVideoCameraEnabled },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isVideoCameraEnabled) Color(0xFF00A884) else Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_videocall),
                                    contentDescription = "الكاميرا",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // End call button
                    Button(
                        onClick = onEndCall,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier.size(72.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "إنهاء المكالمة",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "إنهاء المكالمة",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
