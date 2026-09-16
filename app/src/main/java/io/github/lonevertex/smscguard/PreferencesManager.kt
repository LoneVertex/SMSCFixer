package io.github.lonevertex.smscguard

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class PreferencesManager(private val context: Context) {
    companion object {
        const val PREFS_NAME = "smscguard_prefs"

        @Volatile
        private var sharedService: XposedService? = null

        private val listenerRegistered = AtomicBoolean(false)

        private val _isLsposedBound = MutableStateFlow(false)
        val isLsposedBound: StateFlow<Boolean> = _isLsposedBound.asStateFlow()

        private val _frameworkInfo = MutableStateFlow<String?>(null)
        val frameworkInfo: StateFlow<String?> = _frameworkInfo.asStateFlow()

        private val serviceListener = object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                sharedService = service
                _isLsposedBound.value = true
                _frameworkInfo.value = runCatching {
                    "${service.frameworkName} ${service.frameworkVersion} (${service.frameworkVersionCode})"
                }.getOrNull()
            }

            override fun onServiceDied(service: XposedService) {
                sharedService = null
                _isLsposedBound.value = false
                _frameworkInfo.value = null
            }
        }

        internal fun updateFrameworkStatusForTesting(bound: Boolean, info: String? = null) {
            _isLsposedBound.value = bound
            _frameworkInfo.value = if (bound) info else null
        }
    }

    init {
        if (listenerRegistered.compareAndSet(false, true)) {
            XposedServiceHelper.registerListener(serviceListener)
        }
        if (sharedService != null) {
            _isLsposedBound.value = true
        }
    }

    val isLsposedBound: StateFlow<Boolean>
        get() = PreferencesManager.isLsposedBound

    val frameworkInfo: StateFlow<String?>
        get() = PreferencesManager.frameworkInfo

    fun openPreferences(): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun ensureSchemaVersion(prefs: SharedPreferences) {
        if (prefs.getInt(SmscConfigSchema.KEY_SCHEMA_VERSION, 0) >= SmscConfigSchema.CURRENT_VERSION) {
            return
        }
        val normalizedPrimary = SmscConfigSchema.normalizeSmscOrDefault(
            prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, SmscGuardModule.DEFAULT_SMSC_PRIMARY),
            SmscGuardModule.DEFAULT_SMSC_PRIMARY
        )
        val normalizedSecondary = SmscConfigSchema.normalizeSmscOrDefault(
            prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, SmscGuardModule.DEFAULT_SMSC_SECONDARY),
            SmscGuardModule.DEFAULT_SMSC_SECONDARY
        )
        val normalizedTargets = SmscConfigSchema.normalizeTargetPackagesCsv(
            prefs.getString(
                SmscConfigSchema.KEY_TARGET_PACKAGES_CSV,
                SmscConfigSchema.DEFAULT_TARGET_PACKAGES_CSV
            )
        )
        prefs.edit()
            .putInt(SmscConfigSchema.KEY_SCHEMA_VERSION, SmscConfigSchema.CURRENT_VERSION)
            .putString(SmscConfigSchema.KEY_PRIMARY_SMSC, normalizedPrimary)
            .putString(SmscConfigSchema.KEY_SECONDARY_SMSC, normalizedSecondary)
            .putString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, normalizedTargets)
            .apply()
        syncToRemote(prefs)
    }

    fun syncToRemote(prefs: SharedPreferences) {
        val service = sharedService ?: return
        runCatching {
            service.getRemotePreferences(PREFS_NAME).edit {
                prefs.all.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is String -> putString(key, value)
                        else -> remove(key)
                    }
                }
            }
        }
    }

    @Deprecated("Superseded by isLsposedBound in API 102", ReplaceWith("isLsposedBound.value"))
    val isLsposedManaged: Boolean
        get() = isLsposedBound.value

    @Deprecated("Superseded by syncToRemote in API 102", ReplaceWith("syncToRemote(openPreferences())"))
    fun makePrefsReadableForXposed(): Boolean {
        syncToRemote(openPreferences())
        return true
    }
}
