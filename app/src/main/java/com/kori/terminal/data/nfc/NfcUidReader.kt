package com.kori.terminal.data.nfc

import android.app.Activity
import android.nfc.NfcAdapter

class NfcUidReader(
    private val activity: Activity
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun isAvailable(): Boolean = adapter != null

    fun enable(onUid: (String) -> Unit, onError: (String) -> Unit) {
        val a = adapter ?: run {
            onError("NFC indisponible sur cet appareil")
            return
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        a.enableReaderMode(activity, { tag ->
            val uid = tag.id.toHexString()
            onUid(uid)
        }, flags, null)
    }

    fun disable() {
        adapter?.disableReaderMode(activity)
    }
}

private fun ByteArray.toHexString(): String =
    joinToString(separator = "") { b -> "%02X".format(b) }