package com.noplayserviceswarning;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private void hookAllClassMethods(String className, ClassLoader classloader, String methodName, de.robv.android.xposed.XC_MethodHook callback){
        try {
            Class<?> dialogFragmentClass = XposedHelpers.findClass(className, classloader);
            XposedBridge.hookAllMethods(dialogFragmentClass, methodName, callback);
        } catch (Throwable t) {
            XposedBridge.log("Failed to hook " + className + " -> " + methodName + ": "+ t);
        }
    }

    private void hideGooglePlayAlertDialog(Object alertDialog) {
        if (alertDialog instanceof android.app.AlertDialog) {
            Object alertController = XposedHelpers.getObjectField(alertDialog, "mAlert");

            // title text is in mTitle (CharSequence)
            CharSequence t = (CharSequence) XposedHelpers.getObjectField(alertController, "mTitle");
            if (t != null) {
                XposedBridge.log("android.app.DialogFragment AlertDialog title: " + t);
                if (t.equals("Update Google Play services") || t.equals("Enable Google Play services")) {

                    // setting these two fields causes android.app.Dialog -> show() for this dialog to do nothing
                    XposedHelpers.setObjectField(alertDialog, "mShowing", true);
                    XposedHelpers.setObjectField(alertDialog, "mDecor", null);
                }
            }

        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        final String GMS_PACKAGE_NAME = "com.google.android.gms";

        // don't hook gms app itself b/c it breaks live transcribe (and maybe other stuff)...
        if (lpparam.packageName.equals(GMS_PACKAGE_NAME)) return;

        XposedBridge.log("\nnoplayserviceswarning loaded into: " + lpparam.packageName);


/*
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
*/

        // TODO
        // for newer apps, uses
        // com.google.android.gms.common.ConnectionResult
        // status code .c ??
        // ...


        hookAllClassMethods("android.app.AlertDialog$Builder", lpparam.classLoader,
                "create",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object alertDialog = param.getResult();
                        hideGooglePlayAlertDialog(alertDialog);
                    }
                });

        hookAllClassMethods("android.app.DialogFragment", lpparam.classLoader,
                "onStart",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Object thisObj = param.thisObject;
                        android.app.Dialog dialog = (android.app.Dialog) XposedHelpers.callMethod(thisObj, "getDialog");
                        hideGooglePlayAlertDialog(dialog);
                    }
                });

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