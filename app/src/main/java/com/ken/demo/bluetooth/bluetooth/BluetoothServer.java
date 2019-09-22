package com.ken.demo.bluetooth.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.BlendMode;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ken.demo.bluetooth.constant.Constant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothServer {
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private static final String TAG = "BluetoothServer";

    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;
    private Handler handler;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothServer(Handler handler) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = handler;
        this.state = STATE_NONE;
    }

    public static BluetoothServer getInstance(Handler handler) {
        return new BluetoothServer(handler);
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized void start() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public synchronized void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        Message message = handler.obtainMessage(Constant.BLUETOOTH_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);
    }


    public void sendData(byte[] data) {
        ConnectedThread thread;
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                Message obtain = handler.obtainMessage();
                obtain.what = Constant.BLUETOOTH_SEND_FAILED;
                Bundle bundle = new Bundle();
                bundle.putString(Constant.TOAST, "未连接");
                obtain.setData(bundle);
                handler.sendMessage(obtain);
                return;
            }
            thread = connectedThread;
        }
        thread.write(data);
    }

    public void connectionLost() {
        Message message = handler.obtainMessage(Constant.BLUETOOTH_DISCONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.TOAST, "device connection was lost");
        message.setData(bundle);
        handler.sendMessage(message);
        state = STATE_NONE;
        BluetoothServer.this.start();
    }

    public void connectionFailed() {
        Message message = handler.obtainMessage(Constant.BLUETOOTH_DISCONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.TOAST, "unable to connect device");
        message.setData(bundle);
        handler.sendMessage(message);
        state = STATE_NONE;
        BluetoothServer.this.start();
    }

    public synchronized void stop() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        state = STATE_NONE;
    }

    private class AcceptThread extends Thread {


        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(Constant.SERVER_NAME, Constant.BLUETOOTH_UUID);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: ", e);
            }
            serverSocket = tmp;
            state = STATE_LISTEN;
        }

        @Override
        public void run() {
            super.run();
            setName("AcceptThread");
            BluetoothSocket socket = null;
            while (state != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "run: ", e);
                    break;
                }
                if (socket != null) {
                    synchronized (BluetoothServer.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "run: ", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.d(TAG, "run: accept finish");
        }

        public void cancel() {
            Log.d(TAG, "cancel: accept");
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: ", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(
                        Constant.BLUETOOTH_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            socket = tmp;
            state = STATE_CONNECTING;
        }

        @Override
        public void run() {
            super.run();
            setName("ConnectThread");
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    Log.e(TAG, "run: unable to close socket due to connection failed", ex);
                }
                connectionFailed();
                return;
            }
            synchronized (BluetoothServer.this) {
                connectThread = null;
            }
            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread: ");
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
            state = STATE_CONNECTED;
        }

        @Override
        public void run() {
            super.run();
            Log.d(TAG, "run: begin connectedThread");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (state == STATE_CONNECTED) {
                try {
                    int len;
                    byte[] buffer = new byte[1024];
                    while ((len = inputStream.read(buffer)) != -1) {
                        Log.d(TAG, "run: " + len + " " + inputStream.available());
                        baos.write(buffer, 0, len);
                        Log.d(TAG, "run: " + buffer[len - 1]);
                        if (buffer[len - 1] == (byte) 0x80) {
                            break;
                        }
                    }
                    byte[] bytes1 = baos.toByteArray();
                    baos.reset();
                    SendData data = new SendData(bytes1);
                    handler.obtainMessage(Constant.BLUETOOTH_DATA_RECEIVED, data).sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
                try {
                    baos.close();
                } catch (IOException e) {
                    Log.e(TAG, "close receive buffer : ", e);
                }
            }
        }


        public void write(byte[] data) {
            try {
                outputStream.write(data);
                handler.obtainMessage(Constant.BLUETOOTH_SEND_SUCCESS).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "write failed", e);
                handler.obtainMessage(Constant.BLUETOOTH_SEND_ERROR).sendToTarget();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
