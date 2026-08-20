package io.github.lonevertex.smscguard;

import android.os.Build;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** LSPosed entry point and thin orchestration layer. */
public class SmscGuard implements IXposedHookLoadPackage {
    private static final String TAG = "SmscGuard";
    public static final String DEFAULT_SMSC_PRIMARY = "+20105996500"; // Vodafone Egypt
    public static final String DEFAULT_SMSC_SECONDARY = "+20122000020"; // Orange Egypt
    private static final String MODULE_PACKAGE = "io.github.lonevertex.smscguard";
    private static final String PREFS_NAME = "smscguard_prefs";
    private static final long PRESERVED_DECISION_LOG_THROTTLE_MS = 30_000L;

    private static final String[] ROM_DIAGNOSTIC_KEYWORDS = {
            "a21s",
            "sm-a217",
            "infinity x",
            "infinityx"
    };

    private static final Object HOOK_LOCK = new Object();
    private static final Set<String> HOOKED_SCOPES =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<String> HOOKING_SCOPES =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final DiagnosticLogger LOGGER = new DiagnosticLogger(TAG);
    private static final ConfigurationRepository CONFIGURATION_REPOSITORY = new ConfigurationRepository(
            MODULE_PACKAGE,
            PREFS_NAME,
            DEFAULT_SMSC_PRIMARY,
            DEFAULT_SMSC_SECONDARY,
            LOGGER
    );
    private static final RoutingSignalResolver ROUTING_SIGNAL_RESOLVER = new RoutingSignalResolver(LOGGER);

    private static volatile boolean romDiagnosticsEnabled;
    private static volatile boolean romDiagnosticsInitialized;
    private static volatile SmscSelectionConfig runtimeConfig = CONFIGURATION_REPOSITORY.buildDefaultConfig();

    private static final XC_MethodHook FORCE_SMSC_HOOK = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            HookSignatureRegistry.HookSignature signature = signatureFor(param);
            if (signature == null || param.args == null || param.args.length <= signature.smscArgumentIndex) {
                return;
            }

            RoutingSignals signals = ROUTING_SIGNAL_RESOLVER.resolve(param.thisObject);
            SmscSelector.SelectionResult selection = SmscSelector.selectSmscDetailed(
                    signals.slotIndex,
                    signals.mccMnc,
                    signals.carrierName,
                    runtimeConfig
            );
            if (!selection.replacementAuthorized || selection.smsc == null) {
                LOGGER.throttledDiagnostic(
                        "replacement_preserved",
                        "reason=" + selection.reason,
                        PRESERVED_DECISION_LOG_THROTTLE_MS
                );
                return;
            }

            Object original = param.args[signature.smscArgumentIndex];
            if (selection.smsc.equals(original)) {
                return;
            }
            param.args[signature.smscArgumentIndex] = selection.smsc;
            LOGGER.diagnostic("smsc_replaced", "reason=" + selection.reason);
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
            initializeRomDiagnostics();
        }

        boolean hookedSuccessfully = false;
        try {
            ConfigurationRepository.Snapshot snapshot = CONFIGURATION_REPOSITORY.load(romDiagnosticsEnabled);
            runtimeConfig = snapshot.selectionConfig;
            LOGGER.setDiagnosticsEnabled(snapshot.diagnosticsEnabled);
            if (!SmscRuntimeConfig.shouldHandlePackage(lpparam.packageName, runtimeConfig.targetPackages)) {
                LOGGER.diagnostic("package_skipped", "scope=not_targeted");
                return;
            }

            Class<?> smsManagerClass;
            try {
                smsManagerClass = XposedHelpers.findClass("android.telephony.SmsManager", lpparam.classLoader);
            } catch (Throwable error) {
                LOGGER.info("sms_manager_unavailable");
                return;
            }

            hookedSuccessfully = hookSupportedSendMethods(smsManagerClass) > 0;
            if (hookedSuccessfully) {
                LOGGER.info("hook_applied");
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

    private static void initializeRomDiagnostics() {
        if (romDiagnosticsInitialized) {
            return;
        }
        romDiagnosticsEnabled = detectRomDiagnosticsEnabled();
        romDiagnosticsInitialized = true;
        if (romDiagnosticsEnabled) {
            LOGGER.info("rom_diagnostics_enabled");
        }
    }

    private static HookSignatureRegistry.HookSignature signatureFor(XC_MethodHook.MethodHookParam param) {
        return param != null && param.method instanceof Method
                ? HookSignatureRegistry.match((Method) param.method)
                : null;
    }

    private static int hookSupportedSendMethods(Class<?> clazz) {
        int hookedCount = 0;
        Set<String> seenSignatures = new HashSet<>();
        for (Method method : clazz.getDeclaredMethods()) {
            HookSignatureRegistry.HookSignature signature = HookSignatureRegistry.match(method);
            if (signature == null || !seenSignatures.add(signature.diagnosticName())) {
                continue;
            }
            try {
                XposedBridge.hookMethod(method, FORCE_SMSC_HOOK);
                hookedCount++;
                LOGGER.diagnostic("hook_installed", "signature=" + signature.diagnosticName());
            } catch (Throwable error) {
                LOGGER.info("hook_install_failed");
                LOGGER.throttledFailure("hook:" + signature.methodName, error, 120_000L);
            }
        }
        LOGGER.info("hook_summary count=" + hookedCount);
        return hookedCount;
    }

    private static String buildHookScopeKey(XC_LoadPackage.LoadPackageParam lpparam) {
        String process = lpparam.processName == null ? "" : lpparam.processName;
        String pkg = lpparam.packageName == null ? "" : lpparam.packageName;
        int classLoaderId = lpparam.classLoader == null ? 0 : System.identityHashCode(lpparam.classLoader);
        return process + "|" + pkg + "|" + classLoaderId;
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
}
