package com.shexa.baseproject.activities

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import android.content.IntentFilter
import android.app.PendingIntent
import android.os.Bundle
import com.shexa.baseproject.R
import android.widget.Toast
import android.content.Intent
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.*
import android.util.Log
import android.view.View
import com.shexa.baseproject.entities.WriteHistoryModel
import com.shexa.baseproject.helpers.AppDataBase
import com.shexa.baseproject.helpers.NfcUtils
import com.shexa.baseproject.helpers.WifiNetwork
import com.shexa.baseproject.helpers.WifiAuthType
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.Exception
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.thread

class WriteDataActivity : AppCompatActivity()
{

    var edtInput: AppCompatEditText? = null
    var btnWriteText: AppCompatButton? = null
    var btnWriteUri: AppCompatButton? = null
    var btnContactInfo: AppCompatButton? = null
    var tvData: AppCompatTextView? = null
    private var nfcAdapter: NfcAdapter? = null
    var msgToWrite: NdefMessage? = null
    var writeFilters: Array<IntentFilter>?=null
    private var pendingIntent: PendingIntent? = null
    var writeTechList: Array<Array<String>>?=null
    var recordType :String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_data)
        edtInput = findViewById(R.id.edt_input)
        btnWriteText = findViewById(R.id.btn_write_txt)
        btnWriteUri = findViewById(R.id.btn_write_uri)
        btnContactInfo = findViewById(R.id.btn_write_contact_info)
        tvData = findViewById(R.id.tv_data)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        // If no NfcAdapter, display that the device has no NFC
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_SHORT).show()
            finish()
        }

        // We will then need to enable our PendingIntent to run in the foreground of our activity that we created.
        createPendingIntent()
        processNfcTag(intent)

    }

    private fun createPendingIntent()
    {
        // setup foreground dispatch mode

        // now if our activity is running and at that time if the tag comes into the contact
        // then android should not show disambiguation dialog to select the app , it should
        // give current app preference to handle it so ,
        val intent = Intent(this, javaClass) // getClass() => passing "this" means we are making this
        // explicit intent to come back to the same activity.
        // now we don't want that it should create new instance
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        // if the activity is on the top it just reuse the same intent and
        // call the onNewIntent instead of onCreate()

        //=> Create a PendingIntent object so the Android system can
        //   populate it with the details of the tag when it is scanned.
        //=> pending intent to say that we want to launch activity using this intent
        pendingIntent = PendingIntent.getActivity(this, 0,intent, PendingIntent.FLAG_MUTABLE)
                                                                // requestcode(identifier for intent)
        // this is required when we set up dispatch so will create as global
        // intent-filter is to pass in foreground dispatch here's the tag that are looking for.

        writeFilters = arrayOf() // in this =>will not specify type as will accept any type of tag as long as it is formatted or Formatable with NDEF
        // set up tech list
        writeTechList = arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))
                            // so this allows us to receive either tags that are NDEF formatted already or ones
                            // that are possible to be formatted
    }

    private fun processNfcTag(intent: Intent)
    {
        if (msgToWrite != null) // means message is prepared to write
        {
            writeTag(intent)
        }
    }

    override fun onNewIntent(intent: Intent)
    {
        super.onNewIntent(intent)
        Log.e("TAG", "onNewIntent-intent: " + intent.action)

//      setIntent(intent);
        processNfcTag(intent)
    }

    private fun writeTag(intent: Intent)
    {
        Log.e("TAG", "writeTag: " + intent.type)

        // first will get the tag information off that intent
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            Log.e("TAG", "writeTag: tag detected...")

            // now we can do things with tag , either format or we can write it.
            val ndef = Ndef.get(tag) // will check whether tag is formatted or not by passing tag info
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

                        // writing to db after the data is stored in tag successfully !!!
                        writeDataToDatabase(recordType);

                    } catch (e: IOException) {
                        e.printStackTrace()

                        // if there is some some tag and device issue occurs then need to inform user that place the tag again
                        Toast.makeText(this@WriteDataActivity,"something went wrong , place the tag again",Toast.LENGTH_SHORT).show();

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

                    // writing to db after the data is stored in tag successfully !!!
                    writeDataToDatabase(recordType);

                } catch (e: IOException) {
                    e.printStackTrace()

                    // if there is some some tag and device issue occurs then need to inform user that place the tag again
                    Toast.makeText(this@WriteDataActivity,"something went wrong , place the tag again",Toast.LENGTH_SHORT).show();

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

    private fun enableWrite()
    {
        NfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(this, pendingIntent, writeFilters, writeTechList)
    }

    // set up the text record
    fun onWriteText(view: View?)
    {
        // on which language we want to write , so will get default language
        val language: ByteArray = Locale.getDefault().language.toByteArray(StandardCharsets.UTF_8)
        // will get input from the edit text which we want to write
        val input: ByteArray = edtInput!!.text.toString().toByteArray(StandardCharsets.UTF_8)

        // to put this both array together we need more array called => payload
        val payload: ByteArray = ByteArray(input.size + language.size + 1)
        // +1(we need one more byte) is to represent encoding of payload

        // to fill in this payload first set up encoding byte
        payload[0] = 0x02 // 0x02 represents encoding UTF-8
        // now copy both array into payload so,
        System.arraycopy(language, 0, payload, 1, language.size)
        // from language array ,starting bit of language,into payload array,payload array ma kyanthi position[1 as 0the encoding stored chhe],upto which from source array [or how much bytes we want to copy]
        System.arraycopy(input, 0, payload, language.size + 1, input.size)

        // now we create NDEF record for that
        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
        // tnf that we want,record type definition(RTD),id of record[to find it uniquely] bt we don't need it so new byte[0] , payload array

        // now create message to wrap that record
        msgToWrite = NdefMessage(arrayOf(record)) // global field as these different methods set up message to write after they tap the tag that's going to writing.
        // we need to pass array of record

        // now we should give user indication that they should be tapping a tag to write
        tvData?.text = "Tap a tag to write text data"

        // when user tap a tag , tag could have any type of message on it , it could have been
        // NDEF formatted tag that could already have URI , simple text may be it was not formatted
        // yet but it still was NDEF formattable so what we want to do when we enable our
        // foreground dispatch to tell that we can accept any kind of tag that can be either that
        // is NDEF formatted or formattable , so we create write filters inside onCreate

        // now we have data so will make it enable to write
        enableWrite()
        recordType = "text"
    }

    // set up the uri record
    fun onWriteUri(view: View?) {

//         NdefRecord record = NdefRecord.createUri("https://"+edtInput.getText().toString());
        val record = NdefRecord.createUri(edtInput?.text.toString())
        msgToWrite = NdefMessage(arrayOf(record)) // global field as these different methods set up message to write after they tap the tag that's going to writing.
        tvData?.text = "Tap a tag to write uri data"
        enableWrite()
    }

    //    public void onWriteText(View view)
    //    {
    //        NdefRecord record = NdefRecord.createTextRecord(Locale.getDefault().getLanguage(),edtInput.getText().toString());
    //        msgToWrite = new NdefMessage(new NdefRecord[]{record});  // global field as these different methods set up message to write after they tap the tag that's going to writing.
    //        tvData.setText("Tap a tag to write text data");
    //        enableWrite();
    //    }

    fun onWriteContactInfo(view: View?) {
        try {
            val records = arrayOfNulls<NdefRecord>(1)
            records[0] = createVcardRecord("Henry", "Henry's Company", "0412345678", "henry@domain.com")
            msgToWrite = NdefMessage(records)
            enableWrite()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }

    // => Using the VCARD type.
    // => vCard, also known as VCF, is a file format standard for electronic business card.
    fun createVcardRecord(name: String, org: String, tel: String, email: String): NdefRecord
    {
        val payloadStr = """
               BEGIN:VCARD
               VERSION:2.1
               N:;$name
               ORG:$org
               TEL:$tel
               EMAIL:$email
               END:VCARD
               """.trimIndent()
        val uriField = payloadStr.toByteArray(Charset.forName("US-ASCII"))
        val payload = ByteArray(uriField.size + 1)
        System.arraycopy(uriField, 0, payload, 1, uriField.size)
        return NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            "text/vcard".toByteArray(), ByteArray(0),
            payload
        )
    }

    // phone number
    fun onWritePhoneNumber(view: View?) {
        val record = NdefRecord.createUri("tel:" + edtInput!!.text.toString())
        msgToWrite = NdefMessage(arrayOf(record)) // global field as these different methods set up message to write after they tap the tag that's going to writing.
        tvData?.text = "Tap a tag to write uri data"
        enableWrite()
    }

    // social media , for now only instagram,facebook page
    fun onWriteSocialMediaPage(view: View?) {
        // 1)
        // NdefRecord record = NdefRecord.createUri("https://www.instagram.com/"+edtInput.getText().toString());
        /// 2)
        val record = NdefRecord.createUri("https://www.facebook.com/" + edtInput!!.text.toString())
        msgToWrite = NdefMessage(arrayOf(record)) // global field as these different methods set up message to write after they tap the tag that's going to writing.
        tvData!!.text = "Tap a tag to write social media page"
        enableWrite()
    }

    fun onWriteEmail(view: View?)
    {
        // 1)=> to write only email
        // NdefRecord.createUri("mailto:" + "abc@gmail.com");

        // 2)=> write if you want to write only receiver email id into NFC tag
        // NdefRecord mimeRecord = NdefRecord.createUri("mailto:" + "abc@gmail.com" + "?body=" + "your email message");

        // 3)=> write if you want to write receiver email id and message into NFC tag
        val emailUri = NdefRecord.createUri("mailto:" + "abc@gmail.com" + "?subject=" + "your email subject" + "&body=" + "your email message")
        msgToWrite = NdefMessage(arrayOf(emailUri))
        tvData?.text = "Tap a tag to write Email"
        enableWrite()
    }

    fun onWriteLaunchApp(view: View?)
    {

        installedAppsPackageName
        val records = arrayOf(NdefRecord.createApplicationRecord(edtInput?.text.toString()))
        msgToWrite = NdefMessage(records)
        tvData?.text = "Tap a tag to launch App"
        enableWrite()

        // note => if the written package name app is not installed to the scanned device it will
        //         go to the play store and lead to that app or else open the installed app.
    }//            if((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
//            {
//                String appName = info.applicationInfo.loadLabel(getPackageManager()).toString();
//                Log.e("AppName", appName);
//            }

    // Extra => open app from the package name
    // Intent intent = getPackageManager().getLaunchIntentForPackage(editText.getText().toString());
    // startActivity(intent);
    // get installed applications package name
    val installedAppsPackageName: Unit
        get() {
            // get installed applications package name
            val packageManager = packageManager
            val packages = packageManager.getInstalledPackages(0)
            for (info in packages) {
                Log.e("packages", "getInstalledAppsPackageName: ${info.packageName}")

                //            if((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
//            {
//                String appName = info.applicationInfo.loadLabel(getPackageManager()).toString();
//                Log.e("AppName", appName);
//            }

                // Extra => open app from the package name
                // Intent intent = getPackageManager().getLaunchIntentForPackage(editText.getText().toString());
                // startActivity(intent);
            }
        }

    fun onWriteSendSMS(view: View?) {
        val phone_number = "7874585018"
        val text_message = "Hello , How are you ?"
        val smsUri = "sms:$phone_number?body=$text_message"
        val smsUriRecord = arrayOf(NdefRecord.createUri(smsUri))
        msgToWrite = NdefMessage(smsUriRecord)
        tvData?.text = "Tap a tag to send SMS"
        enableWrite()
    }

    fun onWriteLocation(view: View?) {
        val latitude = "28.398332438781445"
        val longitude = "76.79447361888094"
        val geoUri = "geo:$latitude,$longitude"
        val geoUriRecord = NdefRecord.createUri(geoUri)
        msgToWrite = NdefMessage(geoUriRecord)
        tvData?.text = "Tap a tag to write geo location"
        enableWrite()
    }

    fun onWriteAddress(view: View?) {
        // String address = "shukan cross road,nikol";
        val address = "sardar patel stadium"
        val addressUri = "geo:?q=$address"
        val addressUriRecord = NdefRecord.createUri(addressUri)
        msgToWrite = NdefMessage(addressUriRecord)
        tvData?.text = "Tap a tag to write address location"
        enableWrite()
    }

    fun onWriteWifiConfiguration(view: View?) {
        msgToWrite = NfcUtils.generateNdefMessage(
            WifiNetwork(
                "pinkesh",
                WifiAuthType.WPA2_PSK,
                "123123123",
                false))
        tvData?.text = "Tap a tag to write wifi data"
        enableWrite()
    }

    fun onWriteBluetoothConfiguration(view: View?) {

//        String macAddress = "E4:OC:FD:DC:CE:C6";  // E4:OC:FD:DC:CE:C6
//        String[] macAddressParts = macAddress.split(":");
//
//        // convert hex string to byte values
//        Byte[] macAddressBytes = new Byte[6];
//        for(int i=0; i<6; i++){
//            Integer hex = Integer.parseInt(macAddressParts[i], 16);
//            macAddressBytes[i] = hex.byteValue();
//        }
        val bluetoothRecord = NdefRecord.createMime(
            "application/vnd.bluetooth.ep.oob", "E4:OC:FD:DC:CE:C6".toByteArray(
                StandardCharsets.UTF_8
            )
        )
        msgToWrite = NdefMessage(bluetoothRecord)
        tvData?.text = "Tap a tag to write bluetooth data"
        enableWrite()
    }

    private fun writeDataToDatabase(recordType: String?)
    {
        Log.e("WriteTDB", "writeDataToDatabase: writeDataToDatabase")
        when(recordType)
        {
            "text" ->{
                // saving to db
                Log.e("WriteTDB", "writeDataToDatabase: inside when text")
                val writeModel = WriteHistoryModel(null,edtInput?.text.toString(),"text")
                writeToDb(writeModel)
            }
        }
    }

    private fun writeToDb(writeModel: WriteHistoryModel)
    {
        val dbInstance = AppDataBase.getInstance(this);
        try {

            Log.e("WriteTDB", "writeDataToDatabase: writeToDb" )
            thread(true)
            {
                dbInstance.WriteHistoryDao().insertWriteRecord(writeModel)
            }
        }
        catch (e : Exception)
        {
            e.printStackTrace()
        }

    }

}