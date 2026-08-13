package com.smscfixer;

import android.os.Build;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
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

/**
 * LSPosed entry point. This class only orchestrates configuration loading, explicit hook
 * installation, routing-signal lookup, and application of a pure routing decision.
 */
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
    private static final long REFLECTION_LOG_THROTTLE_MS = 120_000L;
    private static final int MAX_THROTTLED_LOG_KEYS = 64;

    private static final String EVT_HOOK_APPLIED = "hook_applied";
    private static final String EVT_PACKAGE_SKIPPED = "package_skipped";
    private static final String EVT_REPLACEMENT_PRESERVED = "replacement_preserved";
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
            HookSignatureRegistry.HookSignature signature = signatureFor(param);
            if (signature == null || param.args == null || param.args.length <= signature.smscArgumentIndex) {
                return;
            }

            int subId = resolveSubscriptionId(param.thisObject);
            int slotIndex = resolveSlotIndex(subId, param.thisObject);
            CarrierInfo carrierInfo = resolveCarrierInfo(subId);
            SmscSelector.SelectionResult selection = SmscSelector.selectSmscDetailed(
                    slotIndex,
                    carrierInfo.mccMnc,
                    carrierInfo.carrierName,
                    runtimeConfig
            );

            if (!selection.replacementAuthorized || selection.smsc == null) {
                maybeLogPreservedDecision(selection.reason);
                return;
            }

            Object original = param.args[signature.smscArgumentIndex];
            if (selection.smsc.equals(original)) {
                return;
            }

            param.args[signature.smscArgumentIndex] = selection.smsc;
            logDiagnosticEvent("smsc_replaced", "reason=" + selection.reason);
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
                    XposedBridge.log(TAG + ": event=rom_diagnostics_enabled");
                }
            }
        }

        boolean hookedSuccessfully = false;
        try {
            runtimeConfig = loadRuntimeConfig();
            if (!SmscRuntimeConfig.shouldHandlePackage(lpparam.packageName, runtimeConfig.targetPackages)) {
                logDiagnosticEvent(EVT_PACKAGE_SKIPPED, "scope=not_targeted");
                return;
            }

            final Class<?> smsManagerClass;
            try {
                smsManagerClass = XposedHelpers.findClass("android.telephony.SmsManager", lpparam.classLoader);
            } catch (Throwable error) {
                XposedBridge.log(TAG + ": event=sms_manager_unavailable");
                return;
            }

            hookedSuccessfully = hookCompatibleSendMethods(smsManagerClass) > 0;
            if (hookedSuccessfully) {
                XposedBridge.log(TAG + ": event=" + EVT_HOOK_APPLIED);
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

    private static HookSignatureRegistry.HookSignature signatureFor(XC_MethodHook.MethodHookParam param) {
        if (param == null || !(param.method instanceof Method)) {
            return null;
        }
        return HookSignatureRegistry.match((Method) param.method);
    }

    private static String buildHookScopeKey(XC_LoadPackage.LoadPackageParam lpparam) {
        String process = lpparam.processName == null ? "" : lpparam.processName;
        String pkg = lpparam.packageName == null ? "" : lpparam.packageName;
        int classLoaderId = lpparam.classLoader == null ? 0 : System.identityHashCode(lpparam.classLoader);
        return process + "|" + pkg + "|" + classLoaderId;
    }

    private static int hookCompatibleSendMethods(Class<?> clazz) {
        int hookedCount = 0;
        Set<String> seenSignatures = new HashSet<>();
        for (Method method : clazz.getDeclaredMethods()) {
            HookSignatureRegistry.HookSignature signature = HookSignatureRegistry.match(method);
            if (signature == null) {
                continue;
            }
            String diagnosticName = signature.diagnosticName();
            if (!seenSignatures.add(diagnosticName)) {
                continue;
            }
            try {
                XposedBridge.hookMethod(method, FORCE_SMSC_HOOK);
                hookedCount++;
                logDiagnosticEvent("hook_installed", "signature=" + diagnosticName);
            } catch (Throwable error) {
                XposedBridge.log(TAG + ": event=hook_install_failed");
                maybeLogReflectionFailure("hook:" + signature.methodName, error);
            }
        }
        XposedBridge.log(TAG + ": event=hook_summary count=" + hookedCount);
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

    private static int resolveSubscriptionId(Object smsManager) {
        Integer fromSmsManager = tryReadIntMethod(smsManager, "getSubscriptionId");
        if (isValidSubscriptionId(fromSmsManager)) {
            return fromSmsManager;
        }
        Integer legacySubId = tryReadIntMethod(smsManager, "getSubId");
        if (isValidSubscriptionId(legacySubId)) {
            return legacySubId;
        }
        Integer fieldSubId = tryReadIntField(smsManager, "mSubId");
        return isValidSubscriptionId(fieldSubId) ? fieldSubId : INVALID_SUBSCRIPTION_ID;
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
        } catch (Throwable error) {
            maybeLogReflectionFailure("read_method:" + methodName, error);
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
        } catch (Throwable error) {
            maybeLogReflectionFailure("read_field:" + fieldName, error);
        }
        return null;
    }

    private static int resolveSlotIndex(int subId, Object smsManager) {
        if (subId != INVALID_SUBSCRIPTION_ID) {
            Integer slotFromSubManager = resolveSlotFromSubscriptionManager(subId);
            if (isValidSlot(slotFromSubManager)) {
                return slotFromSubManager;
            }
        }
        Integer slotFromSmsManager = tryReadIntMethod(smsManager, "getSlotIndex");
        if (isValidSlot(slotFromSmsManager)) {
            return slotFromSmsManager;
        }
        Integer slotFromSimSlot = tryReadIntMethod(smsManager, "getSimSlotIndex");
        if (isValidSlot(slotFromSimSlot)) {
            return slotFromSimSlot;
        }
        Integer slotField = tryReadIntField(smsManager, "mSlotIndex");
        return isValidSlot(slotField) ? slotField : INVALID_SLOT_INDEX;
    }

    private static Integer resolveSlotFromSubscriptionManager(int subId) {
        Integer viaGetSlotIndex = callSubscriptionSlotMethod("getSlotIndex", subId);
        if (isValidSlot(viaGetSlotIndex)) {
            return viaGetSlotIndex;
        }
        Integer viaGetPhoneId = callSubscriptionSlotMethod("getPhoneId", subId);
        if (isValidSlot(viaGetPhoneId)) {
            return viaGetPhoneId;
        }
        Integer viaGetSlotId = callSubscriptionSlotMethod("getSlotId", subId);
        return isValidSlot(viaGetSlotId) ? viaGetSlotId : null;
    }

    private static Integer callSubscriptionSlotMethod(String methodName, int subId) {
        try {
            Class<?> subscriptionManager = Class.forName("android.telephony.SubscriptionManager");
            Object result = XposedHelpers.callStaticMethod(subscriptionManager, methodName, subId);
            if (result instanceof Integer) {
                int slot = (Integer) result;
                return slot >= 0 ? slot : null;
            }
        } catch (Throwable error) {
            maybeLogReflectionFailure("subscription_slot:" + methodName, error);
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
        if (application == null) {
            return new CarrierInfo(carrierName, mccMnc);
        }
        try {
            Object telephony = XposedHelpers.callMethod(application, "getSystemService", "phone");
            if (telephony == null) {
                return new CarrierInfo(carrierName, mccMnc);
            }
            Object scopedTelephony = telephony;
            if (subId >= 0) {
                try {
                    Object candidate = XposedHelpers.callMethod(telephony, "createForSubscriptionId", subId);
                    if (candidate != null) {
                        scopedTelephony = candidate;
                    }
                } catch (Throwable error) {
                    maybeLogReflectionFailure("carrier:create_for_subscription", error);
                }
            }
            try {
                Object resultName = XposedHelpers.callMethod(scopedTelephony, "getSimOperatorName");
                if (resultName instanceof String) {
                    carrierName = (String) resultName;
                }
            } catch (Throwable error) {
                maybeLogReflectionFailure("carrier:get_name", error);
            }
            try {
                Object resultMccMnc = XposedHelpers.callMethod(scopedTelephony, "getSimOperator");
                if (resultMccMnc instanceof String) {
                    mccMnc = (String) resultMccMnc;
                }
            } catch (Throwable error) {
                maybeLogReflectionFailure("carrier:get_operator", error);
            }
        } catch (Throwable error) {
            maybeLogReflectionFailure("carrier:get_service", error);
        }
        return new CarrierInfo(carrierName, mccMnc);
    }

    private static Object getCurrentApplication() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            return XposedHelpers.callStaticMethod(activityThread, "currentApplication");
        } catch (Throwable error) {
            maybeLogReflectionFailure("current_application", error);
            return null;
        }
    }

    private static void maybeLogPreservedDecision(SmscSelector.DecisionReason reason) {
        String key = "preserve:" + reason;
        if (shouldLogThrottled(key, FALLBACK_LOG_THROTTLE_MS)) {
            logDiagnosticEvent(EVT_REPLACEMENT_PRESERVED, "reason=" + reason);
        }
    }

    private static boolean shouldLogThrottled(String key, long throttleMs) {
        long now = System.currentTimeMillis();
        synchronized (THROTTLED_LOGS) {
            Long last = THROTTLED_LOGS.get(key);
            if (last != null && now - last < throttleMs) {
                return false;
            }
            if (!THROTTLED_LOGS.containsKey(key) && THROTTLED_LOGS.size() >= MAX_THROTTLED_LOG_KEYS) {
                THROTTLED_LOGS.clear();
            }
            THROTTLED_LOGS.put(key, now);
            return true;
        }
    }

    private static void maybeLogReflectionFailure(String operation, Throwable throwable) {
        String key = "reflection:" + operation + ":" + throwable.getClass().getName();
        if (shouldLogThrottled(key, REFLECTION_LOG_THROTTLE_MS)) {
            logDiagnosticEvent(EVT_REFLECTION_FAILURE, "operation=" + operation);
        }
    }

    private static void logDiagnosticEvent(String eventName, String message) {
        if (!romDiagnosticsEnabled && !EVT_CONFIG_LOAD_FAILED.equals(eventName)) {
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
            Set<String> targets = SmscConfigSchema.parseAndNormalizeTargetPackages(
                    prefs.getString(
                            SmscConfigSchema.KEY_TARGET_PACKAGES_CSV,
                            SmscConfigSchema.DEFAULT_TARGET_PACKAGES_CSV
                    )
            );
            boolean diagnosticsSetting = prefs.getBoolean(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, false);
            romDiagnosticsEnabled = romDiagnosticsAutoEnabled || diagnosticsSetting;
            SmscSelectionConfig config = SmscRuntimeConfig.buildConfig(
                    primary,
                    secondary,
                    targets,
                    schemaVersion
            );
            logDiagnosticEvent(EVT_CONFIG_LOADED, "target_count=" + config.targetPackages.size());
            return config;
        } catch (Throwable error) {
            XposedBridge.log(TAG + ": event=" + EVT_CONFIG_LOAD_FAILED);
            romDiagnosticsEnabled = romDiagnosticsAutoEnabled;
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
