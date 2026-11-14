package com.example.mad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RemsGold
import com.example.ui.theme.RemsNavyCard
import com.example.ui.theme.RemsNavyDark
import kotlinx.coroutines.launch

/**
 * MAD live-chat screen — naya feature overlay.
 * HomeScreen ke MAD button se MadActivity isi screen ko dikhati hai.
 * v2: jab tak API key nahi hai, chat ke upar BADA setup banner dikhta hai.
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

    // key status — settings se wapas aane par refresh bhi ho jaye
    var hasKey by remember { mutableStateOf(MadEngine.hasApiKey(context)) }
    DisposableEffect(Unit) {
        onDispose { }
    }

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

    // settings se wapas aaye to key-status dobara check
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            hasKey = MadEngine.hasApiKey(context)
        }
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
                    text = "\uD83C\uDFA4 MAD \u2014 Live Assistant",
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

            // ===== BADA API KEY SETUP BANNER (jab tak key nahi hai) =====
            if (!hasKey) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = RemsGold)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "\u26A0\uFE0F API Key abhi set NAHI hai",
                            color = Color(0xFF0D1B2A),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "MAD bol payegi iske baad hi. Key free milti hai.",
                            color = Color(0xFF1D2B45),
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D1B2A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                "\uD83D\uDD11 API Key Daalo (Settings)",
                                color = RemsGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (messages.isEmpty() && !hasKey) {
                    item {
                        Text(
                            "Pehle upar wala button dabakar API key daalo, phir MAD se baat karo.",
                            color = Color(0xFF9FB8D9),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
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
private fun MadBubble(msg: MadChatMessage) {
    val bg = if (msg.fromUser) RemsNavyCard else Color(0xFF233355)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        contentAlignment = if (msg.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (msg.fromUser) 14.dp else 4.dp,
                bottomEnd = if (msg.fromUser) 4.dp else 14.dp
            )
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

/**
 * MAD ko message bhejo, reply chat me dikhao (aur chaho to bol bhi do).
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
