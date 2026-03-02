package com.kori.terminal.data.auth

import com.kori.terminal.data.secure.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
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
        try {
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
                    // Keycloak renvoie souvent: {"error":"unauthorized_client","error_description":"..."}
                    val msg = buildString {
                        append("Keycloak HTTP ").append(response.code)
                        val kcMsg = extractKeycloakError(rawBody)
                        if (kcMsg.isNotBlank()) append(" — ").append(kcMsg)
                    }
                    return@withContext Result.failure(Exception(msg))
                }

                val json = runCatching { JSONObject(rawBody) }.getOrNull()
                    ?: return@withContext Result.failure(Exception("Réponse token invalide (JSON)"))

                val accessToken = json.optString("access_token", "")
                if (accessToken.isBlank()) {
                    val kcMsg = extractKeycloakError(rawBody)
                    return@withContext Result.failure(Exception("Pas de access_token. $kcMsg".trim()))
                }

                return@withContext validateJwt(accessToken)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Auth exception: ${e.message}", e))
        }
    }

    private fun validateJwt(token: String): Result<AuthResult> {
        val parts = token.split(".")
        if (parts.size != 3) return Result.failure(Exception("JWT invalide (format)"))

        val payloadJson = try {
            val payloadBytes = base64UrlDecode(parts[1])
            JSONObject(String(payloadBytes, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            return Result.failure(Exception("JWT invalide (payload decode)"))
        }

        // 1) actorRef
        val actorRef = payloadJson.optString("actorRef", "")
        if (actorRef.isBlank()) {
            return Result.failure(Exception("JWT: claim actorRef manquant"))
        }

        // 2) aud contient "kori-api"
        val audOk = audienceContains(payloadJson, "kori-api")
        if (!audOk) {
            return Result.failure(Exception("JWT: audience 'kori-api' absente"))
        }

        // 3) rôle TERMINAL sur le client kori-api
        val hasTerminalRole = hasClientRole(payloadJson, clientId = "kori-api", role = "TERMINAL")
        if (!hasTerminalRole) {
            return Result.failure(Exception("JWT: rôle client 'TERMINAL' absent sur 'kori-api'"))
        }

        return Result.success(AuthResult(accessToken = token, actorRef = actorRef))
    }

    private fun audienceContains(payload: JSONObject, expected: String): Boolean {
        val aud = payload.opt("aud")
        return when (aud) {
            is String -> aud == expected
            is JSONArray -> (0 until aud.length()).any { aud.optString(it) == expected }
            else -> false
        }
    }

    private fun hasClientRole(payload: JSONObject, clientId: String, role: String): Boolean {
        val resourceAccess = payload.optJSONObject("resource_access") ?: return false
        val clientObj = resourceAccess.optJSONObject(clientId) ?: return false
        val roles = clientObj.optJSONArray("roles") ?: return false
        return (0 until roles.length()).any { roles.optString(it) == role }
    }

    private fun base64UrlDecode(s: String): ByteArray {
        // Gère le padding manquant du JWT
        var str = s.replace('-', '+').replace('_', '/')
        val pad = str.length % 4
        if (pad != 0) str += "=".repeat(4 - pad)
        return Base64.getDecoder().decode(str)
    }

    private fun extractKeycloakError(rawBody: String): String {
        val json = runCatching { JSONObject(rawBody) }.getOrNull() ?: return ""
        val err = json.optString("error", "")
        val desc = json.optString("error_description", "")
        return listOf(err, desc).filter { it.isNotBlank() }.joinToString(" — ")
    }
}