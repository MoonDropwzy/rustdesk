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

    // 注意：此变量需要保持 FlutterEngine 的实例
    private var channel: MethodChannel? = null

    override fun onReceive(context: Context, intent: Intent) {
        // 如果没有 channel，就创建一个
        if (channel == null) {
            val flutterEngine = FlutterEngine(context)
            channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
        }

        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            val pdus = bundle.get("pdus") as Array<*>
            for (i in pdus.indices) {
                val message = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                val sender = message.displayOriginatingAddress
                val content = message.messageBody

                // 通过 MethodChannel 将数据发送到 Flutter
                sendToFlutter(sender, content)
            }
        }
    }

    private fun sendToFlutter(sender: String, content: String) {
        channel?.invokeMethod("receiveSms", mapOf("sender" to sender, "content" to content))
    }
}
