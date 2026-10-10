package com.ariai.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Reads text aloud with the device's built-in text-to-speech engine. */
object ReadAloud {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: (() -> Unit)? = null

    fun speak(context: Context, text: String, lang: String) {
        val say = {
            val engine = tts
            if (engine != null) {
                val locale = when (lang) {
                    "fa" -> Locale("fa", "IR")
                    "ar" -> Locale("ar")
                    "tr" -> Locale("tr")
                    "de" -> Locale.GERMAN
                    "fr" -> Locale.FRENCH
                    else -> Locale.US
                }
                val result = engine.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ariai-read-aloud")
            }
        }
        if (tts == null) {
            pending = say
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) pending?.invoke()
                pending = null
            }
        } else if (ready) {
            say()
        } else {
            pending = say
        }
    }

    fun stop() {
        tts?.stop()
    }
}
