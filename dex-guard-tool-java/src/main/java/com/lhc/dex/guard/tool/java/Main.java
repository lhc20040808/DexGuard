package com.lhc.dex.guard.tool.java;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 作者：lhc
 * 时间：2018/5/1.
 */

public class Main {

    public static void main(String[] args) {
        File aarFile = new File("dex-guard-core/build/outputs/aar/dex-guard-core-debug.aar");
        File aarTmp = new File("dex-guard-tool-java/build/outputs/temp");
        ZipUtils.unZip(aarFile, aarTmp);//解压aar包，需要其中的jar文件
        File classesJarFile = new File(aarTmp, "classes.jar");
        File classesDexFile = new File(aarTmp, "classes.dex");
        try {
            //执行dx命令，把jar包变成dex文件。windows系统需要加上 cmd /c 在命令前面
            Process exec = Runtime.getRuntime().exec("./dx --dex --output " + classesDexFile.getAbsolutePath() + " " + classesJarFile.getAbsolutePath());
            exec.waitFor();

            if (exec.exitValue() != 0) {
                throw new RuntimeException("Jar To Dex Error");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        File apkFile = new File("app/build/outputs/apk/debug/app-debug.apk");
        File apkTmp = new File("app/build/outputs/temp");
        dexEncrypt(apkFile, apkTmp);//dex加密

        //把classes.dex放入解压目录并压缩成apk
        classesDexFile.renameTo(new File(apkTmp, "classes.dex"));
        File unsignedApk = new File("app/build/outputs/apk/debug/app-unsigned.apk");
        ZipUtils.zip(apkTmp, unsignedApk);

        //对齐apk
        File unsignedAlignApk = new File("app/build/outputs/apk/debug/app-unsigned-aligned.apk");
        try {
            Process processAlign = Runtime.getRuntime().exec("./zipalign -f 4 " + unsignedApk.getAbsolutePath() + " " + unsignedAlignApk.getAbsolutePath());
            processAlign.waitFor();

            if (processAlign.exitValue() != 0) {
                throw new RuntimeException("Apk Align Error");
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            //签名apk
            //apksigner sign --ks 签名 --ks-key-alias 别名 --ks-pass pass:密码 --out 输出 输入
            File signedApk = new File("app/build/outputs/apk/debug/app-signed-aligned.apk");
            File jks = new File("lhc");
            Process processAlign = Runtime.getRuntime().exec("./apksigner sign --ks " + jks.getAbsolutePath() + " --ks-key-alias lhc --ks-pass pass:112233 --out " + signedApk.getAbsolutePath() + " " + unsignedAlignApk.getAbsolutePath());
            processAlign.waitFor();

            if (processAlign.exitValue() != 0) {
                throw new RuntimeException("Apk Sign Error");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加密apk中所有dex
     */
    private static void dexEncrypt(File apkFile, File apkTmp) {

        ZipUtils.unZip(apkFile, apkTmp);
        //获得所有dex
        File[] dexFiles = apkTmp.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".dex");
            }
        });
        //初始化AES

        AES.init(AES.DEFAULT_PWD);
        try {
            for (File dex : dexFiles) {
                byte[] bytes = getBytes(dex);
                byte[] encrypt = AES.encrypt(bytes);//加密

                if (encrypt == null)
                    continue;

                FileOutputStream fos = new FileOutputStream(new File(apkTmp, "secret-" + dex.getName()));
                fos.write(encrypt);
                fos.flush();
                close(fos);
                dex.delete();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] getBytes(File file) {
        RandomAccessFile r = null;
        try {
            r = new RandomAccessFile(file, "r");
            byte[] bytes = new byte[(int) r.length()];
            r.readFully(bytes);
            return bytes;
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
