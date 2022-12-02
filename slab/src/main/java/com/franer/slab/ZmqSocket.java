package com.franer.slab;

import android.util.Log;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class ZmqSocket {
    private static final String TAG = "ZmqSocket";
    private static final boolean DBG = false;

    private final String mAddress;
    private final SocketType mSocketType;
    private final int mTimeout;
    private final String mName;
    private final ZMQ.Context mContext;
    private ZMQ.Socket mSocket;

    public ZmqSocket(String address, SocketType socketType, int timeout, String name) {
        mAddress = address;
        if (socketType != SocketType.REP && socketType != SocketType.REQ &&
                socketType != SocketType.PUB && socketType != SocketType.SUB) {
            throw new IllegalStateException("ZmqSocket init with invalid socketType: " +
                    socketType);
        }
        mSocketType = socketType;
        mTimeout = timeout;
        mName = name;
        mContext = ZMQ.context(1);
        Log.i(TAG, "init ZmqSocket: this is " + this);
    }

    public boolean connect() {
        return connect(false);
    }

    public synchronized boolean connect(boolean forceUseConnect) {
        Log.i(TAG, this + " connect");
        mSocket = mContext.socket(mSocketType);
        mSocket.setReceiveTimeOut(mTimeout);
        mSocket.setSendTimeOut(mTimeout);
        boolean result = false;
        try {
            //In REQ-REP mode: REQ = connect; REP = bind.
            //In PUB-SUB mode: forceConnect decide bind/connect.
            if (forceUseConnect || mSocketType == SocketType.REQ) {
                result = mSocket.connect(mAddress);
            } else {
                //REP
                result = mSocket.bind(mAddress);
            }
        } catch (ZMQException zmqException) {
            Log.e(TAG, this + " connect failed: " + Log.getStackTraceString(zmqException));
        } catch (Exception e) {
            Log.e(TAG, this + " connect failed: " + Log.getStackTraceString(e));
        }
        return result;
    }

    public synchronized void disconnect() {
        Log.i(TAG, this + " close");
        if (mSocket != null) {
            //0 linger period, close immediately.
            mSocket.setLinger(0);
            mSocket.close();
            mSocket = null;
        }
    }

    public synchronized boolean isConnected() {
        return mSocket != null;
    }

    /**
     * This method will destroy mContext, and there's no going back.
     */
    public synchronized void close() {
        disconnect();
        mContext.close();
    }

    public synchronized boolean send(byte[] data){
        boolean result = false;
        try {
            if (DBG) Log.d(TAG, this + " socket start send");
            result = mSocket.send(data);
            if (DBG) Log.d(TAG, this + " socket end send");
        } catch (Exception e) {
            Log.e(TAG, this + " send failed:" + Log.getStackTraceString(e));
        }
        return result;
    }

    public synchronized byte[] receive(){
        byte[] reply = null;
        try {
            if (DBG) Log.d(TAG, this + " socket start receive");
            reply = mSocket.recv(0);
            if (DBG) Log.d(TAG, this + " socket end receive");
        } catch (Exception e) {
            Log.e(TAG, this + " receive failed: " + Log.getStackTraceString(e));
        }
        return reply;
    }

    public synchronized byte[] sendAndReceive(byte[] data){
        if (mSocketType != SocketType.REQ) {
            throw new IllegalStateException("only support in req socket: " + mSocketType);
        }

        byte[] reply = null;
        try {
            if (DBG) Log.d(TAG, this + " socket start sendAndReceive");
            mSocket.send(data);
            reply = mSocket.recv(0);
            if (DBG) Log.d(TAG, this + " socket end sendAndReceive");
        } catch (Exception e) {
            Log.e(TAG, this + " sendAndReceive failed: " + Log.getStackTraceString(e));
        }
        return reply;
    }

    @Override
    public String toString() {
        return "ZmqSocket{" +
                "mAddress='" + mAddress + '\'' +
                ", mSocketType=" + mSocketType +
                ", mTimeout=" + mTimeout +
                ", mContext=" + mContext +
                ", mSocket=" + mSocket +
                '}';
    }
}
