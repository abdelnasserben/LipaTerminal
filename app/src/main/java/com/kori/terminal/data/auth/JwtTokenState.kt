package com.kori.terminal.data.auth

import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64

object JwtTokenState {

    fun expirationEpochSeconds(token: String): Long? {
        val parts = token.split('.')
        if (parts.size != 3) return null

        val payload = runCatching {
            val payloadBytes = base64UrlDecode(parts[1])
            JSONObject(String(payloadBytes, StandardCharsets.UTF_8))
        }.getOrNull() ?: return null

        return payload.optLong("exp", 0L).takeIf { it > 0L }
    }

    fun isNonExpired(token: String, nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val exp = expirationEpochSeconds(token) ?: return false
        return exp > nowEpochSeconds
    }

    private fun base64UrlDecode(s: String): ByteArray {
        var str = s.replace('-', '+').replace('_', '/')
        val pad = str.length % 4
        if (pad != 0) str += "=".repeat(4 - pad)
        return Base64.getDecoder().decode(str)
    }
}
