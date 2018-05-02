package com.lhc.dex.guard.tool.groovy;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 作者：lhc
 * 时间：2018/5/1.
 */

public class ZipUtils {

    private static void delFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                delFile(f);
            }
        } else {
            file.delete();
        }
    }

    public static void unZip(File unZipFile, File dstFile) {
        ZipFile zipFile = null;
        try {
            delFile(dstFile);
            zipFile = new ZipFile(unZipFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String name = zipEntry.getName();

                if (name.contains("META-INF")) {
                    //META-INF为签名文件，不需要解压
                    continue;
                }

                if (!zipEntry.isDirectory()) {
                    File file = new File(dstFile, name);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(file);
                    InputStream is = zipFile.getInputStream(zipEntry);
                    byte[] buffer = new byte[2048];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }

                    close(is);
                    close(fos);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void zip(File file, File dstFile) {
        dstFile.delete();

        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(dstFile), new CRC32()));
            compress(zos, file, "");
            zos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(zos);
        }

    }

    private static void compress(ZipOutputStream zos, File srcFile,
                                 String basePath) throws IOException {
        if (srcFile.isDirectory()) {
            File[] files = srcFile.listFiles();
            for (File file : files) {
                // zip 递归添加目录中的文件
                compress(zos, file, basePath + srcFile.getName() + "/");
            }
        } else {
            compressFile(zos, srcFile, basePath);
        }
    }

    private static void compressFile(ZipOutputStream zos, File file, String dir)
            throws IOException {
        String fullName = dir + file.getName();
        // 需要去掉temp
        String[] fileNames = fullName.split("/");
        //正确的文件目录名 (去掉了temp)
        StringBuffer sb = new StringBuffer();
        if (fileNames.length > 1) {
            for (int i = 1; i < fileNames.length; ++i) {
                sb.append("/");
                sb.append(fileNames[i]);
            }
        } else {
            sb.append("/");
        }
        //添加一个zip条目
        ZipEntry entry = new ZipEntry(sb.substring(1));
        zos.putNextEntry(entry);
        //读取条目输出到zip中
        FileInputStream fis = new FileInputStream(file);
        int len;
        byte data[] = new byte[2048];
        while ((len = fis.read(data, 0, 2048)) != -1) {
            zos.write(data, 0, len);
        }
        fis.close();
        zos.closeEntry();
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
