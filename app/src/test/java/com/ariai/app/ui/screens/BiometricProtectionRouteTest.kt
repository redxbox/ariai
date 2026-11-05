package com.ariai.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricProtectionRouteTest {
    @Test
    fun sensitiveCredentialRoutesRequireAuthentication() {
        listOf(
            "providers",
            "mcp",
            "search_service",
            "add_provider",
            "add_provider?preset=GEMINI",
            "edit_provider/provider-1"
        ).forEach { assertTrue("Expected protected route: $it", isSensitiveSettingsRoute(it)) }
    }

    @Test
    fun ordinaryApplicationRoutesRemainAccessible() {
        listOf("chat/chat-1", "settings", "preferences", "storage", "workspace").forEach {
            assertFalse("Expected unprotected route: $it", isSensitiveSettingsRoute(it))
        }
    }
}
