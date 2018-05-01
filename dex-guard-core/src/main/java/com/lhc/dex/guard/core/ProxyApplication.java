package com.lhc.dex.guard.core;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 作者：lhc
 * 时间：2018/5/1.
 */

public class ProxyApplication extends Application {
    private static final String KEY_APP_NAME = "app_name";
    private static final String DEX_SUFFIX = ".dex";
    private static final String MAIN_DEX = "classes.dex";
    private String appName;
    private boolean isBind;
    private Application application;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //attachBaseContext是创建application后调用的第一个函数
        //在此处解密并加载多dex
        //TODO 多次启动application是否会导致这个方法回调多次导致多次加载
        getMetaData();
        File apkFile = new File(getApplicationInfo().sourceDir);//apk目录
        File pkgFile = getDir(appName, MODE_PRIVATE);//data/data/packagename/
        File appDir = new File(pkgFile, "app");//解压apk的目录
        File dexDir = new File(appDir, "dexDir");//存放dex的目录
        List<File> dexFiles = new ArrayList<>();
        //TODO md5文件校验，如何写入数据尽可能的保证本地存储的md5校验码不被修改
        //dex文件不存在或者当前文件夹下的文件校验不通过
        if (!dexDir.exists() || dexDir.list().length == 0 || !checkMd5(dexDir)) {
            //把apk解压到appDir
            ZipUtils.unZip(apkFile, appDir);

            File[] files = appDir.listFiles();
            for (File file : files) {
                String name = file.getName();
                //文件名是.dex结尾，并且不是主dex则放入dexDir目录
                if (name.endsWith(DEX_SUFFIX) && !TextUtils.equals(name, MAIN_DEX)) {
                    byte[] bytes = FileUtils.getBytes(file);
                    FileUtils.decrypt(bytes, file.getAbsolutePath());//解密并写入原文件
                    dexFiles.add(file);
                    Log.e("lhc", "Dex加载");
                }
            }
        } else {
            dexFiles.addAll(Arrays.asList(dexDir.listFiles()));
        }

        loadDex(dexFiles, pkgFile);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindRealApplication();
    }

    /**
     * 替换Application
     */
    private void bindRealApplication() {
        if (TextUtils.isEmpty(appName)) {
            return;
        }

        if (isBind) {
            return;
        }

        try {
            Context baseContext = getBaseContext();

            //创建原宿主application
            Class<?> delegateClz = Class.forName(appName);
            application = (Application) delegateClz.newInstance();
            Method attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(application, baseContext);

            //替换ContextImpl类下Context mOuterContext字段
            Class<?> contextImplClz = Class.forName("android.app.ContextImpl");
            Field mOuterCtxField = contextImplClz.getDeclaredField("mOuterContext");
            mOuterCtxField.setAccessible(true);
            mOuterCtxField.set(baseContext, application);

            //替换ActivityThread类下ArrayList<Application> mAllApplications字段 和Application mInitialApplication字段
            Class<?> activityThreadClz = Class.forName("android.app.ActivityThread");
            Field mAllApplicationsField = activityThreadClz.getDeclaredField("mAllApplications");
            Field mInitialApplicationField = activityThreadClz.getDeclaredField("mInitialApplication");
            mInitialApplicationField.setAccessible(true);
            mAllApplicationsField.setAccessible(true);

            //获得ActivityThread，通过ContextImpl的mMainThread获得
            Field mMainThreadField = contextImplClz.getDeclaredField("mMainThread");
            mMainThreadField.setAccessible(true);
            Object mMainThread = mMainThreadField.get(baseContext);

            //替换ActivityThread类下Application mInitialApplication字段
            mInitialApplicationField.set(mMainThread, application);

            ArrayList<Application> mAllApplications = (ArrayList<Application>) mAllApplicationsField.get(mMainThread);
            mAllApplications.remove(this);
            mAllApplications.add(application);

            //替换LoadedApk类下的Application mApplication字段
            Field mPackageInfoField = contextImplClz.getDeclaredField("mPackageInfo");
            mPackageInfoField.setAccessible(true);
            Object mPackageInfo = mPackageInfoField.get(baseContext);

            Class<?> loadApkInfoClz = Class.forName("android.app.LoadedApk");
            Field mApplicationField = loadApkInfoClz.getDeclaredField("mApplication");
            mApplicationField.setAccessible(true);
            mApplicationField.set(mPackageInfo, application);

            //修改ApplicationInfo下的className
            Field mApplicationInfoField = loadApkInfoClz.getDeclaredField("mApplicationInfo");
            mApplicationInfoField.setAccessible(true);
            ApplicationInfo applicationInfo = (ApplicationInfo) mApplicationInfoField.get(mPackageInfo);
            applicationInfo.className = appName;

            application.onCreate();

            isBind = true;

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    private void loadDex(List<File> dexFiles, File optimizedDirectory) {
        //TODO 加载过程中的异常处理机制

        //获得系统classLoader中dexElement数组
        try {
            ClassLoader classLoader = getClassLoader();
            //获取ClassLoader中的pathList#DexPathList
            Field pathListField = ReflectUtils.findField(classLoader, "pathList");
            Object pathList = pathListField.get(classLoader);
            //获取pathList中的dexElements#Element[]
            Field dexElementsField = ReflectUtils.findField(pathList, "dexElements");
            Object[] dexElements = (Object[]) dexElementsField.get(pathList);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //[4.0-5.0)

            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                //[5.0-6.0)
                //private static Element[] makeDexElements(ArrayList<File> files, File optimizedDirectory,ArrayList<IOException> suppressedExceptions)
                Method makeDexElementsMethod = ReflectUtils.findMethod(pathList, "makeDexElements", ArrayList.class, File.class, ArrayList.class);
                ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
                Object[] addElements = (Object[]) makeDexElementsMethod.invoke(pathList, dexFiles, optimizedDirectory, suppressedExceptions);
                //DexElements数组融合
                Object[] newElements = (Object[]) Array.newInstance(dexElements.getClass().getComponentType(), dexElements.length + addElements.length);
                System.arraycopy(dexElements, 0, newElements, 0, dexElements.length);
                System.arraycopy(addElements, 0, newElements, dexElements.length, addElements.length);

                dexElementsField.set(pathList, newElements);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                //[6.0-7.0)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                //[7.0-8.0)
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {

            }

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        if (TextUtils.isEmpty(appName)) {
            return super.createPackageContext(packageName, flags);
        } else {
            bindRealApplication();
        }
        return application;
    }

    @Override
    public String getPackageName() {
        if (TextUtils.isEmpty(appName)) {
            return super.getPackageName();
        } else {
            return "";
        }
    }

    private boolean checkMd5(File file) {
        return false;
    }

    public void getMetaData() {
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;
            if (metaData != null) {
                //判断是否设置name为app_name的meta-data数据
                if (metaData.containsKey(KEY_APP_NAME)) {
                    appName = metaData.getString(KEY_APP_NAME);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

}
