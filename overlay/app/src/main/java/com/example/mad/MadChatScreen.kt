package com.example.mad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RemsGold
import com.example.ui.theme.RemsNavyCard
import com.example.ui.theme.RemsNavyDark
import kotlinx.coroutines.launch

/**
 * MAD live-chat screen — naya feature overlay.
 * HomeScreen ke MAD button se yahan aate hain; original screens untouched.
 */
data class MadChatMessage(
    val id: Long = System.nanoTime(),
    val fromUser: Boolean,
    val text: String
)

@Composable
fun MadChatScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<MadChatMessage>() }
    var input by remember { mutableStateOf("") }
    var madBusy by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val madVoice = remember { MadVoice(context) }
    DisposableEffect(Unit) {
        madVoice.onHeard = { text ->
            if (text.isNotBlank()) {
                scope.launch {
                    sendToMad(
                        context = context,
                        messages = messages,
                        userText = text,
                        speakReply = true,
                        madVoice = madVoice,
                        setBusy = { madBusy = it }
                    )
                }
            }
        }
        madVoice.onListeningStateChanged = { lst -> listening = lst }
        onDispose { madVoice.destroy() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RemsNavyDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎙 MAD — Live Assistant",
                    color = RemsGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, "MAD Settings", tint = Color.White)
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, "Close MAD", tint = Color.White)
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MadBubble(msg)
                }
                if (madBusy) {
                    item {
                        Text(
                            "MAD soch rahi hai...",
                            color = Color(0xFF9FB8D9),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Bol ya likho...", color = Color(0xFF9FB8D9)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RemsGold,
                        unfocusedBorderColor = Color(0xFF44557A),
                        cursorColor = RemsGold
                    ),
                    shape = RoundedCornerShape(22.dp),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        madVoice.stopSpeaking()
                        if (listening) {
                            madVoice.stopListening()
                        } else {
                            madVoice.startListening()
                        }
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        "Mic",
                        tint = if (listening) Color(0xFF66FF99) else RemsGold,
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(
                    onClick = {
                        if (input.isNotBlank() && !madBusy) {
                            val text = input
                            input = ""
                            scope.launch {
                                sendToMad(
                                    context = context,
                                    messages = messages,
                                    userText = text,
                                    speakReply = false,
                                    madVoice = madVoice,
                                    setBusy = { madBusy = it }
                                )
                            }
                        }
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        "Send",
                        tint = RemsGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MadBubble(msg: MadChatMessage) {
    val bg = if (msg.fromUser) RemsNavyCard else Color(0xFF233355)
    val align = if (msg.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = align)
    ) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(
                topStart = 14.dp, topEnd = 14.dp,
                bottomStart = if (msg.fromUser) 14.dp else 4.dp,
                bottomEnd = if (msgfromUser(msg)) 4.dp else 14.dp
            ),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .align(align)
        ) {
            Text(
                text = msg.text,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

private fun msgfromUser(m: MadChatMessage) = m.fromUser

/**
 * MAD ko message bhejo, reply aane par chat me dikhao (aur chaho to bol bhi do).
 */
suspend fun sendToMad(
    context: android.content.Context,
    messages: SnapshotStateList<MadChatMessage>,
    userText: String,
    speakReply: Boolean,
    madVoice: MadVoice?,
    setBusy: (Boolean) -> Unit
) {
    if (userText.isBlank()) return
    messages.add(MadChatMessage(fromUser = true, text = userText))
    setBusy(true)

    val history = messages
        .takeLast(12)
        .map { (if (it.fromUser) "user" else "model") to it.text }

    val reply = MadEngine.chat(context, userText, history)
    messages.add(MadChatMessage(fromUser = false, text = reply))
    setBusy(false)

    if (speakReply && madVoice != null) {
        madVoice.speak(reply)
    }
}
