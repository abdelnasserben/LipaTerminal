package com.kori.terminal.data.payments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PayByCardResponse(
    val transactionId: String,
    val merchantCode: String?,
    val cardUid: String,
    val amount: Double,
    val fee: Double?,
    val totalDebited: Double?
)

class PaymentService {

    private val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .retryOnConnectionFailure(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun payByCard(
        baseUrl: String,
        bearerToken: String,
        idempotencyKey: String,
        amount: Double,
        cardUid: String,
        pin: String
    ): Result<PayByCardResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val url = baseUrl.trimEnd('/') + "/api/v1/payments/card"

            val bodyJson = JSONObject().apply {
                put("amount", amount)
                put("cardUid", cardUid)
                put("pin", pin)
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer $bearerToken")
                .addHeader("Idempotency-Key", idempotencyKey)
                .addHeader("Accept", "application/json")
                .addHeader("Connection", "close")
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()

                if (response.code != 201) {
                    throw Exception(extractMessage(rawBody, "Payment failed."))
                }

                val json = runCatching { JSONObject(rawBody) }.getOrNull()
                    ?: throw Exception("Invalid server response.")

                val transactionId = json.optString("transactionId", "")
                if (transactionId.isBlank()) {
                    throw Exception("Invalid payment response.")
                }

                PayByCardResponse(
                    transactionId = transactionId,
                    merchantCode = json.optString("merchantCode", null),
                    cardUid = json.optString("cardUid", cardUid),
                    amount = json.optDouble("amount", amount),
                    fee = if (json.has("fee")) json.optDouble("fee") else null,
                    totalDebited = if (json.has("totalDebited")) json.optDouble("totalDebited") else null
                )
            }
        }.recoverCatching { error ->
            throw Exception(error.message ?: "Payment failed.")
        }
    }

    private fun extractMessage(rawBody: String, fallback: String): String {
        val json = runCatching { JSONObject(rawBody) }.getOrNull() ?: return fallback
        val candidates = listOf("message", "error", "detail")
        return candidates.firstNotNullOfOrNull { key ->
            json.optString(key).takeIf { it.isNotBlank() }
        } ?: fallback
    }
}
