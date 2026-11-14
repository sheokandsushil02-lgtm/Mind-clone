package com.example.mad

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * MAD ki awaaz: SpeechRecognizer + TextToSpeech.
 * Original app ki kisi file ko touch nahi karta.
 */
class MadVoice(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null

    var onHeard: ((String) -> Unit)? = null
    var onListeningStateChanged: ((Boolean) -> Unit)? =

        fun startListening() {}
