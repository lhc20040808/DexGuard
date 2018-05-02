package com.lhc.dex.guard.core;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lhc
 * 时间：2018/5/2.
 */

public class DexInstall {

    public static class V21 {


        private V21() {

        }

        public static void install(Context context, List<File> dexFiles, File optimizedDirectory) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
            //private static Element[] makeDexElements(ArrayList<File> files, File optimizedDirectory,ArrayList<IOException> suppressedExceptions)
            ClassLoader classLoader = context.getClassLoader();
            //获取ClassLoader中的pathList#DexPathList
            Field pathListField = ReflectUtils.findField(classLoader, "pathList");
            Object pathList = pathListField.get(classLoader);
            //获取pathList中的dexElements#Element[]
            Field dexElementsField = ReflectUtils.findField(pathList, "dexElements");
            Object[] dexElements = (Object[]) dexElementsField.get(pathList);
            Method makeDexElementsMethod = ReflectUtils.findMethod(pathList, "makeDexElements", ArrayList.class, File.class, ArrayList.class);
            ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
            Object[] addElements = (Object[]) makeDexElementsMethod.invoke(pathList, dexFiles, optimizedDirectory, suppressedExceptions);
            //DexElements数组融合
            Object[] newElements = (Object[]) Array.newInstance(dexElements.getClass().getComponentType(), dexElements.length + addElements.length);
            System.arraycopy(dexElements, 0, newElements, 0, dexElements.length);
            System.arraycopy(addElements, 0, newElements, dexElements.length, addElements.length);
            dexElementsField.set(pathList, newElements);
        }
    }
}
