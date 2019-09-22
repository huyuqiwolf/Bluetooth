package com.ken.demo.bluetooth.bluetooth;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SendData {
    private static final String TAG = "SendData";
    private byte type;
    private byte[] data;

    public SendData(byte type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public SendData(byte[] bytes) {
        if (bytes != null && bytes.length != 0) {
            this.type = bytes[0];
            this.data = new byte[bytes.length - 2]; // 去除类型和结束标记
            System.arraycopy(bytes, 1, this.data, 0, this.data.length);
            Log.d(TAG, "SendData: " + Arrays.toString(data));
            Log.d(TAG, "SendData: " + Arrays.toString(bytes));
        } else {
            Log.d(TAG, "SendData: empty data");
        }
    }

    public byte getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }


    public byte[] getBytes() {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            baos.write(type);
            baos.write(data, 0, data.length);
            baos.write((byte) 0x80); // 结束标记
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "SendData{" +
                "type=" + type +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
