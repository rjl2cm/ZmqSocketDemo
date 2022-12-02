package com.franer.slab;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.zeromq.SocketType;

import androidx.annotation.NonNull;

@SuppressLint("LongLogTag")
public class ZmqTestController {
    private static final String TAG_PRE = "ZmqTestController";

    private static final String REQ_IP_ADDRESS = "tcp://127.0.0.1:12345";
    private static final String REP_IP_ADDRESS = "tcp://*:12345";
    private static final int SOCKET_TIMEOUT = 5 * 1000;
    private static final int CONNECT_RETRY_INTERVAL = 1000;

    private static ZmqTestController mInstance;
    private final HandlerThread mReqThread;
    private ReqHandler mReqHandler;
    private final RepThread mRepThread;

    //test flags
    private boolean startRep = false;
    private boolean doReply = false;

    private ZmqTestController() {
        mReqThread = new HandlerThread("req");
        mRepThread = new RepThread();
        mRepThread.start();
    }

    public static ZmqTestController getInstance() {
        if (mInstance == null) {
            synchronized (ZmqTestController.class) {
                if (mInstance == null) {
                    mInstance = new ZmqTestController();
                }
            }
        }
        return mInstance;
    }

    public void init() {
        mReqThread.start();
        mReqHandler = new ReqHandler(mReqThread.getLooper());
        mReqHandler.sendEmptyMessage(ReqHandler.MSG_INIT);
    }

    public void setTestFlag(boolean startRep, boolean doReply) {
        Log.i(TAG_PRE, "setTestFlag: startRep = " + startRep + "; doReply = " + doReply);
        this.startRep = startRep;
        this.doReply = doReply;
    }

    private class ReqHandler extends Handler {
        private static final String TAG = TAG_PRE + "ZmqReqHandler";

        private static final int MSG_INIT = 0;
        private static final int MSG_PING = 1;

        private final ZmqSocket mReqSocket;

        public ReqHandler(@NonNull Looper looper) {
            super(looper);
            mReqSocket = new ZmqSocket(REQ_IP_ADDRESS, SocketType.REQ, SOCKET_TIMEOUT,
                    "req");
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
//            Log.i(TAG, "handleMessage: msg = " + msg);
            switch (msg.what) {
                case MSG_INIT: {
                    while (!mReqSocket.connect()) {
                        try {
                            Thread.sleep(CONNECT_RETRY_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.w(TAG, "init mReqSocket failed, try again.");
                        mReqSocket.disconnect();
                    }
                    Log.i(TAG, "init mReqSocket success.");
                    sendEmptyMessage(MSG_PING);
                    break;
                }
                case MSG_PING:{
                    byte[] reply = mReqSocket.sendAndReceive("ping".getBytes());
//                    Log.i(TAG, "receive reply or not:" + reply);
                    if (reply != null) {
                        Log.i(TAG, "receive reply:不为空 " + new String(reply));
                        sendEmptyMessageDelayed(MSG_PING, 1000);
                    } else {
                        //do reInit
                        Log.i(TAG, "receive reply: 为空");
//                        sendEmptyMessageDelayed(MSG_PING, 1000);
                        mReqSocket.disconnect();
                        sendEmptyMessage(MSG_INIT);
                    }
                    break;
                }
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private class RepThread extends Thread {
        private static final String TAG = TAG_PRE + "ZmqRepThread";
        private ZmqSocket mRepSocket;

        @Override
        public void run() {
            Log.i(TAG, "init mRepSocket start.");
            while (!startRep) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mRepSocket = new ZmqSocket(REP_IP_ADDRESS, SocketType.REP, -1,
                    "rep");
            while (!mRepSocket.connect()) {
                try {
                    Thread.sleep(CONNECT_RETRY_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.w(TAG, "init mRepSocket failed, try again.");
                mRepSocket.disconnect();
            }
            Log.i(TAG, "init mRepSocket done.");
            while (true) {
                Log.i(TAG, "Reply thread start receive.");
                byte[] reply = mRepSocket.receive();
//                reply = mRepSocket.receive();
                if (reply != null) {
                    Log.i(TAG, "receive request: " + new String(reply));
                    if (doReply) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mRepSocket.send("pong".getBytes());
                    }
                }
            }
        }
    }
}
