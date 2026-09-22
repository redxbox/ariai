package com.ariai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.ariai.app.ui.AriAiApp
import com.ariai.app.ui.AriAiTheme
import com.ariai.app.ui.AriAiViewModel

class MainActivity : ComponentActivity() {
    private val vm: AriAiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AriAiTheme(vm.colorMode) {
                AriAiApp(vm)
            }
        }
    }
}
