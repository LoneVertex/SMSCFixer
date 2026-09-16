package io.github.lonevertex.smscguard

import android.content.SharedPreferences
import android.os.Build
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Method
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class SmscGuardModule : XposedModule() {

    companion object {
        private const val TAG = "SmscGuard"
        const val DEFAULT_SMSC_PRIMARY = "+20105996500" // Vodafone Egypt
        const val DEFAULT_SMSC_SECONDARY = "+20122000020" // Orange Egypt
        const val PREFS_NAME = "smscguard_prefs"
        private const val PRESERVED_DECISION_LOG_THROTTLE_MS = 30_000L

        private val ROM_DIAGNOSTIC_KEYWORDS = arrayOf("a21s", "sm-a217", "infinity x", "infinityx")
        private val HOOKED_SCOPES = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
        private val LOGGER = DiagnosticLogger(TAG)
        private val CONFIGURATION_REPOSITORY = ConfigurationRepository(
            "io.github.lonevertex.smscguard",
            PREFS_NAME,
            DEFAULT_SMSC_PRIMARY,
            DEFAULT_SMSC_SECONDARY,
            LOGGER
        )
        private val ROUTING_SIGNAL_RESOLVER = RoutingSignalResolver(LOGGER)

        @Volatile
        private var romDiagnosticsEnabled = false
        @Volatile
        private var romDiagnosticsInitialized = false
        @Volatile
        private var runtimeConfig: SmscSelectionConfig = CONFIGURATION_REPOSITORY.buildDefaultConfig()
    }

    private var prefsChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    @Volatile
    private var processInitialized = false

    override fun onPackageReady(param: PackageReadyParam) {
        ensureProcessInitialized()

        if (!SmscRuntimeConfig.shouldHandlePackage(param.packageName, runtimeConfig.targetPackages)) {
            LOGGER.diagnostic("package_skipped", "scope=not_targeted")
            return
        }

        val scopeKey = "${param.packageName}|${System.identityHashCode(param.classLoader)}"
        if (HOOKED_SCOPES.contains(scopeKey)) {
            return
        }

        val smsManagerClass = runCatching {
            Class.forName("android.telephony.SmsManager", false, param.classLoader)
        }.getOrNull() ?: run {
            LOGGER.info("sms_manager_unavailable")
            return
        }

        val hookedCount = hookSupportedSendMethods(smsManagerClass)
        if (hookedCount > 0) {
            HOOKED_SCOPES.add(scopeKey)
        }
    }

    private fun ensureProcessInitialized() {
        if (processInitialized) return
        synchronized(this) {
            if (processInitialized) return
            initializeRomDiagnostics()

            val remotePrefs = runCatching { getRemotePreferences(PREFS_NAME) }.getOrNull()
            updateConfigFromPrefs(remotePrefs)

            remotePrefs?.let { prefs ->
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    updateConfigFromPrefs(prefs)
                }
                prefsChangeListener = listener
                prefs.registerOnSharedPreferenceChangeListener(listener)
            }
            processInitialized = true
        }
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        super.onHotReloaded(param)
        HOOKED_SCOPES.clear()
    }

    private fun updateConfigFromPrefs(prefs: SharedPreferences?) {
        val snapshot = CONFIGURATION_REPOSITORY.load(prefs, romDiagnosticsEnabled)
        runtimeConfig = snapshot.selectionConfig
        LOGGER.setDiagnosticsEnabled(snapshot.diagnosticsEnabled)
    }

    @android.annotation.SuppressLint("NewApi")
    private fun hookSupportedSendMethods(clazz: Class<*>): Int {
        var hookedCount = 0
        val seenSignatures = HashSet<String>()
        for (method in clazz.declaredMethods) {
            val signature = HookSignatureRegistry.match(method) ?: continue
            if (!seenSignatures.add(signature.diagnosticName())) continue

            try {
                hook(method).intercept { chain ->
                    if (chain.args.size <= signature.smscArgumentIndex) {
                        return@intercept chain.proceed()
                    }

                    val signals = ROUTING_SIGNAL_RESOLVER.resolve(chain.thisObject)
                    val selection = SmscSelector.selectSmscDetailed(
                        signals.slotIndex,
                        signals.mccMnc,
                        signals.carrierName,
                        runtimeConfig
                    )

                    if (!selection.replacementAuthorized || selection.smsc == null) {
                        LOGGER.throttledDiagnostic(
                            "replacement_preserved",
                            "reason=" + selection.reason,
                            PRESERVED_DECISION_LOG_THROTTLE_MS
                        )
                        return@intercept chain.proceed()
                    }

                    val original = chain.getArg(signature.smscArgumentIndex)
                    if (selection.smsc == original) {
                        return@intercept chain.proceed()
                    }

                    val newArgs = chain.args.toTypedArray()
                    newArgs[signature.smscArgumentIndex] = selection.smsc
                    LOGGER.diagnostic("smsc_replaced", "reason=" + selection.reason)
                    return@intercept chain.proceed(newArgs)
                }
                hookedCount++
                LOGGER.diagnostic("hook_installed", "signature=" + signature.diagnosticName())
            } catch (error: Throwable) {
                LOGGER.info("hook_install_failed")
                LOGGER.throttledFailure("hook:" + signature.methodName, error, 120_000L)
            }
        }
        LOGGER.info("hook_summary count=$hookedCount")
        return hookedCount
    }

    private fun initializeRomDiagnostics() {
        if (romDiagnosticsInitialized) return
        romDiagnosticsEnabled = detectRomDiagnosticsEnabled()
        romDiagnosticsInitialized = true
        if (romDiagnosticsEnabled) {
            LOGGER.info("rom_diagnostics_enabled")
        }
    }

    private fun detectRomDiagnosticsEnabled(): Boolean {
        val romInfo = (Build.BRAND + "|" + Build.MANUFACTURER + "|" + Build.MODEL + "|"
                + Build.DEVICE + "|" + Build.PRODUCT + "|" + Build.DISPLAY + "|"
                + Build.FINGERPRINT).lowercase(Locale.ROOT)
        for (keyword in ROM_DIAGNOSTIC_KEYWORDS) {
            if (romInfo.contains(keyword)) return true
        }
        return false
    }
}
