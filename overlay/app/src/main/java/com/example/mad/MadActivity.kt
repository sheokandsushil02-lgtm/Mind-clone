package com.example.mad

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * MAD ki apni Activity — mic permission handle karti hai aur chat screen dikhati hai.
 * Original MainActivity bilkul untouched.
 */
class MadActivity : ComponentActivity() {

    private var showSettings by mutableStateOf(false)

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Bolne ke liye Microphone chahiye — MAD ki chat me bhi type kar sakte ho",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MyApplicationTheme {
                if (showSettings) {
                    MadSettingsScreen(onBack = { showSettings = false })
                } else {
                    MadChatScreen(
                        onBack = { finish() },
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}
