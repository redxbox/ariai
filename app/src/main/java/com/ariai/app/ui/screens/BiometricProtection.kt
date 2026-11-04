package com.ariai.app.ui.screens

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ariai.app.ui.theme.AriAccent
import com.ariai.app.ui.theme.AriInk
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.ui.theme.AriPaper
import com.ariai.app.util.tx
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

internal fun biometricAuthenticatorTypes(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
} else {
    BiometricManager.Authenticators.BIOMETRIC_WEAK
}

internal suspend fun authenticateSensitiveSettings(activity: FragmentActivity): Boolean {
    val authenticators = biometricAuthenticatorTypes()
    if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) return false

    return suspendCancellableCoroutine { continuation ->
        val finished = AtomicBoolean(false)
        lateinit var prompt: BiometricPrompt
        fun finish(result: Boolean) {
            if (finished.compareAndSet(false, true) && continuation.isActive) continuation.resume(result)
        }
        prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = finish(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = finish(false)
                override fun onAuthenticationFailed() = Unit
            }
        )
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        try {
            val infoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(tx("Unlock protected settings"))
                .setSubtitle(tx("Authenticate to access API keys and MCP settings"))
                .setAllowedAuthenticators(authenticators)
            if ((authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 0) {
                infoBuilder.setNegativeButtonText(tx("Cancel"))
            }
            prompt.authenticate(infoBuilder.build())
        } catch (_: Exception) {
            finish(false)
        }
    }
}

@Composable
internal fun BiometricLockOverlay(
    error: String?,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier.fillMaxSize().background(AriPaper).testTag("biometric-lock-overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AriAccent, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(tx("Protected settings are locked"), color = AriInk, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                Spacer(Modifier.height(7.dp))
                Text(
                    error ?: tx("Authenticate to view API keys and MCP settings."),
                    color = AriMuted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(18.dp))
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AriAccent)) {
                    Text(tx("Authenticate"), color = Color.White)
                }
                TextButton(onClick = onCancel) { Text(tx("Go back"), color = AriAccent) }
            }
        }
    }
}
