package com.carriez.flutter_hbb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class SmsReceiver : BroadcastReceiver() {

    private val channelName = "com.example.zxwy"
    
    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            val pdus = bundle.get("pdus") as Array<*>
            for (i in pdus.indices) {
                val message = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                val sender = message.displayOriginatingAddress
                val content = message.messageBody
                
                // 通过 MethodChannel 将数据发送到 Flutter
                sendToFlutter(context, sender, content)
            }
        }
    }

    private fun sendToFlutter(context: Context, sender: String, content: String) {
        val flutterEngine = FlutterEngine(context)
        flutterEngine.getDartExecutor().executeDartEntrypoint(
            flutterEngine.dartExecutor.getDartEntrypoint(),
            null
        )

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).invokeMethod("receiveSms", mapOf("sender" to sender, "content" to content))
    }
}