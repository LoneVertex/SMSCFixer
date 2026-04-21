package com.smscfixer;

import android.os.Build;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    private static final int SUBSCRIPTION_ARG_SCAN_START_INDEX = 2;
    private static final long FALLBACK_LOG_THROTTLE_MS = 30_000L;
    private static final long REFLECTION_LOG_THROTTLE_MS = 120_000L;

    private static final String EVT_HOOK_APPLIED = "hook_applied";
    private static final String EVT_PACKAGE_SKIPPED = "package_skipped";
    private static final String EVT_FALLBACK_USED = "fallback_used";
    private static final String EVT_AMBIGUOUS_SIGNALS = "ambiguous_signals";
    private static final String EVT_CONFIG_LOADED = "config_loaded";
    private static final String EVT_CONFIG_LOAD_FAILED = "config_load_failed";
    private static final String EVT_REFLECTION_FAILURE = "reflection_failure";

    private static final String[] ROM_DIAGNOSTIC_KEYWORDS = {
            "a21s",
            "sm-a217",
            "infinity x",
            "infinityx"
    };

    private static volatile boolean romDiagnosticsEnabled;
    private static volatile boolean romDiagnosticsAutoEnabled;
    private static volatile boolean romDiagnosticsInitialized;
    private static final Object HOOK_LOCK = new Object();
    private static final Set<String> HOOKED_SCOPES =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<String> HOOKING_SCOPES =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
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
            SmscSelector.SelectionResult selection = SmscSelector.selectSmscDetailed(
                    slotIndex,
                    carrierInfo.mccMnc,
                    carrierInfo.carrierName,
                    runtimeConfig
            );

            Object original = param.args[1];
            if (selection.smsc.equals(original)) {
                return;
            }

            param.args[1] = selection.smsc;
            XposedBridge.log(TAG + ": " + param.method.getName() + " scAddress="
                    + (original == null ? "null" : String.valueOf(original))
                    + " -> " + selection.smsc);

            if (romDiagnosticsEnabled) {
                XposedBridge.log(TAG + ": diag hook signature=" + param.method
                        + " carrier=" + carrierInfo.carrierName
                        + " mccmnc=" + carrierInfo.mccMnc
                        + " slotIndex=" + slotIndex
                        + " decision=" + selection.reason);
            }
            maybeLogFallback(slotIndex, carrierInfo, selection);
        }
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        String hookScope = buildHookScopeKey(lpparam);
        synchronized (HOOK_LOCK) {
            if (HOOKED_SCOPES.contains(hookScope) || HOOKING_SCOPES.contains(hookScope)) {
                return;
            }
            HOOKING_SCOPES.add(hookScope);
            if (!romDiagnosticsInitialized) {
                romDiagnosticsAutoEnabled = detectRomDiagnosticsEnabled();
                romDiagnosticsEnabled = romDiagnosticsAutoEnabled;
                romDiagnosticsInitialized = true;
                if (romDiagnosticsAutoEnabled) {
                    XposedBridge.log(TAG + ": ROM diagnostics enabled for model="
                            + Build.MODEL + " device=" + Build.DEVICE
                            + " display=" + Build.DISPLAY);
                }
            }
        }

        boolean hookedSuccessfully = false;
        try {
            runtimeConfig = loadRuntimeConfig();
            if (!SmscRuntimeConfig.shouldHandlePackage(lpparam.packageName, runtimeConfig.targetPackages)) {
                logDiagnosticEvent(EVT_PACKAGE_SKIPPED, "package=" + lpparam.packageName + " not in target list");
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

            hookedSuccessfully = hookCompatibleSendMethods(smsManagerClass, romDiagnosticsEnabled) > 0;
            if (hookedSuccessfully) {
                logDiagnosticEvent(EVT_HOOK_APPLIED, "package=" + lpparam.packageName);
            }
        } finally {
            synchronized (HOOK_LOCK) {
                HOOKING_SCOPES.remove(hookScope);
                if (hookedSuccessfully) {
                    HOOKED_SCOPES.add(hookScope);
                }
            }
        }
    }

    private static String buildHookScopeKey(XC_LoadPackage.LoadPackageParam lpparam) {
        String process = lpparam.processName == null ? "" : lpparam.processName;
        String pkg = lpparam.packageName == null ? "" : lpparam.packageName;
        int classLoaderId = lpparam.classLoader == null ? 0 : System.identityHashCode(lpparam.classLoader);
        return process + "|" + pkg + "|" + classLoaderId;
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
                if (diagnosticsEnabled) {
                    XposedBridge.log(TAG + ": hooked " + method.getName() + " " + signature);
                }
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
        Integer fromArgs = scanArgsForSubscriptionId(param);
        if (isValidSubscriptionId(fromArgs)) {
            return fromArgs;
        }
        return INVALID_SUBSCRIPTION_ID;
    }

    private static Integer scanArgsForSubscriptionId(XC_MethodHook.MethodHookParam param) {
        if (param == null || param.args == null || !(param.method instanceof Method)) {
            return null;
        }

        Method method = (Method) param.method;
        if (!isCompatibleSmsSendMethod(method)) {
            return null;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        int max = Math.min(param.args.length, parameterTypes.length);
        for (int i = SUBSCRIPTION_ARG_SCAN_START_INDEX; i < max; i++) {
            Object arg = param.args[i];
            Class<?> type = parameterTypes[i];
            if (type != Integer.TYPE || !(arg instanceof Integer)) {
                continue;
            }
            Integer value = (Integer) arg;
            if (isLikelySubscriptionId(value) && isActiveSubscriptionId(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isLikelySubscriptionId(Integer value) {
        if (!isValidSubscriptionId(value)) {
            return false;
        }
        if (isValidSlot(resolveSlotFromSubscriptionManager(value))) {
            return true;
        }
        Integer defaultSmsSubId = resolveDefaultSmsSubscriptionId();
        return defaultSmsSubId != null && defaultSmsSubId.equals(value);
    }

    private static boolean isActiveSubscriptionId(Integer subId) {
        if (!isValidSubscriptionId(subId)) {
            return false;
        }
        try {
            Object application = getCurrentApplication();
            if (application == null) {
                return false;
            }
            Object manager = XposedHelpers.callMethod(application, "getSystemService", "telephony_subscription_service");
            if (manager == null) {
                return false;
            }
            Object infos = XposedHelpers.callMethod(manager, "getActiveSubscriptionInfoList");
            if (!(infos instanceof List)) {
                return false;
            }
            List<?> list = (List<?>) infos;
            for (Object info : list) {
                if (info == null) {
                    continue;
                }
                Object id = XposedHelpers.callMethod(info, "getSubscriptionId");
                if (id instanceof Integer && subId.equals(id)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            maybeLogReflectionFailure("isActiveSubscriptionId", t);
        }
        return false;
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
        } catch (Throwable t) {
            maybeLogReflectionFailure("tryReadIntMethod:" + methodName, t);
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
        } catch (Throwable t) {
            maybeLogReflectionFailure("tryReadIntField:" + fieldName, t);
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
        } catch (Throwable t) {
            maybeLogReflectionFailure("callSubscriptionSlotMethod:" + methodName, t);
        }
        return null;
    }

    private static Integer resolveDefaultSmsSubscriptionId() {
        try {
            Class<?> subscriptionManager = Class.forName("android.telephony.SubscriptionManager");
            Object result = XposedHelpers.callStaticMethod(subscriptionManager, "getDefaultSmsSubscriptionId");
            if (result instanceof Integer) {
                int subId = (Integer) result;
                return subId >= 0 ? subId : null;
            }
        } catch (Throwable t) {
            maybeLogReflectionFailure("resolveDefaultSmsSubscriptionId", t);
        }
        return null;
    }

    private static boolean isValidSlot(Integer slot) {
        return slot != null && (slot == PRIMARY_SLOT_INDEX || slot == SECONDARY_SLOT_INDEX);
    }

    private static CarrierInfo resolveCarrierInfo(int subId) {
        String carrierName = "";
        String mccMnc = "";

        Object application = getCurrentApplication();
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
                        } catch (Throwable t) {
                            maybeLogReflectionFailure("resolveCarrierInfo:createForSubscriptionId", t);
                        }
                    }
                    try {
                        Object resultName = XposedHelpers.callMethod(scopedTelephony, "getSimOperatorName");
                        if (resultName instanceof String) {
                            carrierName = (String) resultName;
                        }
                    } catch (Throwable t) {
                        maybeLogReflectionFailure("resolveCarrierInfo:getSimOperatorName", t);
                    }
                    try {
                        Object resultMccMnc = XposedHelpers.callMethod(scopedTelephony, "getSimOperator");
                        if (resultMccMnc instanceof String) {
                            mccMnc = (String) resultMccMnc;
                        }
                    } catch (Throwable t) {
                        maybeLogReflectionFailure("resolveCarrierInfo:getSimOperator", t);
                    }
                }
            } catch (Throwable t) {
                maybeLogReflectionFailure("resolveCarrierInfo:getSystemService", t);
            }
        }

        return new CarrierInfo(carrierName, mccMnc);
    }

    private static Object getCurrentApplication() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            return XposedHelpers.callStaticMethod(activityThread, "currentApplication");
        } catch (Throwable t) {
            maybeLogReflectionFailure("getCurrentApplication", t);
            return null;
        }
    }

    private static void maybeLogFallback(
            int slotIndex,
            CarrierInfo carrierInfo,
            SmscSelector.SelectionResult selection
    ) {
        if (slotIndex != INVALID_SLOT_INDEX && selection.reason != SmscSelector.DecisionReason.AMBIGUOUS_CARRIER_SIGNALS) {
            return;
        }
        String key = "fallback:" + SmscSelector.normalizeMccMnc(carrierInfo.mccMnc)
                + ":" + SmscSelector.normalizeCarrierName(carrierInfo.carrierName)
                + ":" + selection.smsc
                + ":" + selection.reason;
        if (shouldLogThrottled(key, FALLBACK_LOG_THROTTLE_MS)) {
            String message = "selected SMSC=" + selection.smsc
                    + " decision=" + selection.reason
                    + " carrier=" + carrierInfo.carrierName
                    + " mccmnc=" + carrierInfo.mccMnc;
            if (selection.reason == SmscSelector.DecisionReason.AMBIGUOUS_CARRIER_SIGNALS) {
                logDiagnosticEvent(EVT_AMBIGUOUS_SIGNALS, message);
            } else {
                logDiagnosticEvent(EVT_FALLBACK_USED, message);
            }
        }
    }

    private static boolean shouldLogThrottled(String key, long throttleMs) {
        long now = System.currentTimeMillis();
        synchronized (THROTTLED_LOGS) {
            Long last = THROTTLED_LOGS.get(key);
            if (last != null && (now - last) < throttleMs) {
                return false;
            }
            THROTTLED_LOGS.put(key, now);
            return true;
        }
    }

    private static void maybeLogReflectionFailure(String operation, Throwable throwable) {
        String key = "refl:" + operation + ":" + throwable.getClass().getName();
        if (shouldLogThrottled(key, REFLECTION_LOG_THROTTLE_MS)) {
            logDiagnosticEvent(EVT_REFLECTION_FAILURE, operation + " failed: " + throwable);
        }
    }

    private static void logDiagnosticEvent(String eventName, String message) {
        if (!romDiagnosticsEnabled
                && !EVT_CONFIG_LOAD_FAILED.equals(eventName)
                && !EVT_REFLECTION_FAILURE.equals(eventName)) {
            return;
        }
        XposedBridge.log(TAG + ": event=" + eventName + " " + message);
    }

    private static SmscSelectionConfig loadRuntimeConfig() {
        try {
            XSharedPreferences prefs = new XSharedPreferences(MODULE_PACKAGE, PREFS_NAME);
            prefs.reload();
            int schemaVersion = prefs.getInt(SmscConfigSchema.KEY_SCHEMA_VERSION, SmscConfigSchema.CURRENT_VERSION);
            String primary = SmscConfigSchema.normalizeSmscOrDefault(
                    prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, DEFAULT_SMSC_PRIMARY),
                    DEFAULT_SMSC_PRIMARY
            );
            String secondary = SmscConfigSchema.normalizeSmscOrDefault(
                    prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, DEFAULT_SMSC_SECONDARY),
                    DEFAULT_SMSC_SECONDARY
            );
            String targetPackagesCsv = prefs.getString(
                    SmscConfigSchema.KEY_TARGET_PACKAGES_CSV,
                    SmscConfigSchema.DEFAULT_TARGET_PACKAGES_CSV
            );
            boolean diagnosticsSetting = prefs.getBoolean(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, false);
            romDiagnosticsEnabled = romDiagnosticsAutoEnabled || diagnosticsSetting;
            SmscSelectionConfig config = SmscRuntimeConfig.buildConfig(
                    primary,
                    secondary,
                    SmscConfigSchema.parseAndNormalizeTargetPackages(targetPackagesCsv),
                    schemaVersion
            );
            logDiagnosticEvent(EVT_CONFIG_LOADED,
                    "schemaVersion=" + config.configVersion + " targets=" + config.targetPackages.size());
            return config;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": config load failed, using defaults: " + t);
            romDiagnosticsEnabled = romDiagnosticsAutoEnabled;
            logDiagnosticEvent(EVT_CONFIG_LOAD_FAILED, t.toString());
            return buildDefaultConfig();
        }
    }

    private static SmscSelectionConfig buildDefaultConfig() {
        return SmscRuntimeConfig.buildDefaultConfig(DEFAULT_SMSC_PRIMARY, DEFAULT_SMSC_SECONDARY);
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
