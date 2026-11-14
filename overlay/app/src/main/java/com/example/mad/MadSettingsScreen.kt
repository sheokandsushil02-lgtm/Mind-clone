package com.example.mad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RemsGold
import com.example.ui.theme.RemsNavyCard
import com.example.ui.theme.RemsNavyDark

/**
 * MAD settings — Gemini API key save karo. Original app settings se alag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MadSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var key by remember { mutableStateOf(MadEngine.getApiKey(context)) }
    var saved by remember { mutableStateOf(MadEngine.hasApiKey(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAD Settings", color = RemsGold, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RemsNavyCard)
            )
        },
        containerColor = RemsNavyDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Gemini API Key",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("AIza... paste karo", color = Color(0xFF9FB8D9)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RemsGold,
                    unfocusedBorderColor = Color(0xFF44557A),
                    cursorColor = RemsGold
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Button(
                onClick = {
                    MadEngine.saveApiKey(context, key)
                    saved = MadEngine.hasApiKey(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF0D1B2A))
                Spacer(Modifier.width(6.dp))
                Text("Save Key", color = Color(0xFF0D1B2A), fontWeight = FontWeight.Bold)
            }

            if (saved) {
                Text(
                    "✅ Key saved — MAD ab live hai!",
                    color = Color(0xFF66FF99),
                    fontSize = 14.sp
                )
            }

            Surface(color = RemsNavyCard, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Key kahan se milegi?",
                        color = RemsGoogleGoldish(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "1. aistudio.google.com/app/apikey kholo\n" +
                        "2. 'Create API key' dabao\n" +
                        "3. Key copy karke upar paste karo aur Save karo\n\n" +
                        "Key free hai (Gemini ke free tier me).",
                        color = Color(0xFFC9D7EE),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RemsGoogleGoldish() = RemsGold
