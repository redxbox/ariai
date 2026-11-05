package com.ariai.app.data.remote

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** RFC 7636 PKCE helpers for a native OAuth public client. */
internal object OAuthPkce {
    const val CODE_CHALLENGE_METHOD_S256 = "S256"
    private const val VERIFIER_ENTROPY_BYTES = 32
    private val verifierPattern = Regex("[A-Za-z0-9._~-]{43,128}")

    /** Returns 256 bits of cryptographically secure entropy as an unpadded base64url verifier. */
    fun generateCodeVerifier(random: SecureRandom = SecureRandom()): String {
        val entropy = ByteArray(VERIFIER_ENTROPY_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(entropy)
    }

    /** Creates a separate unpredictable OAuth CSRF state with the same secure alphabet and entropy. */
    fun generateState(): String = generateCodeVerifier()

    /** Computes BASE64URL-ENCODE(SHA256(ASCII(code_verifier))) as required by S256. */
    fun codeChallenge(codeVerifier: String): String {
        require(isValidCodeVerifier(codeVerifier)) { "Invalid PKCE code verifier." }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun isValidCodeVerifier(value: String): Boolean = verifierPattern.matches(value)

    /** State values are generated from the same unpadded base64url alphabet as the verifier. */
    fun isValidState(value: String): Boolean = verifierPattern.matches(value)

    /** Compares the callback's returned OAuth state without an early-exit string comparison. */
    fun matchesState(expected: String, returned: String?): Boolean {
        if (!isValidState(expected) || returned == null || !isValidState(returned)) return false
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.US_ASCII),
            returned.toByteArray(StandardCharsets.US_ASCII)
        )
    }
}
