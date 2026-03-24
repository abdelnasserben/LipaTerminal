package com.kori.terminal.data.auth

import com.kori.terminal.data.secure.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64

data class AuthResult(
    val accessToken: String,
    val actorRef: String
)

class KeycloakAuthService {

    private val client = OkHttpClient()

    suspend fun authenticate(config: AppConfig): Result<AuthResult> = withContext(Dispatchers.IO) {
        runCatching {
            val tokenEndpointCandidates = buildTokenEndpointCandidates(config.keycloakTokenUrl)
            var lastErrorMessage = "Authentication failed."

            for (endpoint in tokenEndpointCandidates) {
                val attempt = requestToken(endpoint, config)
                if (attempt.isSuccess) {
                    return@runCatching attempt.getOrThrow()
                }
                lastErrorMessage = attempt.exceptionOrNull()?.message ?: lastErrorMessage
            }

            throw Exception(lastErrorMessage)
        }.recoverCatching { error ->
            throw Exception(error.message ?: "Authentication failed.")
        }
    }

    private fun requestToken(tokenUrl: String, config: AppConfig): Result<AuthResult> {
        return runCatching {
            val requestBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", config.clientId)
                .add("client_secret", config.clientSecret)
                .build()

            val request = Request.Builder()
                .url(tokenUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body.string()

                if (!response.isSuccessful) {
                    throw Exception(extractMessage(rawBody, "Authentication failed (${response.code})."))
                }

                val json = runCatching { JSONObject(rawBody) }.getOrNull()
                    ?: throw Exception("Invalid server response from $tokenUrl.")

                val accessToken = json.optString("access_token", "")
                if (accessToken.isBlank()) {
                    throw Exception(extractMessage(rawBody, "Authentication failed."))
                }

                val actorRef = extractActorReference(accessToken)
                AuthResult(accessToken = accessToken, actorRef = actorRef)
            }
        }
    }

    private fun buildTokenEndpointCandidates(rawUrl: String): List<String> {
        val base = rawUrl.trim().trimEnd('/')
        if (base.isBlank()) return emptyList()

        val candidates = linkedSetOf<String>()
        candidates += base

        if (!base.endsWith("/token")) {
            candidates += "$base/token"
        }

        if (!base.endsWith("/token") && !base.contains("/protocol/openid-connect/token")) {
            if (base.contains("/realms/")) {
                candidates += "$base/protocol/openid-connect/token"
            } else {
                candidates += "$base/realms/kori/protocol/openid-connect/token"
            }
        }

        return candidates.toList()
    }

    private fun extractActorReference(token: String): String {
        val parts = token.split('.')
        if (parts.size != 3) throw Exception("Invalid session token.")

        val payload = runCatching {
            val payloadBytes = base64UrlDecode(parts[1])
            JSONObject(String(payloadBytes, StandardCharsets.UTF_8))
        }.getOrNull() ?: throw Exception("Invalid session token.")

        return payload.optString("actorRef", "").ifBlank { "-" }
    }

    private fun base64UrlDecode(s: String): ByteArray {
        var str = s.replace('-', '+').replace('_', '/')
        val pad = str.length % 4
        if (pad != 0) str += "=".repeat(4 - pad)
        return Base64.getDecoder().decode(str)
    }

    private fun extractMessage(rawBody: String, fallback: String): String {
        val json = runCatching { JSONObject(rawBody) }.getOrNull() ?: return fallback
        val candidates = listOf(
            "message",
            "error_description",
            "error",
            "detail"
        )
        return candidates.firstNotNullOfOrNull { key ->
            json.optString(key).takeIf { it.isNotBlank() }
        } ?: fallback
    }
}
