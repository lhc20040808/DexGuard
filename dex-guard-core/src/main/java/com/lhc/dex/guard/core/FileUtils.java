package com.lhc.dex.guard.core;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 作者：lhc
 * 时间：2018/5/1.
 */

public class FileUtils {


    static {
        System.loadLibrary("op_ssl");
    }

    public static native void decrypt(byte[] data, String path);

    public static byte[] getBytes(File file) {

        RandomAccessFile r = null;

        try {
            r = new RandomAccessFile(file, "r");
            byte[] buffer = new byte[(int) r.length()];
            r.readFully(buffer);
            return buffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(r);
        }
        return null;
    }

    private static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
