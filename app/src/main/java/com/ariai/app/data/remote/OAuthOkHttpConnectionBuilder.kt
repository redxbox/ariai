package com.ariai.app.data.remote

import android.content.Context
import android.net.Uri
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationService
import net.openid.appauth.connectivity.ConnectionBuilder
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Routes AppAuth token exchange and refresh requests through OkHttp while AppAuth handles the
 * browser Custom Tab and PendingIntent callback lifecycle.
 */
internal object OAuthAuthorizationServices {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            // Never forward authorization codes or refresh tokens to a redirect target.
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .build()
    }

    private val connectionBuilder: ConnectionBuilder by lazy {
        object : ConnectionBuilder {
            override fun openConnection(uri: Uri): HttpURLConnection {
                if (!uri.scheme.equals("https", ignoreCase = true) ||
                    uri.host.isNullOrBlank() || uri.userInfo != null ||
                    uri.query != null || uri.fragment != null
                ) {
                    throw IOException("OAuth token endpoints must be credential-free HTTPS URLs.")
                }
                val url = try {
                    URL(uri.toString())
                } catch (failure: Exception) {
                    throw IOException("The OAuth token endpoint is invalid.", failure)
                }
                return OAuthOkHttpHttpURLConnection(url, httpClient)
            }
        }
    }

    private val appAuthConfiguration: AppAuthConfiguration by lazy {
        AppAuthConfiguration.Builder()
            .setConnectionBuilder(connectionBuilder)
            .build()
    }

    fun create(context: Context): AuthorizationService = AuthorizationService(context, appAuthConfiguration)
}
