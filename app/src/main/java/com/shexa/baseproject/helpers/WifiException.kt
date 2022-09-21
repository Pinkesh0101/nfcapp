package com.shexa.baseproject.helpers

import java.lang.Exception

class WifiException(val errorCode: Int) : Exception() {

    companion object {
        const val WEP_KEY_LENGTH_ERROR = 0x0001
        const val WPA_KEY_LENGTH_ERROR = 0x0002
    }
}