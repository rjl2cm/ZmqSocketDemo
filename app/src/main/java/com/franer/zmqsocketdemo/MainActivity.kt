package com.franer.zmqsocketdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.zeromq.SocketType

class MainActivity : AppCompatActivity() {

    private val TAG: String = "DiagnosisManager"

    private val scopeReq = CoroutineScope(newSingleThreadContext("DiagnosisManager-Req"))

//    private val CHASSIS_IP_REQ = "ipc://192.168.0.110:36021"
//    private val CHASSIS_IP_REQ = "ipc://localhost:36023"
//    private val CHASSIS_IP_REQ = "ipc://192.168.0.104:36023"
    // 超时时间
    private val SOCKET_TIMEOUT = 2 * 1000
    // socket名称
    private val SOCKET_NAME = "DiagnosticManager"
    // 重试间隔
    private val CONNECT_RETRY_INTERVAL = 500L

    // 发送请求socket
    private var reqSocket: ZmqSocket? = null


    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val editText = findViewById<EditText>(R.id.ip)

        findViewById<Switch>(R.id.switch1).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                scopeReq.launch {
                    val ip = editText.text.toString()
                    reqSocket = ZmqSocket(ip, SocketType.REQ, SOCKET_TIMEOUT, SOCKET_NAME)

                    while (reqSocket?.connect() != true) {
                        delay(CONNECT_RETRY_INTERVAL)
                        reqSocket?.disconnect()
                    }
                    Log.i(TAG, "init reqSocket success..")
                }
            } else {
                scopeReq.launch {
                    reqSocket?.disconnect()
                }
            }
        }

        val textView = findViewById<TextView>(R.id.textView)

        findViewById<Button>(R.id.send_and_receive).setOnClickListener {
            val str: String = textView.text.toString() + count++
            scopeReq.launch {
                val receive = reqSocket?.sendAndReceive(str.toByteArray())
                val receiveStr = receive?.run { String(receive) } ?: "空"

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "收到返回的消息:$receiveStr", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}