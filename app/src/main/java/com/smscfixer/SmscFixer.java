package com.smscfixer;

import android.os.Build;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SmscFixer implements IXposedHookLoadPackage {
    private static final String TAG = "SmscFixer";
    private static final String FORCED_SMSC = "+20105996500";
    private static final String[] ROM_DIAGNOSTIC_KEYWORDS = {
            "a21s",
            "sm-a217",
            "infinity x",
            "infinityx"
    };

    private static volatile boolean hookedInProcess;
    private static volatile boolean romDiagnosticsEnabled;
    private static volatile boolean romDiagnosticsInitialized;
    private static final Object HOOK_LOCK = new Object();

    private static final XC_MethodHook FORCE_SMSC_HOOK = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            if (param.args == null || param.args.length < 2) {
                return;
            }
            Object original = param.args[1];
            if (FORCED_SMSC.equals(original)) {
                return;
            }
            param.args[1] = FORCED_SMSC;
            XposedBridge.log(TAG + ": " + param.method.getName() + " scAddress="
                    + (original == null ? "null" : String.valueOf(original))
                    + " -> " + FORCED_SMSC);
            if (romDiagnosticsEnabled) {
                XposedBridge.log(TAG + ": diag hook signature=" + param.method);
            }
        }
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        synchronized (HOOK_LOCK) {
            if (hookedInProcess) {
                return;
            }
            if (!romDiagnosticsInitialized) {
                romDiagnosticsEnabled = detectRomDiagnosticsEnabled();
                romDiagnosticsInitialized = true;
                if (romDiagnosticsEnabled) {
                    XposedBridge.log(TAG + ": ROM diagnostics enabled for model="
                            + Build.MODEL + " device=" + Build.DEVICE
                            + " display=" + Build.DISPLAY);
                }
            }
        }

        final Class<?> smsManagerClass;
        try {
            smsManagerClass = XposedHelpers.findClass("android.telephony.SmsManager", lpparam.classLoader);
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": SmsManager not found: " + e);
            return;
        }

        if (romDiagnosticsEnabled) {
            XposedBridge.log(TAG + ": analyzing package=" + lpparam.packageName
                    + " process=" + lpparam.processName);
        }

        int hookedCount = hookCompatibleSendMethods(smsManagerClass, romDiagnosticsEnabled);
        synchronized (HOOK_LOCK) {
            hookedInProcess = hookedCount > 0;
        }
    }

    private static int hookCompatibleSendMethods(Class<?> clazz, boolean diagnosticsEnabled) {
        int hookedCount = 0;
        Set<String> seenSignatures = new HashSet<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (!isCompatibleSmsSendMethod(method)) {
                if (diagnosticsEnabled && method.getName().startsWith("send")) {
                    XposedBridge.log(TAG + ": diag skipped incompatible method " + method);
                }
                continue;
            }
            String signature = method.toString();
            if (!seenSignatures.add(signature)) {
                continue;
            }
            try {
                XposedBridge.hookMethod(method, FORCE_SMSC_HOOK);
                hookedCount++;
                XposedBridge.log(TAG + ": hooked " + method.getName() + " " + signature);
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": hook error in " + method.getName() + ": " + t);
            }
        }
        XposedBridge.log(TAG + ": compatible methods hooked=" + hookedCount);
        return hookedCount;
    }

    private static boolean detectRomDiagnosticsEnabled() {
        String romInfo = (Build.BRAND + "|" + Build.MANUFACTURER + "|" + Build.MODEL + "|"
                + Build.DEVICE + "|" + Build.PRODUCT + "|" + Build.DISPLAY + "|"
                + Build.FINGERPRINT).toLowerCase(Locale.ROOT);
        for (String keyword : ROM_DIAGNOSTIC_KEYWORDS) {
            if (romInfo.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCompatibleSmsSendMethod(Method method) {
        if (!method.getName().startsWith("send")) {
            return false;
        }
        if (method.getReturnType() != Void.TYPE) {
            return false;
        }
        Class<?>[] params = method.getParameterTypes();
        return params.length >= 2
                && params[0] == String.class
                && params[1] == String.class;
    }
}
