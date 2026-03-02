package com.kori.terminal.data.secure

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64

data class AppConfig(
    val koriBaseUrl: String,
    val keycloakTokenUrl: String,
    val clientId: String,
    val clientSecret: String
) {
    fun isValid(): Boolean =
        koriBaseUrl.isNotBlank() &&
                keycloakTokenUrl.isNotBlank() &&
                clientId.isNotBlank() &&
                clientSecret.isNotBlank()
}

private val Context.dataStore by preferencesDataStore(name = "lipa_terminal_datastore")

class SecureSettingsStore(private val context: Context) {

    private val aead: Aead by lazy { buildAead(context) }

    fun configFlow(): Flow<AppConfig?> {
        return context.dataStore.data.map { prefs ->
            val blob = prefs[KEY_CONFIG_BLOB] ?: return@map null
            decryptConfig(blob)
        }
    }

    suspend fun saveConfig(config: AppConfig) {
        require(config.isValid()) { "Config invalide" }
        val blob = encryptConfig(config)
        context.dataStore.edit { prefs ->
            prefs[KEY_CONFIG_BLOB] = blob
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_CONFIG_BLOB)
        }
    }

    private fun encryptConfig(config: AppConfig): String {
        val json = JSONObject().apply {
            put("koriBaseUrl", config.koriBaseUrl.trim())
            put("keycloakTokenUrl", config.keycloakTokenUrl.trim())
            put("clientId", config.clientId.trim())
            put("clientSecret", config.clientSecret)
        }.toString()

        val ciphertext = aead.encrypt(
            json.toByteArray(StandardCharsets.UTF_8),
            ASSOCIATED_DATA
        )

        return Base64.getEncoder().encodeToString(ciphertext)
    }

    private fun decryptConfig(blob: String): AppConfig? {
        return try {
            val ciphertext = Base64.getDecoder().decode(blob)
            val plaintext = aead.decrypt(ciphertext, ASSOCIATED_DATA)
            val json = JSONObject(String(plaintext, StandardCharsets.UTF_8))

            val cfg = AppConfig(
                koriBaseUrl = json.optString("koriBaseUrl", ""),
                keycloakTokenUrl = json.optString("keycloakTokenUrl", ""),
                clientId = json.optString("clientId", ""),
                clientSecret = json.optString("clientSecret", "")
            )

            if (cfg.isValid()) cfg else null
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val KEY_CONFIG_BLOB = stringPreferencesKey("config_blob")

        // “Associated data” : empêche de réutiliser le blob dans un autre contexte
        private val ASSOCIATED_DATA =
            "lipa-terminal-config-v1".toByteArray(StandardCharsets.UTF_8)

        private const val KEYSET_PREFS_NAME = "lipa_terminal_tink_keyset_prefs"
        private const val KEYSET_NAME = "lipa_terminal_tink_keyset"
        private const val MASTER_KEY_URI =
            "android-keystore://lipa_terminal_master_key"

        private fun buildAead(context: Context): Aead {
            // Init Tink
            AeadConfig.register()

            val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_NAME)
                .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle

            return keysetHandle.getPrimitive(Aead::class.java)
        }
    }
}