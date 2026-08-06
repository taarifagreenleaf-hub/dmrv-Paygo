package com.greenleaf.paygo.whatsapp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Sends a WhatsApp message through the official Meta WhatsApp Cloud API.
 * Requires a phone-number id and a bearer token from a Meta Business account.
 * This is the reliable, ToS-compliant path; the on-device intent/accessibility
 * paths are provided for users who don't have a Business API set up.
 */
class WhatsAppCloudApi(
    private val token: String,
    private val phoneNumberId: String,
    private val client: OkHttpClient = defaultClient()
) {

    fun sendText(toNumber: String, text: String): Result<Unit> = runCatching {
        val url = "https://graph.facebook.com/v20.0/$phoneNumberId/messages"
        val payload = JSONObject().apply {
            put("messaging_product", "whatsapp")
            put("to", toNumber.filter { it.isDigit() })
            put("type", "text")
            put("text", JSONObject().put("preview_url", false).put("body", text))
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                "WhatsApp Cloud API error ${response.code}: ${response.body?.string()}"
            }
        }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
