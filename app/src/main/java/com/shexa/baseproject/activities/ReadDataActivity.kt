package com.shexa.baseproject.activities

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import android.content.IntentFilter
import android.app.PendingIntent
import android.nfc.NfcAdapter
import android.os.Bundle
import com.shexa.baseproject.R
import android.text.method.ScrollingMovementMethod
import android.widget.Toast
import android.content.Intent
import android.content.IntentFilter.MalformedMimeTypeException
import android.net.Uri
import android.nfc.tech.Ndef
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.shexa.baseproject.helpers.UriPrefix
import com.shexa.baseproject.helpers.NfcUtils
import android.nfc.Tag
import android.util.Log
import android.view.View
import java.lang.IllegalArgumentException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_16
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import kotlin.experimental.and

class ReadDataActivity : AppCompatActivity()
{

    var tvData: AppCompatTextView? = null
    var tvType: AppCompatTextView? = null
    var tvStatus: AppCompatTextView? = null
    var tvSize: AppCompatTextView? = null
    var tvCanMakeReadOnly: AppCompatTextView? = null
    var tvIsProtected: AppCompatTextView? = null
    var edtData: AppCompatTextView? = null
    var readFilters: Array<IntentFilter>?=null
    private var pendingIntent: PendingIntent? = null
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_data)
        tvData = findViewById(R.id.tv_data)
        tvType = findViewById(R.id.tv_tag_type)
        tvSize = findViewById(R.id.tv_tag_size)
        tvStatus = findViewById(R.id.tv_status)
        tvCanMakeReadOnly = findViewById(R.id.tv_read_only)
        tvIsProtected = findViewById(R.id.tv_protected_pwd)
        edtData = findViewById(R.id.edt_data)

        tvData?.movementMethod = ScrollingMovementMethod()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is Not Supported", Toast.LENGTH_SHORT).show()
            finish()
        }
        createPendingIntent()

        // for reading
        // readTag(getIntent()); // handle the tag that might be coming in to start up
        // the activity and pass intent that comes into.
    }

    private fun createPendingIntent() {
        try {
            // setup foreground dispatch mode

            // now if our activity is running and at that time if the tag comes into the contact
            // then android should not show disambiguation dialog to select the app , it should
            // give current app preference to handle it so ,
            val intent = Intent(this, javaClass) // getClass()[java]/javaClass[kotlin] => passing "this" means we are making this
            // explicit intent to come back to the same activity.
            // now we don't want that it should create new instance
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // if the activity is on the top it just reuse the same intent and
            // call the onNewIntent instead of oncreate

            // pending intent to say that we want to launch activity using this intent
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            // this is required when we set up dispatch so will create as global
            // intent-filter is to pass in foreground dispatch here's the tag that are looking for.

            // for URI
//            IntentFilter uriFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
//            // will add the scheme and host in this filter
//            uriFilter.addDataScheme("https");
//            uriFilter.addDataScheme("http");
//            uriFilter.addDataAuthority("*", null);
//                                             // if we pass null then it will use default port
//            //for text
//            IntentFilter textFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED, "text/plain");


            // inorder to use this intent filters , create array to combine and will pass when we enable foreground dispatch , so it will get that intent when it is discovered.
//            readFilters = new IntentFilter[]{uriFilter, textFilter};
            val filter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
            filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
            filter.addDataType("*")
            readFilters = arrayOf(filter)

            // now data is set up to enable the foreground read
        } catch (e: MalformedMimeTypeException) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //Log.e("Read", "onNewIntent- intent :" + intent + " action : " + intent.getAction());
        readTag(intent)
    }

    private fun enableRead()
    {
        NfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(this, pendingIntent, readFilters, null)
        // tech-list allows us say which type of tags we want to accept even if they are not
        // in NDEF format , so will get action tech discovered intent
        /**
         * enableForegroundDispatch => this enables foreground reading means that if the
         * activity is running in foreground then android will
         * not show disambiguation dialog to choose the app
         * it just give the preference to the current app
         * to read it and it also helps to prevent of creating
         * new instance everytime.
         */

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
        val detectedTag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val ndef = Ndef.get(detectedTag) ?: return
        // tag info
//        tvData.setText(ndef.getType());
//        tvData.setText(ndef.getMaxSize());
//        tvData.setText(ndef.isWritable()?"True":"False");
//        tvData.setText(ndef.isConnected()?"connected":"not connected");
//          tvData.setText(String.valueOf(detectedTag.getTechList()));
        tvSize!!.append(ndef.maxSize.toString())
        tvStatus!!.append(ndef.isWritable.toString())
        tvType!!.append(ndef.type.toString())
        tvCanMakeReadOnly!!.append(ndef.canMakeReadOnly().toString())
        tvIsProtected!!.append((!ndef.isWritable).toString())

        // we need to ask that intent for the list of NDEF messages that are encoded on it.
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        tvData!!.text = "Data : \n" // clear the previous message
        Log.e("current", "readTag: $messages")
        if (messages != null) {
            for (message in messages) {
                // get the message
                val ndefMessage = message as NdefMessage
                // get the records out of the message
                for (record in ndefMessage.records) {
                    Log.e("typeTNF", "readTag: " + record.tnf)
                    when (record.tnf) {
                        NdefRecord.TNF_WELL_KNOWN -> {
                            // some common types like URI and text
                            tvData!!.append("\nWELL-KNOWN \n")
                            // now we actually check what type of record inside there so gonna get byte array that
                            // uniquely identifies the record type of well-known record.
                            if (Arrays.equals(record.type, NdefRecord.RTD_TEXT)) {
                                tvData!!.append(" TEXT : ")
                                val payload = record.payload
                                var textEncoding: Charset?
                                if (payload[0] and 128.toByte() == 0.toByte()) {
                                    textEncoding = UTF_8
                                } else {
                                    textEncoding = UTF_16 // Get the Text Encoding
                                }
                                val languageCodeLength =
                                    (payload[0] and 51) as Int // Get the Language Code, e.g. "en"
                                tvData!!.append(
                                    String(
                                        payload,
                                        languageCodeLength + 1,
                                        payload.size - languageCodeLength - 1,
                                        textEncoding!!
                                    )
                                )

                                // with language code and all
                                //tvData.append(new String(record.getPayload())); // record.getPayload() => gives byte array so converted into string , payload is nothing but a data which is written
                                //tvData.append("\n");
                            } else if (Arrays.equals(record.type, NdefRecord.RTD_URI)) {
                                edtData!!.visibility = View.VISIBLE
                                val payload = record.payload
                                val prefix = UriPrefix.URI_PREFIX_MAP[payload[0]]
                                val prefixBytes = prefix!!.toByteArray(Charset.forName("UTF-8"))
                                val fullUri = ByteArray(prefixBytes.size + payload.size - 1)
                                System.arraycopy(prefixBytes, 0, fullUri, 0, prefixBytes.size)
                                System.arraycopy(
                                    payload,
                                    1,
                                    fullUri,
                                    prefixBytes.size,
                                    payload.size - 1
                                )
                                val uri = Uri.parse(String(fullUri, Charset.forName("UTF-8")))
                                tvData!!.append("URI :")
                                Log.e("HEY", "readTag: $uri")
                                //tvData.append(new String(record.getPayload(),Charset.forName("UTF-8")));
                                edtData!!.text = uri.toString()
                                //Linkify.addLinks(tvData,Linkify.ALL);
                                tvData!!.append("\n")
                                edtData!!.setOnClickListener {
                                    val intent1 = Intent(Intent.ACTION_VIEW, uri)
                                    startActivity(intent1)
                                }
                            }
                        }
                        NdefRecord.TNF_EXTERNAL_TYPE -> {
                            //tvData.append(new String(record.getType()));
                            tvData!!.append("\nEXTERNAL_TYPE \n")
                            tvData!!.append(String(record.payload))
                        }

                        NdefRecord.TNF_MIME_MEDIA -> {
                            tvData!!.append("\nMIME_MEDIA \n")
                            if (String(record.type) == NfcUtils.NFC_TOKEN_MIME_TYPE) {
                                Log.e("DATA", "readTag: " + record.payload)

                                //tvData.append(new String(record.getPayload(),Charset.forName("US-ASCII")));
                                val wifiConfiguration = NfcUtils.readTag(detectedTag)
                                tvData!!.text = """network name :${wifiConfiguration?.SSID} 
networkId :${wifiConfiguration?.networkId}
password :${wifiConfiguration?.preSharedKey}"""
                                // "\nmacaddress :" + wifiConfiguration.getRandomizedMacAddress());

//
                            } else {        // for contact info
                                tvData!!.append(String(record.payload, Charset.forName("US-ASCII")))
                            }
                        }
                        else -> Toast.makeText(this, "Invalid Type", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    companion object {
        fun parse(record: NdefRecord): Uri {
            val tnf = record.tnf
            if (tnf == NdefRecord.TNF_WELL_KNOWN) {
                return parseAbsolute(record)
            } else if (tnf == NdefRecord.TNF_ABSOLUTE_URI) {
                return parseAbsolute(record)
            }
            throw IllegalArgumentException("Unknown TNF $tnf")
        }

        private fun parseAbsolute(ndefRecord: NdefRecord): Uri {
            // get all byte data
            val payload = ndefRecord.payload
            return Uri.parse(String(payload, Charset.forName("UTF-8")))
        }
    }
}