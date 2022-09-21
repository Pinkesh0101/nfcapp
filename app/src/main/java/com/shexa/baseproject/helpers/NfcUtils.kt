package com.shexa.baseproject.helpers

import android.nfc.NdefMessage
import android.nfc.tech.Ndef
import android.nfc.NdefRecord
import android.nfc.tech.NdefFormatable
import android.net.wifi.WifiConfiguration
import android.nfc.Tag
import android.util.Log
import com.shexa.baseproject.BuildConfig
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

object NfcUtils
{
    private val TAG = NfcUtils::class.java.simpleName
    private const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
    const val NFC_TOKEN_MIME_TYPE = "application/vnd.wfa.wsc"

    /*
     * ID into configuration record for SSID and Network Key in hex.
     * Obtained from WFA Wi-Fi Simple Configuration Technical Specification v2.0.5.
     */
    const val CREDENTIAL_FIELD_ID: Short = 0x100e
    const val NETWORK_INDEX_FIELD_ID: Short = 0x1026
    const val NETWORK_INDEX_DEFAULT_VALUE = 0x01.toByte()
    const val SSID_FIELD_ID: Short = 0x1045
    const val AUTH_TYPE_FIELD_ID: Short = 0x1003
    const val AUTH_TYPE_EXPECTED_SIZE: Short = 2
    const val AUTH_TYPE_OPEN: Short = 0x0001
    const val AUTH_TYPE_WPA_PSK: Short = 0x0002
    const val AUTH_TYPE_WPA_EAP: Short = 0x0008
    const val AUTH_TYPE_WPA2_EAP: Short = 0x0010
    const val AUTH_TYPE_WPA2_PSK: Short = 0x0020
    const val ENC_TYPE_FIELD_ID: Short = 0x100f
    const val ENC_TYPE_NONE: Short = 0x0001
    const val ENC_TYPE_WEP: Short = 0x0002 // deprecated
    const val ENC_TYPE_TKIP: Short = 0x0004 // deprecated -> only with mixed mode (0x000c)
    const val ENC_TYPE_AES: Short = 0x0008 // includes CCMP and GCMP
    const val ENC_TYPE_AES_TKIP: Short = 0x000c // mixed mode
    const val NETWORK_KEY_FIELD_ID: Short = 0x1027

    // WPA2-personal (passphrase): 8-63 ASCII characters
    // WPA2-personal: 64 hex characters
    const val MAC_ADDRESS_FIELD_ID: Short = 0x1020
    const val MAX_SSID_SIZE_BYTES = 32
    const val MAX_MAC_ADDRESS_SIZE_BYTES = 6
    const val MAX_NETWORK_KEY_SIZE_BYTES = 64

    /**
     * Write the given Wi-Fi configuration to the NFC tag.
     *
     * The tag is NDEF-formatted with the application/vnd.wfa.wsc MIME type
     *
     * @param wifiNetwork the Wi-Fi configuration to write to the tag
     * @param tag the tag to write
     * @return true if the configuration has successfully been written to the tag, false otherwise
     */
    fun isWrittenSuccessfully(wifiNetwork: WifiNetwork, tag: Tag): Boolean {
        return writeTag(generateNdefMessage(wifiNetwork), tag)
    }

    /**
     * Write the given NDEF message to the NFC tag.
     *
     * The NDEF message should contain two NDEF records: the actual Wi-Fi configuration and an
     * AAR (Android Application Record) with the application package id.
     * If the message size is greater than the maximum writable size, the AAR is removed from the
     * NDEF message. If it is still too big, the message is not written.
     *
     * @param message the NDEF message to write
     * @param tag the NFC tag
     * @return true if the NDEF message was successfully written to the tag, false otherwise
     */
    private fun writeTag(message: NdefMessage, tag: Tag): Boolean {
        val messageSize = message.toByteArray().size
        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    Log.w(TAG, "Tag not writable")
                    return false
                }
                val ndefMaxSize = ndef.maxSize
                if (messageSize > ndefMaxSize) {
                    /* Try to write the NDEF message without the Android Application Record */
                    val newMessage = NdefMessage(arrayOf(message.records[0]))
                    val newMessageSize = newMessage.toByteArray().size
                    return if (newMessageSize > ndefMaxSize) {
                        Log.w(TAG, "Tag too small")
                        false
                    } else {
                        Log.d(TAG, "Writing tag without AAR")
                        ndef.writeNdefMessage(newMessage)
                        true
                    }
                }
                ndef.writeNdefMessage(message)
                true
            } else {
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    try {
                        ndefFormatable.connect()
                        ndefFormatable.format(message) // FIXME: try without AAR if message too big
                        true
                    } catch (e: IOException) {
                        Log.w(TAG, "Tag not formatted")
                        false
                    }
                } else {
                    Log.d(TAG, "ndefFormatable is null")
                    false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Writing to tag failed", e)
            false
        }
    }

    /**
     * Generate an NDEF message containing the given Wi-Fi configuration
     *
     * @param wifiNetwork the Wi-Fi configuration to convert
     * @return an NDEF message containing the given Wi-Fi configuration
     */
    fun generateNdefMessage(wifiNetwork: WifiNetwork): NdefMessage {
        val payload = generateNdefPayload(wifiNetwork)
        val mimeRecord = NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            NFC_TOKEN_MIME_TYPE.toByteArray(Charset.forName("US-ASCII")), ByteArray(0),
            payload
        )
        val aarRecord = NdefRecord.createApplicationRecord(PACKAGE_NAME)
        return NdefMessage(arrayOf(mimeRecord, aarRecord))
    }

    private fun generateNdefPayload(wifiNetwork: WifiNetwork): ByteArray {
        val ssid = wifiNetwork.ssid
        val ssidSize = ssid.toByteArray().size.toShort()
        val authType: Short = when (wifiNetwork.authType)
        {
            WifiAuthType.WPA_PSK -> AUTH_TYPE_WPA_PSK
            WifiAuthType.WPA2_PSK -> AUTH_TYPE_WPA2_PSK
            WifiAuthType.WPA_EAP -> AUTH_TYPE_WPA_EAP
            WifiAuthType.WPA2_EAP -> AUTH_TYPE_WPA2_EAP
            else -> AUTH_TYPE_OPEN
        }

        /*short encType;
        switch (wifiNetwork.getEncType()) {
            case WEP:
                encType = ENC_TYPE_WEP;
                break;
            case TKIP:
                encType = ENC_TYPE_TKIP;
                break;
            case AES:
                encType = ENC_TYPE_AES;
                break;
            case AES_TKIP:
                encType = ENC_TYPE_AES_TKIP;
                break;
            default:
                encType = ENC_TYPE_NONE;
                break;
        }*/
        val networkKey = wifiNetwork.key
        val networkKeySize = networkKey.toByteArray().size.toShort()
        val macAddress = ByteArray(MAX_MAC_ADDRESS_SIZE_BYTES)
        for (i in 0 until MAX_MAC_ADDRESS_SIZE_BYTES) {
            macAddress[i] = 0xff.toByte()
        }

        /* Fill buffer */
        val bufferSize = 18 + ssidSize + networkKeySize // size of required credential attributes
        val buffer = ByteBuffer.allocate(bufferSize)
        buffer.putShort(CREDENTIAL_FIELD_ID)
        buffer.putShort((bufferSize - 4).toShort())

//        buffer.putShort(NETWORK_INDEX_FIELD_ID);
//        buffer.putShort((short) 1);
//        buffer.put(NETWORK_INDEX_DEFAULT_VALUE);
        buffer.putShort(SSID_FIELD_ID)
        buffer.putShort(ssidSize)
        buffer.put(ssid.toByteArray())
        buffer.putShort(AUTH_TYPE_FIELD_ID)
        buffer.putShort(2.toShort())
        buffer.putShort(authType)

//        buffer.putShort(ENC_TYPE_FIELD_ID);
//        buffer.putShort((short) 2);
//        buffer.putShort(ENC_TYPE_NONE); // FIXME
        buffer.putShort(NETWORK_KEY_FIELD_ID)
        buffer.putShort(networkKeySize)
        buffer.put(networkKey.toByteArray())

//        buffer.putShort(MAC_ADDRESS_FIELD_ID);
//        buffer.putShort((short) MAX_MAC_ADDRESS_SIZE_BYTES);
//        buffer.put(macAddress);
        return buffer.array()
    }

    fun readTag(tag: Tag?): WifiConfiguration? {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            Log.d(TAG, "NDEF not supported")
            return null
        }
        val ndefMessage = ndef.cachedNdefMessage
        if (ndefMessage == null) {
            Log.d(TAG, "ndefMessage is null")
            return null
        }
        return parse(ndefMessage)
    }

    /**
     * Parse an NDEF message and return the corresponding Wi-Fi configuration
     *
     * Source: http://androidxref.com/6.0.1_r10/xref/packages/apps/Nfc/src/com/android/nfc/NfcWifiProtectedSetup.java
     *
     * @param message the NDEF message to parse
     * @return a WifiConfiguration extracted from the NDEF message
     */
    fun parse(message: NdefMessage): WifiConfiguration? {
        val records = message.records
        for (record in records) {
            if (String(record.type) == NFC_TOKEN_MIME_TYPE) {
                val payload = ByteBuffer.wrap(record.payload)
                while (payload.hasRemaining()) {
                    val fieldId = payload.short
                    val fieldSize = payload.short
                    if (fieldId == CREDENTIAL_FIELD_ID) {
                        Log.e(TAG, "parse => CREDENTIAL_FIELD_ID $fieldId")
                        return parseCredential(payload, fieldSize)
                    } else if (fieldId == AUTH_TYPE_FIELD_ID) {
                        Log.e(TAG, "parse => AUTH_TYPE_FIELD_ID $fieldId")
                        return parseCredential(payload, fieldSize)
                    } else if (fieldId == SSID_FIELD_ID) {
                        Log.e(TAG, "parse => SSID_FIELD_ID $fieldId")
                        return parseCredential(payload, fieldSize)
                    } else {
                        payload.position(payload.position() + fieldSize)
                    }
                }
            }
        }
        return null
    }

    fun parseCredential(payload: ByteBuffer, size: Short): WifiConfiguration? {
        val startPosition = payload.position()
        val result = WifiConfiguration()
        while (payload.position() < startPosition + size) {
            val fieldId = payload.short
            val fieldSize = payload.short
            // sanity check
            if (payload.position() + fieldSize > startPosition + size) {
                return null
            }
            when (fieldId) {
                SSID_FIELD_ID -> {
                    val ssid = ByteArray(fieldSize.toInt())
                    payload[ssid]
                    result.SSID = "\"" + String(ssid) + "\""
                }
                NETWORK_KEY_FIELD_ID -> {
                    if (fieldSize > MAX_NETWORK_KEY_SIZE_BYTES) {
                        return null
                    }
                    val networkKey = ByteArray(fieldSize.toInt())
                    payload[networkKey]
                    result.preSharedKey = "\"" + String(networkKey) + "\""
                }
                AUTH_TYPE_FIELD_ID -> {
                    if (fieldSize != AUTH_TYPE_EXPECTED_SIZE) {
                        // corrupt data
                        return null
                    }
                    val authType = payload.short
                    populateAllowedKeyManagement(result.allowedKeyManagement, authType)
                }
                else ->                     // unknown / unparsed tag
                    payload.position(payload.position() + fieldSize)
            }
        }
        if (result.allowedKeyManagement[WifiConfiguration.KeyMgmt.NONE]) {
            result.preSharedKey = null
        }
        return result
    }

    private fun populateAllowedKeyManagement(allowedKeyManagement: BitSet, authType: Short) {
        if (authType == AUTH_TYPE_WPA_PSK || authType == AUTH_TYPE_WPA2_PSK) {
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        } else if (authType == AUTH_TYPE_WPA_EAP || authType == AUTH_TYPE_WPA2_EAP) {
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP)
        } else if (authType == AUTH_TYPE_OPEN) {
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        }
    }
}