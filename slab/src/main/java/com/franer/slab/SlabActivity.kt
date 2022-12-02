package com.franer.slab

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.zeromq.SocketType

class SlabActivity : AppCompatActivity() {

    private val TAG: String = "DiagnosisSlab"

    private val scopeRep = CoroutineScope(newSingleThreadContext("DiagnosisManager-Rep"))

    //    private val CHASSIS_IP_REQ = "ipc://192.168.0.110:36021"
    private val CHASSIS_IP_REP = "ipc://*:36023"
    // 超时时间
    private val SOCKET_TIMEOUT = 2 * 1000
    // socket名称
    private val SOCKET_NAME = "DiagnosticSlab"
    // 重试间隔
    private val CONNECT_RETRY_INTERVAL = 500L

    // 发送请求socket
    private var repSocket: ZmqSocket = ZmqSocket(CHASSIS_IP_REP, SocketType.REP, SOCKET_TIMEOUT, SOCKET_NAME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slab)

        val textView = findViewById<TextView>(R.id.textView)

        findViewById<Switch>(R.id.switch1).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                scopeRep.launch {
                    while (!repSocket.connect()) {
                        delay(CONNECT_RETRY_INTERVAL)
                        repSocket.disconnect()
                    }
                    Log.i(TAG, "init reqSocket success..")

                    while (true) {
                        val bytes: ByteArray? = repSocket.receive()
                        val str = bytes?.run { String(this) } ?: "空"
                        Log.i(TAG, "收到的消息为:$str")
                        if (str.isBlank()) {
                            runOnUiThread {
                                textView.text = "收到消息内容为:$str"
                            }
                            val str = textView.text.toString() + " okay!!!"
                            repSocket.send(str.toByteArray())
                        }
                    }
                }
            } else {
                scopeRep.launch {
                    repSocket.disconnect()
                }
            }
        }

    }


    override fun onResume() {
        super.onResume()

        findViewById<Button>(R.id.btn).setOnClickListener {
            ZmqTestController.getInstance().init()
        }
    }
}