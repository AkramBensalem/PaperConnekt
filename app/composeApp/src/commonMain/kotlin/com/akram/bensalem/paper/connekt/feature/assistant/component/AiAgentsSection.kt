package com.akram.bensalem.paper.connekt.feature.assistant.component
import com.paperconnekt.app.resources.Res
import org.jetbrains.compose.resources.painterResource

import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.akram.bensalem.paper.connekt.feature.assistant.ChatMessage
import com.akram.bensalem.paper.connekt.feature.assistant.MessageSender
import com.paperconnekt.app.resources.filled_send

@Composable
fun AiAgentsSection(
    chatMessages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    errorMessage: String? = null,
) {
    var textState by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            errorMessage ?: "AI chat is not connected in this build. Configure a provider in Settings when AI responses are available.",
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(chatMessages) { message ->
                MessageBubble(message)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask the AI about this paper...") },
                enabled = false,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        onSendMessage(textState)
                        textState = ""
                    }
                },
                enabled = false, modifier = Modifier.interactivePointer(enabled = false)
            ) {
                Icon(painterResource(Res.drawable.filled_send), contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 12.dp
                    )
                )
                .background(color)
                .padding(12.dp)
        ) {
            Text(text = message.content, color = textColor)
        }
        Text(
            text = if (isUser) "You" else "AI Agent",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
