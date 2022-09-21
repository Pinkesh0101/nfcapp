package com.shexa.baseproject.helpers

import com.shexa.baseproject.helpers.UriPrefix
import java.util.HashMap

object UriPrefix {
    val URI_PREFIX_MAP: MutableMap<Byte, String> = HashMap()

    init {
        URI_PREFIX_MAP[0x00.toByte()] = ""
        URI_PREFIX_MAP[0x01.toByte()] = "http://www."
        URI_PREFIX_MAP[0x02.toByte()] = "https://www."
        URI_PREFIX_MAP[0x03.toByte()] = "http://"
        URI_PREFIX_MAP[0x04.toByte()] = "https://"
        URI_PREFIX_MAP[0x05.toByte()] = "tel:"
        URI_PREFIX_MAP[0x06.toByte()] = "mailto:"
        URI_PREFIX_MAP[0x07.toByte()] = "ftp://anonymous:anonymous@"
        URI_PREFIX_MAP[0x08.toByte()] = "ftp://ftp."
        URI_PREFIX_MAP[0x09.toByte()] = "ftps://"
        URI_PREFIX_MAP[0x0A.toByte()] = "sftp://"
        URI_PREFIX_MAP[0x0B.toByte()] = "smb://"
        URI_PREFIX_MAP[0x0C.toByte()] = "nfs://"
        URI_PREFIX_MAP[0x0D.toByte()] = "ftp://"
        URI_PREFIX_MAP[0x0E.toByte()] = "dav://"
        URI_PREFIX_MAP[0x0F.toByte()] = "news:"
        URI_PREFIX_MAP[0x10.toByte()] = "telnet://"
        URI_PREFIX_MAP[0x11.toByte()] = "imap:"
        URI_PREFIX_MAP[0x12.toByte()] = "rtsp://"
        URI_PREFIX_MAP[0x13.toByte()] = "urn:"
        URI_PREFIX_MAP[0x14.toByte()] = "pop:"
        URI_PREFIX_MAP[0x15.toByte()] = "sip:"
        URI_PREFIX_MAP[0x16.toByte()] = "sips:"
        URI_PREFIX_MAP[0x17.toByte()] = "tftp:"
        URI_PREFIX_MAP[0x18.toByte()] = "btspp://"
        URI_PREFIX_MAP[0x19.toByte()] = "btl2cap://"
        URI_PREFIX_MAP[0x1A.toByte()] = "btgoep://"
        URI_PREFIX_MAP[0x1B.toByte()] = "tcpobex://"
        URI_PREFIX_MAP[0x1C.toByte()] = "irdaobex://"
        URI_PREFIX_MAP[0x1D.toByte()] = "file://"
        URI_PREFIX_MAP[0x1E.toByte()] = "urn:epc:id:"
        URI_PREFIX_MAP[0x1F.toByte()] = "urn:epc:tag:"
        URI_PREFIX_MAP[0x20.toByte()] = "urn:epc:pat:"
        URI_PREFIX_MAP[0x21.toByte()] = "urn:epc:raw:"
        URI_PREFIX_MAP[0x22.toByte()] = "urn:epc:"
        URI_PREFIX_MAP[0x23.toByte()] = "urn:nfc:"
    }
}