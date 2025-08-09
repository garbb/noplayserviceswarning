package com.noplayserviceswarning;

import android.content.pm.PackageInfo;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class PackageInfoLogger {
    //    private static final String TAG = "PackageInfoLogger";
    private static final String TAG = "LSPosed-Bridge";
    private static final int MAX_DEPTH = 2;

    private static void logFields(Object obj, int depth, Set<Object> visited) {
        if (obj == null || depth > MAX_DEPTH || visited.contains(obj)) return;

        visited.add(obj);
        Class<?> clazz = obj.getClass();
        Log.d(TAG, indent(depth) + "Class: " + clazz.getName());

        Field[] fields = clazz.getFields(); // Only public fields
        for (Field field : fields) {
            try {
                Object value = field.get(obj);
                if (value == null || isPrimitiveOrWrapper(value.getClass())) {
                    Log.d(TAG, indent(depth) + field.getName() + " = " + value);
                } else if (value.getClass().isArray()) {
                    logArray(field.getName(), value, depth);
                } else {
                    Log.d(TAG, indent(depth) + field.getName() + ":");
                    logFields(value, depth + 1, visited);
                }
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Failed to access field: " + field.getName(), e);
            }
        }
    }

    private static void logArray(String fieldName, Object arrayObj, int depth) {
        int length = java.lang.reflect.Array.getLength(arrayObj);
        Log.d(TAG, indent(depth) + fieldName + " (array length: " + length + "):");
        for (int i = 0; i < length; i++) {
            Object element = java.lang.reflect.Array.get(arrayObj, i);
            Log.d(TAG, indent(depth + 1) + "[" + i + "] = " + element);
        }
    }

    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                clazz == Boolean.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Float.class ||
                clazz == Double.class ||
                clazz == Character.class ||
                clazz == Byte.class ||
                clazz == Short.class;
    }

    private static String indent(int depth) {
        return new String(new char[depth * 2]).replace('\0', ' ');
    }

    public static void logPackageInfo(PackageInfo pkgInfo) {
        logFields(pkgInfo, 0, new HashSet<>());

        Log.d(TAG,"\n");
    }
}
