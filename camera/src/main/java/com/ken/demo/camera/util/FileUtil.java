package com.ken.demo.camera.util;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static File writeFile(byte[] data, File saveDir, long count) {

        BufferedOutputStream outputStream = null;
        try {
            File file = new File(saveDir, count + ".jpg");
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            outputStream.write(data);
            Log.d(TAG, "writeFile: ==================================" + count);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        return null;
    }
}
