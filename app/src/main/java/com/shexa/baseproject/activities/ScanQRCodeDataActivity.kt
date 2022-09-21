package com.shexa.baseproject.activities

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import android.content.IntentFilter
import android.app.PendingIntent
import android.os.Bundle
import com.shexa.baseproject.R
import com.google.zxing.integration.android.IntentIntegrator
import android.content.Intent
import android.nfc.*
import android.widget.Toast
import android.text.util.Linkify
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import android.view.View
import java.io.IOException
import java.util.*

class ScanQRCodeDataActivity : AppCompatActivity() {
    var tvMsgText: AppCompatTextView? = null
    var tvMsgFormat: AppCompatTextView? = null
    var writeFilters: Array<IntentFilter>?=null
    private var pendingIntent: PendingIntent? = null
    lateinit var writeTechList: Array<Array<String>>
    private var msgToWrite: NdefMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qrcode_data)
        tvMsgText = findViewById(R.id.tvMsgText)
        tvMsgFormat = findViewById(R.id.tvMsgFormat)
        createPendingIntent()
        openScanner()
    }

    private fun openScanner() {
        // we need to create the object
        // of IntentIntegrator class
        // which is the class of QR library
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.setPrompt("Scan a barcode or QR Code")
        intentIntegrator.setOrientationLocked(true)
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        intentIntegrator.setCameraId(0) // Use a specific camera of the device
        //        intentIntegrator.setBeepEnabled(true);
//        intentIntegrator.setBarcodeImageEnabled(true);
        intentIntegrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        // if the intentResult is null then
        // toast a message as "cancelled"
        if (intentResult != null) {
            if (intentResult.contents == null) // when we go back without scanning means without getting data.
            {
                Toast.makeText(baseContext, "Cancelled", Toast.LENGTH_SHORT).show()
                // finish() => if user comes back without scanning then it should not show that
                //             QR Code scanning screen.
                finish()
            } else {
                // if the intentResult is not null we'll set
                // the content and format of scan message
                identifyData(intentResult.contents)
                //tvMsgText.setText(intentResult.getContents());
                //tvMsgFormat.setText(intentResult.getFormatName());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun identifyData(data: String) {
        tvMsgText!!.text = data
        //tvMsgText.setText("+919383936275");
        Linkify.addLinks(tvMsgText!!, Linkify.ALL)
    }

    // set up the uri record  => when we scan the qr code will get the data stored inside the
    //                           qr code , if we found phone number then will get data like
    //                           tel:9383837373 , so while storing into NFC tag no need
    //                           check whether it is link,phNo or anything else.QRCode will
    //                           have data and it's type also so , just make URI of the fetched
    //                           data and write into NFC tag.
    fun onWriteScannedData(view: View?) {
        val data = tvMsgText!!.text.toString().lowercase()
        Log.e("TAG", "onWriteScannedData: " + data.contains("http"))
        msgToWrite =
            if (data.contains("tel:") || data.contains("http") || data.contains("mail") || data.contains(
                    "vcard"
                )
                || data.contains("sms") || data.contains("geo:")
            ) {
                val record = NdefRecord.createUri(tvMsgText!!.text.toString())
                NdefMessage(arrayOf(record)) // global field as these different methods set up message to write after they tap the tag that's going to writing.
            } else {
                val record = NdefRecord.createTextRecord(
                    Locale.getDefault().language,
                    tvMsgText!!.text.toString()
                )
                NdefMessage(arrayOf(record)) // global field as these different methods set up message to write after they tap the tag that's going to writing.
            }
        tvMsgFormat!!.text = "Tap a tag to write data"
        enableWrite()
    }

    private fun enableWrite() {
        NfcAdapter.getDefaultAdapter(this)
            .enableForegroundDispatch(this, pendingIntent, writeFilters, writeTechList)
    }

    private fun createPendingIntent() {
        val intent =
            Intent(this, javaClass) // getClass() => passing "this" means we are making this
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        ) // requestcode(identifier for intent)
        val filter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filter.addDataScheme("*")
        filter.addDataAuthority("*", null)
        writeFilters =
            arrayOf() // {} in this =>will not specify type as will accept any type of tag as long as it is formatted or Formatable with NDEF
        // set up tech list
        writeTechList = arrayOf(
            arrayOf(Ndef::class.java.name), arrayOf(
                NdefFormatable::class.java.name
            )
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.e("TAG", "onNewIntent-intent: " + intent.action)
        setIntent(intent)
        writeNfcTag(intent)
    }

    private fun writeNfcTag(intent: Intent) {
        // first will get the tag information off that intent
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            Log.e("TAG", "writeTag: tag detected...")

            // now we can do things with tag , either format or we can write it.
            val ndef =
                Ndef.get(tag) // will check whether tag is formatted or not by passing tag info
            if (ndef == null) // if null then it is not already formatted
            {
                // to format it
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    Log.e("TAG", "writeTag: tag formatable and msgToWrite : $msgToWrite")
                    try {
                        ndefFormatable.connect() // to talk with tag
                        ndefFormatable.format(msgToWrite) // it will format and write new message
                        ndefFormatable.close()
                        Toast.makeText(
                            this,
                            "tag formatted and written successfully !!!",
                            Toast.LENGTH_SHORT
                        ).show()
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
                    Toast.makeText(this, "tag written successfully !!!", Toast.LENGTH_SHORT).show()
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