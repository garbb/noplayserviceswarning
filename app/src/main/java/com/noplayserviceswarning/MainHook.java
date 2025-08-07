package com.noplayserviceswarning;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log("\nnoplayserviceswarning loaded into: " + lpparam.packageName);

        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader),
                "getPackageInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args.length >= 1 && param.args[0] instanceof String) {

                            final String GMS_PACKAGE_NAME = "com.google.android.gms";

                            String packageName = (String) param.args[0];

                            if (GMS_PACKAGE_NAME.equals(packageName)) {
                                XposedBridge.log("Spoofing getPackageInfo() for: " + packageName);
                                PackageInfo fakeInfo = (PackageInfo) param.getResult();

                                // empty ApplicationInfo makes app skip google play services check for some reason...
                                // also causes some apps to not even create google play services availability notification channel...
                                fakeInfo.applicationInfo = new ApplicationInfo();

                                param.setResult(fakeInfo);
                            }
                        }
                    }
                }
        );



        String targetClass = "com.google.android.gms.common.GoogleApiAvailabilityLight";
//        String targetClass = "com.google.android.gms.common.GoogleApiAvailability";
//        String targetClass = "com.google.android.gms.common.GooglePlayServicesUtilLight";
        Class<?> classexists = XposedHelpers.findClassIfExists(targetClass, lpparam.classLoader);
        if (classexists != null) {
            try {
                XposedBridge.hookAllMethods(
                        classexists, // className
                        "isGooglePlayServicesAvailable", // the method to hook
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) {
                                XposedBridge.log("making isGooglePlayServicesAvailable return 0 for " + lpparam.packageName);
                                // isGooglePlayServicesAvailable returns 0 if there are no errors
                                // This way the method it's not executed and 0 is always returned
                                return 0;
                            }
                        }
                );
                XposedBridge.log("hooked isGooglePlayServicesAvailable for : " + lpparam.packageName);
            }
            catch (NoSuchMethodError e) {
                XposedBridge.log("Error: \"" + lpparam.appInfo.name + "\" does not contain the target method..\n");
            }
            catch (Exception e) {
                XposedBridge.log("Unexpected error :\n" + e.getMessage());
            }

        }


        // when notification channel is created, set importance to NONE
        // this happens upon first launch after data clear
        XposedHelpers.findAndHookMethod(NotificationManager.class, "createNotificationChannel",
            NotificationChannel.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        NotificationChannel channel = (NotificationChannel) param.args[0];
                        if (channel.getId().equals("com.google.android.gms.availability")) {
                            XposedBridge.log("hooked createNotificationChannel for " + channel.getId());
                            channel.setImportance(NotificationManager.IMPORTANCE_NONE);
                        }
                    }
                });

        // if channel already exists then change importance to NONE
        XposedHelpers.findAndHookMethod(Application.class, "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Context context = (Context) param.args[0];

                        NotificationManager nm = context.getSystemService(NotificationManager.class);
                        NotificationChannel channel = nm.getNotificationChannel("com.google.android.gms.availability");

                        if (channel != null) {
                            XposedBridge.log("setting importance for " + channel.getId() + " to IMPORTANCE_NONE");
                            channel.setImportance(NotificationManager.IMPORTANCE_NONE);
                            nm.createNotificationChannel(channel);
                        }

                    }
                });


    }


}