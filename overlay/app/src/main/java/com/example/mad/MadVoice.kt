package com.example.mad

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * MAD ki awaaz: SpeechRecognizer (Google voice typing engine) + TextToSpeech.
 * Original app ki kisi file ko touch nahi karta — naya standalone module hai.
 */
class MadVoice(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null

    var onHeard: ((String) -> Unit)? = null
    var onListeningStateChanged: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                tts?.language = Locale("hi", "IN")
            }
        }
    }

    fun isRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) return
        stopListening()

        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onListeningStateChanged?.invoke(true)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    onListeningStateChanged?.invoke(false)
                }

                override fun onError(error: Int) {
                    onListeningStateChanged?.invoke(false)
                    // 7 = no-match, 6 = speech-timeout — chuppi normal hai, ignore
                }

                override fun onResults(results: Bundle?) {
                    onListeningStateChanged?.invoke(false)
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = list?.firstOrNull()?.trim() ?: ""
                    if (text.isNotEmpty()) onHeard?.invoke(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }
            startListening(intent)
        }
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!ttsReady) {
            onDone?.invoke()
            return
        }
        stopListening()
        val utterance = "mad_${System.currentTimeMillis()}"
        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        if (onDone != null) {
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {
                    if (utteranceId == utterance) onDone.invoke()
                }
                @Deprecated("Deprecated in Java")
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == utterance) onDone.invoke()
                }
            })
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utterance)
    }

    fun stopSpeaking() {
        try { tts?.stop() } catch (_: Exception) {}
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
    }
}
