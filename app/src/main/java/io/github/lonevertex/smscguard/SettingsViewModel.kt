package io.github.lonevertex.smscguard

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

sealed class UiStatus {
    data class ResourceMessage(@StringRes val resId: Int, val isError: Boolean = false) : UiStatus()
    data class TextMessage(val message: String, val isError: Boolean = false) : UiStatus()
}

data class SettingsUiState(
    val primarySmsc: String = SmscGuardModule.DEFAULT_SMSC_PRIMARY,
    val isPrimaryValid: Boolean = true,
    val secondarySmsc: String = SmscGuardModule.DEFAULT_SMSC_SECONDARY,
    val isSecondaryValid: Boolean = true,
    val targetPackages: Set<String> = setOf(
        "com.google.android.apps.messaging",
        "com.android.mms"
    ),
    val customPackageInput: String = "",
    val isCustomPackageValid: Boolean = true,
    val isDefaultScopeActive: Boolean = true,
    val diagnosticsEnabled: Boolean = false,
    val lsposedManagedPreferences: Boolean = false,
    val isLsposedBound: Boolean = false,
    val frameworkInfo: String? = null,
    val isSaving: Boolean = false,
    val status: UiStatus? = null,
    val sanitizedRoutingDecision: String? = null,
    val simulationStep: Int = 0,
    val appVersion: String = "v2.0.0",
    val isReady: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val prefs = preferencesManager.openPreferences()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    companion object {
        private val E164_PATTERN = Pattern.compile("^\\+[0-9]{5,20}$")
        val DEFAULT_PACKAGES = setOf("com.google.android.apps.messaging", "com.android.mms")
        const val DEFAULT_PRIMARY = SmscGuardModule.DEFAULT_SMSC_PRIMARY
        const val DEFAULT_SECONDARY = SmscGuardModule.DEFAULT_SMSC_SECONDARY
    }

    init {
        preferencesManager.ensureSchemaVersion(prefs)
        loadInitialPreferences()
        observeFrameworkTelemetry()
    }

    private fun observeFrameworkTelemetry() {
        viewModelScope.launch {
            PreferencesManager.isLsposedBound.collect { bound ->
                _uiState.update { it.copy(isLsposedBound = bound, lsposedManagedPreferences = bound) }
            }
        }
        viewModelScope.launch {
            PreferencesManager.frameworkInfo.collect { info ->
                _uiState.update { it.copy(frameworkInfo = info) }
            }
        }
    }

    private fun loadInitialPreferences() {
        val primary = prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, DEFAULT_PRIMARY) ?: DEFAULT_PRIMARY
        val secondary = prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, DEFAULT_SECONDARY) ?: DEFAULT_SECONDARY
        val targetsCsv = prefs.getString(
            SmscConfigSchema.KEY_TARGET_PACKAGES_CSV,
            SmscConfigSchema.DEFAULT_TARGET_PACKAGES_CSV
        ) ?: SmscConfigSchema.DEFAULT_TARGET_PACKAGES_CSV
        val diagnostics = prefs.getBoolean(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, false)

        val parsedPackages = SmscConfigSchema.parseAndNormalizeTargetPackages(targetsCsv)
        val isDefaultScope = parsedPackages == DEFAULT_PACKAGES

        _uiState.update { state ->
            state.copy(
                primarySmsc = primary,
                isPrimaryValid = isStrictE164(primary),
                secondarySmsc = secondary,
                isSecondaryValid = isStrictE164(secondary),
                targetPackages = parsedPackages,
                isDefaultScopeActive = isDefaultScope,
                diagnosticsEnabled = diagnostics,
                lsposedManagedPreferences = preferencesManager.isLsposedManaged,
                isLsposedBound = PreferencesManager.isLsposedBound.value,
                frameworkInfo = PreferencesManager.frameworkInfo.value,
                status = UiStatus.ResourceMessage(R.string.settings_status_initial, isError = false)
            )
        }
    }

    fun onPrimarySmscChanged(value: String) {
        _uiState.update { it.copy(primarySmsc = value, isPrimaryValid = isStrictE164(value)) }
    }

    fun onSecondarySmscChanged(value: String) {
        _uiState.update { it.copy(secondarySmsc = value, isSecondaryValid = isStrictE164(value)) }
    }

    fun resetPrimaryToDefault() {
        _uiState.update {
            it.copy(
                primarySmsc = DEFAULT_PRIMARY,
                isPrimaryValid = true,
                status = UiStatus.TextMessage("Slot 0 reset to Vodafone Egypt default ($DEFAULT_PRIMARY)")
            )
        }
    }

    fun resetSecondaryToDefault() {
        _uiState.update {
            it.copy(
                secondarySmsc = DEFAULT_SECONDARY,
                isSecondaryValid = true,
                status = UiStatus.TextMessage("Slot 1 reset to Orange Egypt default ($DEFAULT_SECONDARY)")
            )
        }
    }

    fun onCustomPackageInputChanged(value: String) {
        val trimmed = value.trim()
        val isValid = trimmed.isEmpty() || SmscConfigSchema.isTargetPackagesCsvAcceptable(trimmed)
        _uiState.update {
            it.copy(customPackageInput = value, isCustomPackageValid = isValid)
        }
    }

    fun addCustomPackage() {
        val input = _uiState.value.customPackageInput.trim()
        if (input.isEmpty() || !SmscConfigSchema.isTargetPackagesCsvAcceptable(input)) {
            _uiState.update {
                it.copy(
                    isCustomPackageValid = false,
                    status = UiStatus.ResourceMessage(R.string.settings_validation_invalid_packages, isError = true)
                )
            }
            return
        }

        val updated = LinkedHashSet(_uiState.value.targetPackages).apply { add(input) }
        _uiState.update {
            it.copy(
                targetPackages = updated,
                customPackageInput = "",
                isCustomPackageValid = true,
                isDefaultScopeActive = updated == DEFAULT_PACKAGES,
                status = UiStatus.TextMessage("Package added: $input")
            )
        }
    }

    fun toggleTargetPackage(pkg: String) {
        val current = _uiState.value.targetPackages
        val updated = if (current.contains(pkg)) {
            if (current.size == 1) {
                // Prevent empty scope: fall back to default
                DEFAULT_PACKAGES
            } else {
                current - pkg
            }
        } else {
            current + pkg
        }

        _uiState.update {
            it.copy(
                targetPackages = updated,
                isDefaultScopeActive = updated == DEFAULT_PACKAGES
            )
        }
    }

    fun onDiagnosticsChanged(enabled: Boolean) {
        _uiState.update { it.copy(diagnosticsEnabled = enabled) }
    }

    fun saveSettings() {
        val state = _uiState.value
        if (!isStrictE164(state.primarySmsc) || !isStrictE164(state.secondarySmsc)) {
            _uiState.update {
                it.copy(
                    isPrimaryValid = isStrictE164(state.primarySmsc),
                    isSecondaryValid = isStrictE164(state.secondarySmsc),
                    status = UiStatus.ResourceMessage(R.string.settings_validation_invalid_smsc, isError = true)
                )
            }
            return
        }

        val targetsCsv = state.targetPackages.joinToString(",")
        if (!SmscConfigSchema.isTargetPackagesCsvAcceptable(targetsCsv)) {
            _uiState.update {
                it.copy(
                    status = UiStatus.ResourceMessage(R.string.settings_validation_invalid_packages, isError = true)
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                status = UiStatus.ResourceMessage(R.string.settings_saving)
            )
        }

        viewModelScope.launch {
            val (stored, readable) = withContext(Dispatchers.IO) {
                val stored = prefs.edit()
                    .putInt(SmscConfigSchema.KEY_SCHEMA_VERSION, SmscConfigSchema.CURRENT_VERSION)
                    .putString(SmscConfigSchema.KEY_PRIMARY_SMSC, state.primarySmsc.trim())
                    .putString(SmscConfigSchema.KEY_SECONDARY_SMSC, state.secondarySmsc.trim())
                    .putString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, targetsCsv)
                    .putBoolean(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, state.diagnosticsEnabled)
                    .commit()
                preferencesManager.syncToRemote(prefs)
                val readable = stored && preferencesManager.makePrefsReadableForXposed()
                Pair(stored, readable)
            }

            _uiState.update {
                it.copy(
                    isSaving = false,
                    status = when {
                        stored && readable -> UiStatus.ResourceMessage(R.string.settings_saved_reboot, isError = false)
                        stored -> UiStatus.ResourceMessage(R.string.settings_saved_readability_failed, isError = true)
                        else -> UiStatus.ResourceMessage(R.string.settings_save_failed, isError = true)
                    }
                )
            }
        }
    }

    fun savePreferences() = saveSettings()

    fun runSelfCheck() {
        val schemaVersion = prefs.getInt(SmscConfigSchema.KEY_SCHEMA_VERSION, 0)
        val primary = prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, "") ?: ""
        val secondary = prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, "") ?: ""
        val targetsCsv = prefs.getString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, "") ?: ""

        val schemaOk = schemaVersion >= SmscConfigSchema.CURRENT_VERSION
        val smscOk = SmscConfigSchema.isSmscFormatValid(primary) && SmscConfigSchema.isSmscFormatValid(secondary)
        val packagesOk = SmscConfigSchema.isTargetPackagesCsvAcceptable(targetsCsv)
        val parsedTargets = SmscConfigSchema.parseAndNormalizeTargetPackages(targetsCsv)
        val targetScopeOk = parsedTargets.isNotEmpty()

        val isPassing = schemaOk && smscOk && packagesOk && targetScopeOk
        _uiState.update {
            it.copy(
                status = if (isPassing) {
                    UiStatus.ResourceMessage(R.string.settings_self_check_ok, isError = false)
                } else {
                    UiStatus.ResourceMessage(R.string.settings_self_check_failed, isError = true)
                }
            )
        }
    }

    fun triggerSanitizedRoutingTest() {
        val step = (_uiState.value.simulationStep + 1) % 3
        val simulatedConfig: SmscSelectionConfig = SmscRuntimeConfig.buildConfig(
            _uiState.value.primarySmsc,
            _uiState.value.secondarySmsc,
            _uiState.value.targetPackages,
            SmscConfigSchema.CURRENT_VERSION
        )

        val decision = when (step) {
            0 -> {
                val result = SmscSelector.selectSmscDetailed(0, "60202", "Vodafone Egypt", simulatedConfig)
                "Simulated Send: Slot 0 (Vodafone EG) -> reason=${result.reason} [REPLACED]"
            }
            1 -> {
                val result = SmscSelector.selectSmscDetailed(1, "60201", "Orange Egypt", simulatedConfig)
                "Simulated Send: Slot 1 (Orange EG) -> reason=${result.reason} [REPLACED]"
            }
            else -> {
                val result = SmscSelector.selectSmscDetailed(-1, "", "", simulatedConfig)
                "Simulated Send: Unknown Signals -> reason=${result.reason} [PRESERVED]"
            }
        }

        _uiState.update {
            it.copy(
                simulationStep = step,
                sanitizedRoutingDecision = decision,
                status = UiStatus.TextMessage("Routing verified: $decision")
            )
        }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(status = null) }
    }

    private fun isStrictE164(value: String): Boolean {
        return E164_PATTERN.matcher(value.trim()).matches()
    }
}
