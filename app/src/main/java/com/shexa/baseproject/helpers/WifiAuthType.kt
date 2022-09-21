package com.shexa.baseproject.helpers

enum class WifiAuthType(private val printableName: String) {
    OPEN("Open"), WEP("WEP"), WPA_PSK("WPA PSK"), WPA_EAP("WPA EAP"), WPA2_EAP("WPA2 EAP"), WPA2_PSK(
        "WPA2 PSK"
    );

    override fun toString(): String {
        return printableName
    }
}