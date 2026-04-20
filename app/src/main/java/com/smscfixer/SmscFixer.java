package com.smscfixer;

import android.app.PendingIntent;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SmscFixer implements IXposedHookLoadPackage {
    private static final String TAG = "SmscFixer";
    private static final String FORCED_SMSC = "+20105996500";

    private static volatile boolean hookedInProcess;
    private static final Object HOOK_LOCK = new Object();

    private static final XC_MethodHook FORCE_SMSC_HOOK = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            Object original = param.args[1];
            param.args[1] = FORCED_SMSC;
            XposedBridge.log(TAG + ": " + param.method.getName() + " scAddress="
                    + (original == null ? "null" : String.valueOf(original))
                    + " -> " + FORCED_SMSC);
        }
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        synchronized (HOOK_LOCK) {
            if (hookedInProcess) {
                return;
            }
            hookedInProcess = true;
        }

        final Class<?> smsManagerClass;
        try {
            smsManagerClass = Class.forName("android.telephony.SmsManager", false, null);
        } catch (ClassNotFoundException e) {
            XposedBridge.log(TAG + ": SmsManager not found: " + e);
            return;
        }

        safeHook(smsManagerClass, "sendTextMessage",
                String.class, String.class, String.class,
                PendingIntent.class, PendingIntent.class,
                FORCE_SMSC_HOOK);

        safeHook(smsManagerClass, "sendTextMessage",
                String.class, String.class, String.class,
                PendingIntent.class, PendingIntent.class, long.class,
                FORCE_SMSC_HOOK);

        safeHook(smsManagerClass, "sendTextMessageWithoutPersisting",
                String.class, String.class, String.class,
                PendingIntent.class, PendingIntent.class,
                FORCE_SMSC_HOOK);

        safeHook(smsManagerClass, "sendMultipartTextMessage",
                String.class, String.class, ArrayList.class,
                ArrayList.class, ArrayList.class,
                FORCE_SMSC_HOOK);

        safeHook(smsManagerClass, "sendMultipartTextMessage",
                String.class, String.class, ArrayList.class,
                ArrayList.class, ArrayList.class, long.class,
                FORCE_SMSC_HOOK);
    }

    private static void safeHook(Class<?> clazz, String methodName, Object... args) {
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, args);
            XposedBridge.log(TAG + ": hooked " + methodName);
        } catch (NoSuchMethodError e) {
            XposedBridge.log(TAG + ": method missing " + methodName + ": " + e.getMessage());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook error in " + methodName + ": " + t);
        }
    }
}
