package com.shexa.baseproject.activities

import androidx.appcompat.app.AppCompatActivity
import android.content.IntentFilter
import android.app.PendingIntent
import androidx.appcompat.widget.AppCompatTextView
import android.os.Bundle
import com.shexa.baseproject.R
import android.content.Intent
import android.content.IntentFilter.MalformedMimeTypeException
import android.nfc.*
import com.shexa.baseproject.activities.ReadDataActivity
import com.shexa.baseproject.activities.WriteDataActivity
import android.widget.Toast
import android.nfc.tech.Ndef
import android.util.Log
import android.view.View
import com.shexa.baseproject.activities.ScanQRCodeDataActivity
import com.shexa.baseproject.activities.CopyTagDataActivity
import java.io.IOException

class MainActivity : AppCompatActivity()
{

    var readFilters: Array<IntentFilter>?=null
    private var pendingIntent: PendingIntent? = null
    var tvIndicator: AppCompatTextView? = null
    var isForErase = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvIndicator = findViewById(R.id.tv_indicator)
        createPendingIntent()
    }

    private fun createPendingIntent() {
        try {
            val intent = Intent(this, javaClass)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            val filter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
            filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
            filter.addDataType("*")
            readFilters = arrayOf(filter)
        } catch (e: MalformedMimeTypeException) {
            e.printStackTrace()
        }
    }

    fun onReadTag(view: View?) {
        val intent = Intent(this, ReadDataActivity::class.java)
        startActivity(intent)
    }

    fun onWriteTag(view: View?) {
        val intent = Intent(this, WriteDataActivity::class.java)
        startActivity(intent)
    }

    fun onEraseTag(view: View?) {
        tvIndicator!!.visibility = View.VISIBLE
        isForErase = true
    }

    override fun onResume() {
        super.onResume()
        NfcAdapter.getDefaultAdapter(this)
            .enableForegroundDispatch(this, pendingIntent, readFilters, null)
    }

    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.e("erase", "onNewIntent: ")
        if (intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) != null) {
            if (isForErase) {
                eraseTag(intent)
            }
        } else {
            Toast.makeText(this, "Tag is already Empty", Toast.LENGTH_SHORT).show()
            tvIndicator!!.visibility = View.GONE
        }
    }

    private fun eraseTag(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            val ndefTag = Ndef.get(tag)
            try {
                ndefTag.connect()
                ndefTag.writeNdefMessage(
                    NdefMessage(
                        NdefRecord(
                            NdefRecord.TNF_EMPTY,
                            null,
                            null,
                            null
                        )
                    )
                )
                ndefTag.close()
                Toast.makeText(this, "Tag formatted successfully !!!", Toast.LENGTH_SHORT).show()
                tvIndicator!!.visibility = View.GONE
                isForErase = false
            } catch (e: FormatException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Tag not found...Try again", Toast.LENGTH_SHORT).show()
        }
    }

    fun onScanQRCode(view: View?) {
        val intent = Intent(this, ScanQRCodeDataActivity::class.java)
        startActivity(intent)
    }

    fun onCopyTag(view: View?) {
        val intent = Intent(this, CopyTagDataActivity::class.java)
        startActivity(intent)
    }

    fun onTagHistory(view: View)
    {
        val intent = Intent(this,TagHistoryActivity::class.java)
        startActivity(intent)
    }
}