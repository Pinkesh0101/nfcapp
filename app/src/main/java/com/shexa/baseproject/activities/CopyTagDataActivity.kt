package com.shexa.baseproject.activities

import androidx.appcompat.app.AppCompatActivity
import android.content.IntentFilter
import android.app.PendingIntent
import androidx.appcompat.widget.AppCompatTextView
import android.os.Bundle
import com.shexa.baseproject.R
import android.content.Intent
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.content.IntentFilter.MalformedMimeTypeException
import com.shexa.baseproject.helpers.NfcUtils
import android.nfc.*
import android.util.Log
import android.view.View
import android.widget.Toast
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_16
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import kotlin.experimental.and

class CopyTagDataActivity : AppCompatActivity()
{

    var readFilters: Array<IntentFilter>?=null
    private var pendingIntent: PendingIntent? = null
    var tvData: AppCompatTextView? = null
    private var msgToWrite: NdefMessage? = null
    var writeFilters: Array<IntentFilter>?=null
    lateinit var writeTechList: Array<Array<String>>
    var isApplicationRecord = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_copy_tag_data)
        tvData = findViewById(R.id.tvData)
        createPendingIntent()
    }

    private fun createPendingIntent() {
        try {

            val intent = Intent(this, javaClass) // javaClass => passing "this" means we are making this
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)

            // == filter for reading data from the tag ==
            val filter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
            filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
            filter.addDataType("*")
            readFilters = arrayOf(filter)

            // == write copied data ==
            writeFilters = arrayOf() //  in this =>will not specify type as will accept any type of tag as long as it is formatted or Formatable with NDEF
            writeTechList = arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))
        } catch (e: MalformedMimeTypeException) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent)
    {
        super.onNewIntent(intent)
        if (msgToWrite != null) {
            writeTag(intent)
        } else {
            readTag(intent)
        }
    }

    private fun enableRead() {
        NfcAdapter.getDefaultAdapter(this)
            .enableForegroundDispatch(this, pendingIntent, readFilters, null)
    }

    private fun disableRead() {
        NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()
        enableRead()
    }

    override fun onPause() {
        super.onPause()
        disableRead()
    }

    private fun readTag(intent: Intent) {
        // we need to ask that intent for the list of NDEF messages that are encoded on it.
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (messages != null) {
            for (message in messages) {
                // get the message
                val ndefMessage = message as NdefMessage
                // get the records out of the message
                for (record in ndefMessage.records) {
                    // check the which type of record it is
                    when (record.tnf) {
                        NdefRecord.TNF_WELL_KNOWN ->                             // some common types like URI and text
                            // now we actually check what type of record inside there so gonna get byte array that
                            // uniquely identifies the record type of well-known record.
                            if (Arrays.equals(record.type, NdefRecord.RTD_TEXT)) {
                                val payload = record.payload
                                var textEncoding: Charset?
                                if (payload[0] and 128.toByte() == 0.toByte()) {
                                    textEncoding = UTF_8
                                } else {
                                    textEncoding = UTF_16 // Get the Text Encoding
                                }
                                val languageCodeLength =
                                    (payload[0] and 51) as Int // Get the Language Code, e.g. "en"
                                tvData!!.text = String(
                                    payload,
                                    languageCodeLength + 1,
                                    payload.size - languageCodeLength - 1,
                                    textEncoding!!
                                )

                                // with language code and all
                                //tvData.append(new String(record.getPayload())); // record.getPayload() => gives byte array so converted into string , payload is nothing but a data which is written
                                //tvData.append("\n");
                            } else if (Arrays.equals(record.type, NdefRecord.RTD_URI)) {
                                tvData!!.text = String(record.payload)
                            }
                        NdefRecord.TNF_EXTERNAL_TYPE -> {
                            //tvData.append(new String(record.getType()));
                            isApplicationRecord = true
                            tvData!!.text = String(record.payload)
                        }
                        NdefRecord.TNF_MIME_MEDIA -> if (String(record.type) == NfcUtils.NFC_TOKEN_MIME_TYPE) {
                            // tvData.append(new String(record.getPayload()));
                            val payload = ByteBuffer.wrap(record.payload)
                            while (payload.hasRemaining()) {
                                val fieldId = payload.short
                                val fieldSize = payload.short
                                if (fieldId == NfcUtils.CREDENTIAL_FIELD_ID) {
                                    val result = NfcUtils.parseCredential(payload, fieldSize)
                                    tvData!!.text = result.toString()
                                } else if (fieldId == NfcUtils.SSID_FIELD_ID) {
                                    val result = NfcUtils.parseCredential(payload, fieldSize)
                                    tvData!!.text = result.toString()
                                } else if (fieldId == NfcUtils.AUTH_TYPE_FIELD_ID) {
                                    val result = NfcUtils.parseCredential(payload, fieldSize)
                                    tvData!!.text = result.toString()
                                }
                                //                                    else {
//                                        payload.position(payload.position() + fieldSize);
//                                    }
                            }
                        } else {
                            tvData!!.text = String(record.payload)
                        }
                        else -> Toast.makeText(this, "Invalid Type", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // set up the uri record
    fun onCopyTagData(view: View?) {
        val data = tvData!!.text.toString().lowercase()
        if (data.contains("tel:") || data.contains("http:") || data.contains("mail") || data.contains(
                "vcard"
            )
            || data.contains("sms") || data.contains("geo:")
        ) {
            //Toast.makeText(this, "Tap a tag to copy data", Toast.LENGTH_SHORT).show();
            val record = NdefRecord.createUri(tvData!!.text.toString())
            msgToWrite =
                NdefMessage(arrayOf(record)) // global field as these different methods set up message to write after they tap the tag that's going to writing.
        } else if (isApplicationRecord) {
            val records = arrayOf(
                NdefRecord.createApplicationRecord(
                    tvData!!.text.toString()
                )
            )
            msgToWrite = NdefMessage(records)
            isApplicationRecord = false
        } else {
            val record = NdefRecord.createTextRecord(Locale.getDefault().language, data)
            msgToWrite =
                NdefMessage(arrayOf(record)) // global field as these different methods set up message to write after they tap the tag that's going to writing.
        }
        tvData!!.text = "Tap a tag to Copy Data"
        enableWrite()
    }

    private fun enableWrite() {
        NfcAdapter.getDefaultAdapter(this)
            .enableForegroundDispatch(this, pendingIntent, writeFilters, writeTechList)
    }

    private fun writeTag(intent: Intent) {
        // first will get the tag information off that intent
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {  // means tag information is there

            // now we can do things with tag , either format or we can write it.
            val ndef =
                Ndef.get(tag) // will check whether tag is formatted or not by passing tag info
            if (ndef == null) // if null then it is not already formatted we need to format first
            {
                // to format it we want to try to get an Ndef format-able for it.
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) // if not null then we can format the tag
                {
                    Log.e("TAG", "writeTag: tag formatable and msgToWrite : $msgToWrite")
                    try {
                        ndefFormatable.connect() // to talk with tag
                        ndefFormatable.format(msgToWrite) // it will format and write new message
                        ndefFormatable.close()
                        Toast.makeText(
                            this,
                            "tag formatted and copied successfully !!!",
                            Toast.LENGTH_SHORT
                        ).show()
                        // after writing successfully we finish this activity
                        finish()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: FormatException) {
                        e.printStackTrace()
                    } finally {
                        msgToWrite = null
                    }
                } else {
                    // tag cannot be formatted
                }
            } else  // already formatted just write to it.
            {
                try {
                    ndef.connect()
                    ndef.writeNdefMessage(msgToWrite) // to write message
                    ndef.close()
                    Toast.makeText(this, "tag copied successfully !!!", Toast.LENGTH_SHORT).show()
                    // after writing successfully we finish this activity
                    finish()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: FormatException) {
                    e.printStackTrace()
                } finally {
                    msgToWrite = null // blank out message so we can read it properly
                }
            }
        } else {
            Log.e("TAG", "writeTag: tag not detected...")
        }
    }
}