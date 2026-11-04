package com.ariai.app.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Current UI language code. Compose reads it, so screens recompose on change. */
object UiLang {
    var code by mutableStateOf("en")
}

/** Returns the UI text for the English source string. */
fun tx(en: String): String =
    if (UiLang.code == "fa") faStrings[en] ?: en else en
