package com.example.mad

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * MAD live-chat engine — naya feature, original REMS app ki kisi file ko touch nahi karta.
 * Gemini REST API use karta hai (OkHttp app me pehle se maujood hai).
 * API key: pehle MAD settings (SharedPreferences) se, warna BuildConfig.GEMINI_API_KEY se.
 */
object MadEngine {

    private const val MODEL = "gemini-2.0-flash"
    private const val PREFS = "mad_assistant_prefs"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT =
        "Tum 'MAD' ho — Sushil Sheokand ka personal voice assistant (REMS Business app ke andar). " +
        "Tum natural Hindi, Haryanvi aur mixed Hindi-English me baat karti ho. " +
        "Jawaab HAMESHA chhote, bolne layak aur natural rakho — 2-4 lines se zyada nahi, " +
        "kyunki jawaab awaaz me bola jata hai. Emoji aur markdown mat use karo. " +
        "Sushil ko 'jugaadu' ya dost ki tarah address kar sakti ho. " +
        "Kaam ki baat ho to seedha point pe aao aur poochho jo poochna zaroori ho."

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getApiKey(context: Context): String {
        val saved = prefs(context).getString("gemini_api_key", "")?.trim() ?: ""
        if (saved.isNotBlank()) return saved
        // Secrets Gradle Plugin BuildConfig.GEMINI_API_KEY (agar .env me hai)
        return try {
            val field = Class.forName("com.example.BuildConfig").getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun saveApiKey(context: Context, key: String) {
        prefs(context).edit().putString("gemini_api_key", key.trim()).apply()
    }

    fun hasApiKey(context: Context): Boolean = getApiKey(context).isNotBlank()

    /**
     * Gemini se live reply. history = list of ("user"/"model", text).
     */
    suspend fun chat(
        context: Context,
        userText: String,
        history: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        val key = getApiKey(context)
        if (key.isBlank()) {
            return@withContext "Gemini API key nahi mili. MAD ke Settings me jaakar apni key paste karo — aistudio.google.com/app/apikey se free milti hai."
        }

        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$key"

        val contents = JSONArray()
        for ((role, text) in history.takeLast(12)) {
            contents.put(
                JSONObject()
                    .put("role", if (role == "user") "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", text)))
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userText)))
        )

        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
            )
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("maxOutputTokens", 800)
                    .put("temperature", 0.7)
            )

        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string() ?: "{}"
                if (!response.isSuccessful) {
                    val msg = try {
                        JSONObject(raw).getJSONObject("error").getString("message")
                    } catch (_: Exception) {
                        "HTTP ${response.code}"
                    }
                    return@withContext "Gemini error: $msg"
                }
                val json = JSONObject(raw)
                val parts = json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    sb.append(parts.getJSONObject(i).optString("text"))
                }
                sb.toString().trim().ifBlank { "..." }
            }
        } catch (e: Exception) {
            "Network problem: ${e.message ?: "internet check karo"}"
        }
    }
}
