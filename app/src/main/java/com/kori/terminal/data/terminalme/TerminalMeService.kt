package com.kori.terminal.data.terminalme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class TerminalMeSnapshot(
    val status: Map<String, String>,
    val health: Map<String, String>,
    val config: Map<String, String>
)

class TerminalMeService {

    private val client = OkHttpClient()

    suspend fun loadAll(baseUrl: String, bearerToken: String): Result<TerminalMeSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val status = getMap(cleanBaseUrl, bearerToken, "/api/v1/terminal/me/status")
            val health = getMap(cleanBaseUrl, bearerToken, "/api/v1/terminal/me/health")
            val config = getMap(cleanBaseUrl, bearerToken, "/api/v1/terminal/me/config")
            TerminalMeSnapshot(status = status, health = health, config = config)
        }
    }

    private fun getMap(baseUrl: String, bearerToken: String, path: String): Map<String, String> {
        val request = Request.Builder()
            .url(baseUrl + path)
            .addHeader("Authorization", "Bearer $bearerToken")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("$path HTTP ${response.code}: ${rawBody.take(300)}")
            }

            val json = JSONObject(rawBody)
            return json.keys().asSequence().associateWith { key ->
                json.optString(key, "")
            }
        }
    }
}
