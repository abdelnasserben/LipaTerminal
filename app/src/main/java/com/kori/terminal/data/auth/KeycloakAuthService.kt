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
            val requestBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", config.clientId)
                .add("client_secret", config.clientSecret)
                .build()

            val request = Request.Builder()
                .url(config.keycloakTokenUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    throw Exception(extractMessage(rawBody, "Authentication failed."))
                }

                val json = runCatching { JSONObject(rawBody) }.getOrNull()
                    ?: throw Exception("Invalid server response.")

                val accessToken = json.optString("access_token", "")
                if (accessToken.isBlank()) {
                    throw Exception(extractMessage(rawBody, "Authentication failed."))
                }

                val actorRef = extractActorReference(accessToken)
                AuthResult(accessToken = accessToken, actorRef = actorRef)
            }
        }.recoverCatching { error ->
            throw Exception(error.message ?: "Authentication failed.")
        }
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
