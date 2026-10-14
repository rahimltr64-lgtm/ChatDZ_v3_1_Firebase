package com.example.chatdz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chatdz.R
import com.example.chatdz.models.PollData
import com.example.chatdz.models.PollOption

@Composable
fun CreatePollDialog(
    onDismiss: () -> Unit,
    onCreatePoll: (PollData) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }
    var option4 by remember { mutableStateOf("") }
    var allowMultiple by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_poll),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إنشاء استطلاع رأي 📊",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("السؤال المطروح...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "خيارات التصويت:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = option1,
                    onValueChange = { option1 = it },
                    label = { Text("الخيار 1 (مطلوب)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = option2,
                    onValueChange = { option2 = it },
                    label = { Text("الخيار 2 (مطلوب)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = option3,
                    onValueChange = { option3 = it },
                    label = { Text("الخيار 3 (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = option4,
                    onValueChange = { option4 = it },
                    label = { Text("الخيار 4 (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = allowMultiple,
                        onCheckedChange = { allowMultiple = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "السماح باختيار إجابات متعددة",
                        style = MaterialTheme.typography.bodyMedium
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

                    val canCreate = question.isNotBlank() && option1.isNotBlank() && option2.isNotBlank()
                    Button(
                        onClick = {
                            val options = mutableListOf<PollOption>()
                            options.add(PollOption(id = 1, text = option1.trim()))
                            options.add(PollOption(id = 2, text = option2.trim()))
                            if (option3.isNotBlank()) options.add(PollOption(id = 3, text = option3.trim()))
                            if (option4.isNotBlank()) options.add(PollOption(id = 4, text = option4.trim()))

                            onCreatePoll(
                                PollData(
                                    question = question.trim(),
                                    options = options,
                                    allowMultiple = allowMultiple
                                )
                            )
                        },
                        enabled = canCreate,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("نشر الاستطلاع")
                    }
                }
            }
        }
    }
}
