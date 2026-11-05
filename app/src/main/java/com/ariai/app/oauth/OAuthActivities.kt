package com.ariai.app.oauth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.ariai.app.AriAiApp
import com.ariai.app.MainActivity
import com.ariai.app.data.remote.PendingOAuthAuthorization
import com.ariai.app.util.tx
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import java.io.IOException

/** AppAuth sends the verified custom-scheme redirect result to this non-exported completion activity. */
class OAuthCompletionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID).orEmpty()
        lifecycleScope.launch {
            val app = application as AriAiApp
            var message = tx("OAuth authorization failed. Please try again.")
            try {
                require(transactionId.isNotBlank()) { "Missing OAuth transaction." }
                val raw = app.preferencesManager.getPendingOAuthAuthorization(transactionId)
                    ?: throw IOException(tx("The OAuth sign-in expired. Try again."))
                val pending = PendingOAuthAuthorization.fromJson(raw, transactionId)
                    ?: throw IOException(tx("The OAuth sign-in expired. Try again."))
                AuthorizationException.fromIntent(intent)?.let { exception ->
                    throw IOException(tx("OAuth authorization failed. Please try again."), exception)
                }
                val response = AuthorizationResponse.fromIntent(intent)
                    ?: throw IOException(tx("The OAuth redirect did not match the pending sign-in."))
                app.oauthSessionManager.completeAuthorization(pending, response)
                message = tx("OAuth connected successfully.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                message = failure.message?.takeIf(String::isNotBlank)
                    ?: tx("OAuth authorization failed. Please try again.")
            } finally {
                if (transactionId.isNotBlank()) {
                    runCatching { app.preferencesManager.removePendingOAuthAuthorization(transactionId) }
                }
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this@OAuthCompletionActivity, message, Toast.LENGTH_LONG).show()
                    returnToApp()
                }
            }
        }
    }

    private fun returnToApp() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "com.ariai.app.oauth.TRANSACTION_ID"
    }
}

/** Removes a pending PKCE verifier if the user cancels in the browser. */
class OAuthCanceledActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID).orEmpty()
        lifecycleScope.launch {
            val app = application as AriAiApp
            if (transactionId.isNotBlank()) {
                runCatching { app.preferencesManager.removePendingOAuthAuthorization(transactionId) }
            }
            Toast.makeText(this@OAuthCanceledActivity, tx("OAuth sign-in was cancelled."), Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(this@OAuthCanceledActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            finish()
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "com.ariai.app.oauth.CANCELED_TRANSACTION_ID"
    }
}
