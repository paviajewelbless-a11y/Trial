package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.ui.components.LocalIsDesktopLayout
import com.example.ui.components.desktopPointerHand
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusWatchTextDark
import com.example.viewmodel.AiInsightState

@Composable
fun AiOpsScreen(
    insightState: AiInsightState,
    chatMessages: List<ChatMessage>,
    isChatSending: Boolean,
    onRunPlantAnalysis: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isDesktop = LocalIsDesktopLayout.current

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            val targetIdx = if (isDesktop) chatMessages.lastIndex else chatMessages.lastIndex + 1
            listState.animateScrollToItem(targetIdx.coerceAtLeast(0))
        }
    }

    val sendPromptAction: () -> Unit = {
        val text = inputPrompt.trim()
        if (text.isNotBlank() && !isChatSending) {
            inputPrompt = ""
            onSendChatMessage(text)
        }
    }

    if (isDesktop) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlantIntelligenceCard(
                    insightState = insightState,
                    onRunPlantAnalysis = onRunPlantAnalysis
                )
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChatHeaderAndQuickPrompts(
                        hasMessages = chatMessages.isNotEmpty(),
                        onClearChat = onClearChat,
                        onSendChatMessage = onSendChatMessage
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages, key = { it.id }) { msg ->
                        ChatBubbleRow(msg = msg, maxWidthDp = 520)
                    }
                }

                ChatInputBar(
                    inputPrompt = inputPrompt,
                    onInputChange = { inputPrompt = it },
                    isChatSending = isChatSending,
                    isDesktop = true,
                    onSend = sendPromptAction
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PlantIntelligenceCard(
                        insightState = insightState,
                        onRunPlantAnalysis = onRunPlantAnalysis
                    )
                }

                item {
                    ChatHeaderAndQuickPrompts(
                        hasMessages = chatMessages.isNotEmpty(),
                        onClearChat = onClearChat,
                        onSendChatMessage = onSendChatMessage
                    )
                }

                items(chatMessages, key = { it.id }) { msg ->
                    ChatBubbleRow(msg = msg, maxWidthDp = 320)
                }
            }

            ChatInputBar(
                inputPrompt = inputPrompt,
                onInputChange = { inputPrompt = it },
                isChatSending = isChatSending,
                isDesktop = false,
                onSend = sendPromptAction
            )
        }
    }
}

@Composable
private fun PlantIntelligenceCard(
    insightState: AiInsightState,
    onRunPlantAnalysis: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gemini Plant Intelligence", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Structured risk & shift analysis grounded in Bulacan Plant Firestore state",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onRunPlantAnalysis,
                    enabled = insightState !is AiInsightState.Loading,
                    modifier = Modifier
                        .desktopPointerHand()
                        .testTag("analyze_plant_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestEmerald,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyze")
                }
            }

            when (insightState) {
                AiInsightState.Idle -> {
                    Text(
                        "Tap Analyze to generate structured stockout alerts, recovery warnings, and next-shift production priorities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AiInsightState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Analyzing inventory, dispatches, and recovery batches...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AiInsightState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = insightState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                is AiInsightState.Success -> {
                    val data = insightState.insight
                    Text(data.executiveSummary, style = MaterialTheme.typography.bodyMedium)
                    if (data.stockoutAlerts.isNotEmpty()) {
                        InsightSection("Stockout & Order Alerts", data.stockoutAlerts, StatusCriticalTextDark)
                    }
                    if (data.recoveryWarnings.isNotEmpty()) {
                        InsightSection("Recovery Variance Warnings", data.recoveryWarnings, StatusWatchTextDark)
                    }
                    if (data.shiftRecommendations.isNotEmpty()) {
                        InsightSection("Next-Shift Recommendations", data.shiftRecommendations, MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatHeaderAndQuickPrompts(
    hasMessages: Boolean,
    onClearChat: () -> Unit,
    onSendChatMessage: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Plant Ops Assistant Chat", style = MaterialTheme.typography.titleMedium)
            TextButton(
                onClick = onClearChat,
                enabled = hasMessages,
                modifier = Modifier
                    .desktopPointerHand()
                    .testTag("reset_ai_chat_button")
            ) {
                Icon(
                    Icons.Filled.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Chat")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Which SKUs are at stockout risk?",
                "Summarize our Oct recovery losses",
                "Plan tomorrow's MIXING/CONVEYOR shift"
            ).forEach { quick ->
                AssistChip(
                    onClick = { onSendChatMessage(quick) },
                    label = { Text(quick) },
                    modifier = Modifier.desktopPointerHand()
                )
            }
        }
    }
}

@Composable
private fun ChatBubbleRow(
    msg: ChatMessage,
    maxWidthDp: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (msg.isUser) ForestEmerald else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (msg.isUser) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = maxWidthDp.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (msg.isUser) "You" else "Bulacan Ops AI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.text.ifBlank { if (msg.isStreaming) "Thinking..." else "" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputPrompt: String,
    onInputChange: (String) -> Unit,
    isChatSending: Boolean,
    isDesktop: Boolean,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        if (isDesktop) "Ask about stock, orders, recovery, or shift plans (Enter to send)..."
                        else "Ask about stock, orders, recovery, or shift plans..."
                    )
                },
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            (event.key == Key.Enter || event.key == Key.NumPadEnter) &&
                            !event.isShiftPressed
                        ) {
                            onSend()
                            true
                        } else {
                            false
                        }
                    }
                    .testTag("chat_input_field")
            )
            Button(
                onClick = onSend,
                enabled = inputPrompt.isNotBlank() && !isChatSending,
                modifier = Modifier
                    .height(52.dp)
                    .desktopPointerHand()
                    .testTag("chat_send_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                if (isChatSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                }
            }
        }
    }
}

@Composable
private fun InsightSection(
    title: String,
    items: List<String>,
    accent: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = accent)
        }
        items.forEach { bullet ->
            Text("• $bullet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
