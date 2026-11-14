package com.example.mad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.RemsGold
import com.example.ui.theme.RemsNavyCard
import com.example.ui.theme.RemsNavyDark

/**
 * MAD live-chat screen — naya feature overlay.
 * App ke kisi bhi existing screen ka layout untouched rehta hai;
 * HomeScreen ke MAD button se yahan aate hain.
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
            if (text.isNotBlank()) sendToMad(context, messages, text) { busy -> madBusy = busy }
        }
        madVoice.onListeningStateChanged = { lst -> listening = lst }
        onDispose { madVoice.destroy() }
    }

    // Nayi message aane par auto-scroll
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
            // Header
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
                    Icon(Icons.Default.Stop, "Close MAD", tint = Color.White)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Chat list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) aur
