package io.github.lonevertex.smscguard

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.IOException

class PreferencesManager(private val context: Context) {
    companion object {
        const val PREFS_NAME = "smscguard_prefs"
    }

    var isLsposedManaged: Boolean = false
        private set

    fun openPreferences(): SharedPreferences {
        return try {
            @Suppress("DEPRECATION")
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
            isLsposedManaged = true
            prefs
        } catch (ignored: SecurityException) {
            isLsposedManaged = false
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun ensureSchemaVersion(prefs: SharedPreferences) {
        if (prefs.getInt(SmscConfigSchema.KEY_SCHEMA_VERSION, 0) >= SmscConfigSchema.CURRENT_VERSION) {
            return
        }
        val normalizedPrimary = SmscConfigSchema.normalizeSmscOrDefault(
            prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, SmscGuard.DEFAULT_SMSC_PRIMARY),
            SmscGuard.DEFAULT_SMSC_PRIMARY
        )
        val normalizedSecondary = SmscConfigSchema.normalizeSmscOrDefault(
            prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, SmscGuard.DEFAULT_SMSC_SECONDARY),
            SmscGuard.DEFAULT_SMSC_SECONDARY
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
    }

    fun makePrefsReadableForXposed(): Boolean {
        if (isLsposedManaged) {
            return true
        }
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val prefsFile = File(prefsDir, "$PREFS_NAME.xml")
        if (!isSafePrefsPath(prefsDir, prefsFile) || !prefsFile.exists()) {
            return false
        }
        val readableBefore = prefsFile.canRead()
        return prefsFile.setReadable(true, false) || readableBefore
    }

    private fun isSafePrefsPath(prefsDir: File, prefsFile: File): Boolean {
        val dataDir = prefsDir.parentFile ?: return false
        return try {
            val dataDirPath = dataDir.canonicalPath
            val prefsDirPath = prefsDir.canonicalPath
            val prefsFilePath = prefsFile.canonicalPath
            prefsDirPath.startsWith(dataDirPath + File.separator) &&
                prefsFilePath.startsWith(prefsDirPath + File.separator)
        } catch (e: IOException) {
            false
        }
    }
}
