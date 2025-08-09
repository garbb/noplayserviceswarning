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

    private void setField(Object Obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = Obj.getClass().getField(fieldName); // Only finds public fields
        field.setAccessible(true); // Allow access if it's not public
        field.set(Obj, value);
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

                                // empty ApplicationInfo makes app skip google play services check for some reason...
                                // also causes some apps to not even create google play services availability notification channel...
                                //fakeInfo.applicationInfo = new ApplicationInfo();

                                int fakeVersionCode = Integer.MAX_VALUE; // 2147483647
//                                int fakeVersionCode = 252863013;
//                                int fakeVersionCode = 12862063L;

                                fakeInfo.setLongVersionCode(fakeVersionCode);

                                ApplicationInfo appInfo = fakeInfo.applicationInfo;
                                appInfo.enabled = true;
                                setField(appInfo, "enabledSetting", PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
                                setField(appInfo, "longVersionCode", fakeVersionCode);
                                setField(appInfo, "versionCode", fakeVersionCode);

                                PackageInfoLogger.logPackageInfo(fakeInfo);

                                param.setResult(fakeInfo);
                            }
                        }
                    }
                }
        );




    }


}