package com.shexa.baseproject.helpers

import android.net.wifi.WifiConfiguration
import kotlin.Throws
import java.io.Serializable

class WifiNetwork(
    val ssid: String,
    val authType: WifiAuthType,
    val key: String,
    val isHidden: Boolean
) : Serializable {
    val isPasswordProtected: Boolean
        get() = authType === WifiAuthType.WPA_PSK || authType === WifiAuthType.WPA2_PSK || authType === WifiAuthType.WEP || !key.isEmpty()

    fun needsPassword(): Boolean {
        return isPasswordProtected && key.isEmpty()
    }

    override fun toString(): String {
        return "WifiNetwork{" +
                ", ssid='" + ssid + '\'' +
                ", key='" + key + '\'' +
                ", authType=" + authType +
                ", isHidden=" + isHidden +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as WifiNetwork
        return isHidden == that.isHidden &&
                ssid == that.ssid &&
                key == that.key && authType === that.authType
    }

    companion object {
        fun fromWifiConfiguration(wifiConfiguration: WifiConfiguration): WifiNetwork {
            val SSID = getSsidFromWifiConfiguration(wifiConfiguration)
            val authType = getSecurityFromWifiConfiguration(wifiConfiguration)
            val key = ""
            val isHidden = wifiConfiguration.hiddenSSID
            return WifiNetwork(SSID, authType, key, isHidden)
        }

        private fun getSsidFromWifiConfiguration(wifiConfiguration: WifiConfiguration): String {
            val SSID = wifiConfiguration.SSID
            return if (SSID != null) {
                if (SSID.startsWith("\"") && SSID.endsWith("\"")) {
                    SSID.substring(1, SSID.length - 1)
                } else {
                    SSID
                }
            } else ""
        }

        private fun getSecurityFromWifiConfiguration(wifiConfiguration: WifiConfiguration): WifiAuthType {
            if (wifiConfiguration.allowedKeyManagement[WifiConfiguration.KeyMgmt.WPA_PSK]) {
                return if (wifiConfiguration.allowedProtocols[WifiConfiguration.Protocol.RSN]) {
                    WifiAuthType.WPA2_PSK
                } else { // WifiConfiguration.Protocol.WPA
                    WifiAuthType.WPA_PSK
                }
            }
            if (wifiConfiguration.allowedKeyManagement[WifiConfiguration.KeyMgmt.WPA_EAP] ||
                wifiConfiguration.allowedKeyManagement[WifiConfiguration.KeyMgmt.IEEE8021X]
            ) {
                return if (wifiConfiguration.allowedProtocols[WifiConfiguration.Protocol.RSN]) {
                    WifiAuthType.WPA2_EAP
                } else  // WifiConfiguration.Protocol.WPA
                {
                    WifiAuthType.WPA_EAP
                }
            }
            return if (wifiConfiguration.wepKeys[0] != null) WifiAuthType.WEP else WifiAuthType.OPEN
        }

        @Throws(WifiException::class)
        fun isValidKeyLength(authType: WifiAuthType, key: String): Boolean {
            val keyLength = key.length
            if (authType === WifiAuthType.WEP) {
                if (keyLength != 5 && keyLength != 13) {
                    throw WifiException(WifiException.WEP_KEY_LENGTH_ERROR)
                }
            } else { // WPA
                if (keyLength >= 5 && keyLength < 8 || keyLength > 63) { // TODO: support hex key (64)
                    throw WifiException(WifiException.WPA_KEY_LENGTH_ERROR)
                }
            }
            return true
        }
    }
}