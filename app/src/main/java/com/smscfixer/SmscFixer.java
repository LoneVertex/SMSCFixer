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
    private static final String FORCED_SMSC_PRIMARY = "+20105996500"; // Vodafone Egypt
    private static final String FORCED_SMSC_SECONDARY = "+20122000020"; // Orange Egypt
    private static final int PRIMARY_SLOT_INDEX = 0;
    private static final int SECONDARY_SLOT_INDEX = 1;
    private static final int INVALID_SUBSCRIPTION_ID = -1;
    private static final int INVALID_SLOT_INDEX = -1;
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
            String forcedSmsc = resolveForcedSmsc(param);
            Object original = param.args[1];
            if (forcedSmsc.equals(original)) {
                return;
            }
            param.args[1] = forcedSmsc;
            XposedBridge.log(TAG + ": " + param.method.getName() + " scAddress="
                    + (original == null ? "null" : String.valueOf(original))
                    + " -> " + forcedSmsc);
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

    private static String resolveForcedSmsc(XC_MethodHook.MethodHookParam param) {
        int subId = resolveSubscriptionId(param);
        int slotIndex = resolveSlotIndex(subId);
        if (slotIndex == SECONDARY_SLOT_INDEX) {
            if (romDiagnosticsEnabled) {
                XposedBridge.log(TAG + ": diag selected secondary SMSC for subId=" + subId
                        + " slotIndex=" + slotIndex);
            }
            return FORCED_SMSC_SECONDARY;
        }
        if (slotIndex == INVALID_SLOT_INDEX) {
            XposedBridge.log(TAG + ": slot resolution unavailable for subId=" + subId
                    + ", using primary SMSC fallback");
        }
        if (romDiagnosticsEnabled) {
            XposedBridge.log(TAG + ": diag selected primary SMSC for subId=" + subId
                    + " slotIndex=" + slotIndex);
        }
        return FORCED_SMSC_PRIMARY;
    }

    private static int resolveSubscriptionId(XC_MethodHook.MethodHookParam param) {
        if (param.thisObject == null) {
            return INVALID_SUBSCRIPTION_ID;
        }
        try {
            Object value = XposedHelpers.callMethod(param.thisObject, "getSubscriptionId");
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (Throwable ignored) {
            // Keep fallback behavior for ROM/API variants where method is absent/inaccessible.
        }
        return INVALID_SUBSCRIPTION_ID;
    }

    private static int resolveSlotIndex(int subId) {
        if (subId == INVALID_SUBSCRIPTION_ID) {
            return INVALID_SLOT_INDEX;
        }
        try {
            Class<?> subscriptionManager = Class.forName("android.telephony.SubscriptionManager");
            Object result = XposedHelpers.callStaticMethod(subscriptionManager, "getSlotIndex", subId);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Throwable ignored) {
            // Keep fallback behavior for ROM/API variants where method is absent/inaccessible.
        }
        return INVALID_SLOT_INDEX;
    }
}
