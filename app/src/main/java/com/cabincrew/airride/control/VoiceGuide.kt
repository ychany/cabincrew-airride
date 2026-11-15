package com.cabincrew.airride.control

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/** Short driving voice prompts via Android TextToSpeech (Gleo AI TTS 연동 전 대체). */
class VoiceGuide(context: Context) {

    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.KOREAN
        else Log.w(TAG, "TTS unavailable ($status)")
    }

    fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "airride")
    }

    fun release() {
        runCatching { tts.shutdown() }
    }

    private companion object {
        const val TAG = "VoiceGuide"
    }
}
