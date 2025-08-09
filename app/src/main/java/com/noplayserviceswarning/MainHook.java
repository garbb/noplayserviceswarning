package com.noplayserviceswarning;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private void setField(Object targetObject, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        if (targetObject == null) {
            return;
        }
        try {
            Field field;
            try {
                field = targetObject.getClass().getField(fieldName); // Try public field first
            } catch (NoSuchFieldException e) {
                field = targetObject.getClass().getDeclaredField(fieldName); // Then private/protected
            }
            field.setAccessible(true);
            field.set(targetObject, value);
        } catch (Throwable ignored) {
            // Fail silently
        }

    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("\nnoplayserviceswarning loaded into: " + lpparam.packageName);

        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader),
                "getPackageInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws NoSuchFieldException, IllegalAccessException {
                        if (param.args.length >= 1 && param.args[0] instanceof String) {

                            final String GMS_PACKAGE_NAME = "com.google.android.gms";

                            String packageName = (String) param.args[0];

                            if (GMS_PACKAGE_NAME.equals(packageName)) {
                                XposedBridge.log("Spoofing getPackageInfo() for: " + packageName);
                                PackageInfo fakeInfo = (PackageInfo) param.getResult();

                                int fakeVersionCode = Integer.MAX_VALUE; // 2147483647
//                                int fakeVersionCode = 252863013;
//                                int fakeVersionCode = 12862063L;

                                fakeInfo.setLongVersionCode(fakeVersionCode);

                                ApplicationInfo appInfo = fakeInfo.applicationInfo;
                                appInfo.enabled = true;
                                setField(appInfo, "enabledSetting", PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
                                setField(appInfo, "longVersionCode", fakeVersionCode);
                                setField(appInfo, "versionCode", fakeVersionCode);

                                // for debugging...
                                //PackageInfoLogger.logPackageInfo(fakeInfo);

                                param.setResult(fakeInfo);
                            }
                        }
                    }
                }
        );




    }


}