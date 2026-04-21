package com.smscfixer;

import android.os.Build;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SmscFixer implements IXposedHookLoadPackage {
    private static final String TAG = "SmscFixer";
    public static final String DEFAULT_SMSC_PRIMARY = "+20105996500"; // Vodafone Egypt
    public static final String DEFAULT_SMSC_SECONDARY = "+20122000020"; // Orange Egypt
    private static final String MODULE_PACKAGE = "com.smscfixer";
    private static final String PREFS_NAME = "smscfixer_prefs";

    private static final int PRIMARY_SLOT_INDEX = 0;
    private static final int SECONDARY_SLOT_INDEX = 1;
    private static final int INVALID_SUBSCRIPTION_ID = -1;
    private static final int INVALID_SLOT_INDEX = -1;
    private static final long FALLBACK_LOG_THROTTLE_MS = 30_000L;

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
    private static final Map<String, Long> THROTTLED_LOGS = new ConcurrentHashMap<>();
    private static volatile SmscSelectionConfig runtimeConfig = buildDefaultConfig();

    private static final XC_MethodHook FORCE_SMSC_HOOK = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            if (param.args == null || param.args.length < 2) {
                return;
            }

            int subId = resolveSubscriptionId(param);
            int slotIndex = resolveSlotIndex(subId, param);
            CarrierInfo carrierInfo = resolveCarrierInfo(subId);
            String forcedSmsc = SmscSelector.selectSmsc(
                    slotIndex,
                    carrierInfo.mccMnc,
                    carrierInfo.carrierName,
                    runtimeConfig
            );

            Object original = param.args[1];
            if (forcedSmsc.equals(original)) {
                return;
            }

            param.args[1] = forcedSmsc;
            XposedBridge.log(TAG + ": " + param.method.getName() + " scAddress="
                    + (original == null ? "null" : String.valueOf(original))
                    + " -> " + forcedSmsc);

            if (romDiagnosticsEnabled) {
                XposedBridge.log(TAG + ": diag hook signature=" + param.method
                        + " carrier=" + carrierInfo.carrierName
                        + " mccmnc=" + carrierInfo.mccMnc
                        + " slotIndex=" + slotIndex);
            }
            maybeLogFallback(slotIndex, carrierInfo, forcedSmsc);
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

        runtimeConfig = loadRuntimeConfig();
        if (!shouldHandlePackage(lpparam.packageName, runtimeConfig.targetPackages)) {
            if (romDiagnosticsEnabled) {
                XposedBridge.log(TAG + ": skipping package=" + lpparam.packageName + " (not in target list)");
            }
            return;
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

    private static int resolveSubscriptionId(XC_MethodHook.MethodHookParam param) {
        Integer fromSmsManager = tryReadIntMethod(param.thisObject, "getSubscriptionId");
        if (isValidSubscriptionId(fromSmsManager)) {
            return fromSmsManager;
        }
        Integer legacySubId = tryReadIntMethod(param.thisObject, "getSubId");
        if (isValidSubscriptionId(legacySubId)) {
            return legacySubId;
        }
        Integer fieldSubId = tryReadIntField(param.thisObject, "mSubId");
        if (isValidSubscriptionId(fieldSubId)) {
            return fieldSubId;
        }
        Integer fromArgs = scanArgsForSubscriptionId(param.args);
        if (isValidSubscriptionId(fromArgs)) {
            return fromArgs;
        }
        return INVALID_SUBSCRIPTION_ID;
    }

    private static Integer scanArgsForSubscriptionId(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Integer) {
                Integer value = (Integer) arg;
                if (isValidSubscriptionId(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private static boolean isValidSubscriptionId(Integer value) {
        return value != null && value >= 0;
    }

    private static Integer tryReadIntMethod(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Object value = XposedHelpers.callMethod(target, methodName);
            if (value instanceof Integer) {
                int result = (Integer) value;
                return result >= 0 ? result : null;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Integer tryReadIntField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        try {
            Object value = XposedHelpers.getObjectField(target, fieldName);
            if (value instanceof Integer) {
                int result = (Integer) value;
                return result >= 0 ? result : null;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int resolveSlotIndex(int subId, XC_MethodHook.MethodHookParam param) {
        if (subId != INVALID_SUBSCRIPTION_ID) {
            Integer slotFromSubManager = resolveSlotFromSubscriptionManager(subId);
            if (isValidSlot(slotFromSubManager)) {
                return slotFromSubManager;
            }
        }
        Integer slotFromSmsManager = tryReadIntMethod(param.thisObject, "getSlotIndex");
        if (isValidSlot(slotFromSmsManager)) {
            return slotFromSmsManager;
        }
        Integer slotFromSimSlot = tryReadIntMethod(param.thisObject, "getSimSlotIndex");
        if (isValidSlot(slotFromSimSlot)) {
            return slotFromSimSlot;
        }
        Integer slotField = tryReadIntField(param.thisObject, "mSlotIndex");
        if (isValidSlot(slotField)) {
            return slotField;
        }
        return INVALID_SLOT_INDEX;
    }

    private static Integer resolveSlotFromSubscriptionManager(int subId) {
        if (subId < 0) {
            return null;
        }
        Integer viaGetSlotIndex = callSubscriptionSlotMethod("getSlotIndex", subId);
        if (isValidSlot(viaGetSlotIndex)) {
            return viaGetSlotIndex;
        }
        Integer viaGetPhoneId = callSubscriptionSlotMethod("getPhoneId", subId);
        if (isValidSlot(viaGetPhoneId)) {
            return viaGetPhoneId;
        }
        Integer viaGetSlotId = callSubscriptionSlotMethod("getSlotId", subId);
        if (isValidSlot(viaGetSlotId)) {
            return viaGetSlotId;
        }
        return null;
    }

    private static Integer callSubscriptionSlotMethod(String methodName, int subId) {
        try {
            Class<?> subscriptionManager = Class.forName("android.telephony.SubscriptionManager");
            Object result = XposedHelpers.callStaticMethod(subscriptionManager, methodName, subId);
            if (result instanceof Integer) {
                int slot = (Integer) result;
                return slot >= 0 ? slot : null;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isValidSlot(Integer slot) {
        return slot != null && (slot == PRIMARY_SLOT_INDEX || slot == SECONDARY_SLOT_INDEX);
    }

    private static CarrierInfo resolveCarrierInfo(int subId) {
        String carrierName = "";
        String mccMnc = "";

        Object application = null;
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            application = XposedHelpers.callStaticMethod(activityThread, "currentApplication");
        } catch (Throwable ignored) {
        }

        if (application != null) {
            try {
                Object telephony = XposedHelpers.callMethod(application, "getSystemService", "phone");
                if (telephony != null) {
                    Object scopedTelephony = telephony;
                    if (subId >= 0) {
                        try {
                            Object candidate = XposedHelpers.callMethod(telephony, "createForSubscriptionId", subId);
                            if (candidate != null) {
                                scopedTelephony = candidate;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    try {
                        Object resultName = XposedHelpers.callMethod(scopedTelephony, "getSimOperatorName");
                        if (resultName instanceof String) {
                            carrierName = (String) resultName;
                        }
                    } catch (Throwable ignored) {
                    }
                    try {
                        Object resultMccMnc = XposedHelpers.callMethod(scopedTelephony, "getSimOperator");
                        if (resultMccMnc instanceof String) {
                            mccMnc = (String) resultMccMnc;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return new CarrierInfo(carrierName, mccMnc);
    }

    private static void maybeLogFallback(int slotIndex, CarrierInfo carrierInfo, String selectedSmsc) {
        if (slotIndex != INVALID_SLOT_INDEX) {
            return;
        }
        String key = "fallback:" + SmscSelector.normalizeMccMnc(carrierInfo.mccMnc)
                + ":" + SmscSelector.normalizeCarrierName(carrierInfo.carrierName)
                + ":" + selectedSmsc;
        if (shouldLogThrottled(key)) {
            XposedBridge.log(TAG + ": slot resolution unavailable, fallback selected SMSC="
                    + selectedSmsc + " carrier=" + carrierInfo.carrierName
                    + " mccmnc=" + carrierInfo.mccMnc);
        }
    }

    private static boolean shouldLogThrottled(String key) {
        long now = System.currentTimeMillis();
        synchronized (THROTTLED_LOGS) {
            Long last = THROTTLED_LOGS.get(key);
            if (last != null && (now - last) < FALLBACK_LOG_THROTTLE_MS) {
                return false;
            }
            THROTTLED_LOGS.put(key, now);
            return true;
        }
    }

    private static boolean shouldHandlePackage(String packageName, Set<String> targetPackages) {
        if ("android".equals(packageName)) {
            return true;
        }
        if (targetPackages == null || targetPackages.isEmpty()) {
            return true;
        }
        return targetPackages.contains(packageName);
    }

    private static SmscSelectionConfig loadRuntimeConfig() {
        try {
            XSharedPreferences prefs = new XSharedPreferences(MODULE_PACKAGE, PREFS_NAME);
            prefs.reload();
            String primary = safeValue(prefs.getString("primary_smsc", DEFAULT_SMSC_PRIMARY), DEFAULT_SMSC_PRIMARY);
            String secondary = safeValue(prefs.getString("secondary_smsc", DEFAULT_SMSC_SECONDARY), DEFAULT_SMSC_SECONDARY);
            String targetPackagesCsv = safeValue(
                    prefs.getString("target_packages_csv", "com.google.android.apps.messaging,com.android.mms"),
                    "com.google.android.apps.messaging,com.android.mms"
            );
            boolean diagnosticsSetting = prefs.getBoolean("diagnostics_enabled", false);
            if (diagnosticsSetting) {
                romDiagnosticsEnabled = true;
            }
            return new SmscSelectionConfig(
                    primary,
                    secondary,
                    defaultMccMncFallbacks(primary, secondary),
                    defaultCarrierNameFallbacks(primary, secondary),
                    parsePackages(targetPackagesCsv)
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": config load failed, using defaults: " + t);
            return buildDefaultConfig();
        }
    }

    private static SmscSelectionConfig buildDefaultConfig() {
        return new SmscSelectionConfig(
                DEFAULT_SMSC_PRIMARY,
                DEFAULT_SMSC_SECONDARY,
                defaultMccMncFallbacks(DEFAULT_SMSC_PRIMARY, DEFAULT_SMSC_SECONDARY),
                defaultCarrierNameFallbacks(DEFAULT_SMSC_PRIMARY, DEFAULT_SMSC_SECONDARY),
                new LinkedHashSet<>(Arrays.asList("com.google.android.apps.messaging", "com.android.mms"))
        );
    }

    private static Map<String, String> defaultMccMncFallbacks(String primary, String secondary) {
        Map<String, String> map = new HashMap<>();
        map.put("60202", primary);   // Vodafone EG
        map.put("60201", secondary); // Orange EG
        return map;
    }

    private static Map<String, String> defaultCarrierNameFallbacks(String primary, String secondary) {
        Map<String, String> map = new HashMap<>();
        map.put(SmscSelector.normalizeCarrierName("vodafone"), primary);
        map.put(SmscSelector.normalizeCarrierName("vodafone egypt"), primary);
        map.put(SmscSelector.normalizeCarrierName("orange"), secondary);
        map.put(SmscSelector.normalizeCarrierName("orange egypt"), secondary);
        return map;
    }

    private static Set<String> parsePackages(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String item : csv.split(",")) {
            String pkg = item.trim();
            if (!pkg.isEmpty()) {
                out.add(pkg);
            }
        }
        return out;
    }

    private static String safeValue(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static final class CarrierInfo {
        final String carrierName;
        final String mccMnc;

        CarrierInfo(String carrierName, String mccMnc) {
            this.carrierName = carrierName == null ? "" : carrierName;
            this.mccMnc = mccMnc == null ? "" : mccMnc;
        }
    }
}
