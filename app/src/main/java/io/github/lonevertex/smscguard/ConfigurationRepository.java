package io.github.lonevertex.smscguard;

import android.content.SharedPreferences;
import java.util.Set;

/** Loads and validates the cross-process configuration consumed by the hook. */
final class ConfigurationRepository {
    static final class Snapshot {
        final SmscSelectionConfig selectionConfig;
        final boolean diagnosticsEnabled;

        Snapshot(SmscSelectionConfig selectionConfig, boolean diagnosticsEnabled) {
            this.selectionConfig = selectionConfig;
            this.diagnosticsEnabled = diagnosticsEnabled;
        }
    }

    private final String defaultPrimarySmsc;
    private final String defaultSecondarySmsc;
    private final DiagnosticLogger logger;

    ConfigurationRepository(
            String defaultPrimarySmsc,
            String defaultSecondarySmsc,
            DiagnosticLogger logger
    ) {
        this.defaultPrimarySmsc = defaultPrimarySmsc;
        this.defaultSecondarySmsc = defaultSecondarySmsc;
        this.logger = logger;
    }

    Snapshot load(SharedPreferences prefs, boolean romDiagnosticsEnabled) {
        if (prefs == null) {
            return new Snapshot(buildDefaultConfig(), romDiagnosticsEnabled);
        }
        try {
            int schemaVersion = prefs.getInt(SmscConfigSchema.KEY_SCHEMA_VERSION, SmscConfigSchema.CURRENT_VERSION);
            String primary = SmscConfigSchema.normalizeSmscOrDefault(
                    prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, defaultPrimarySmsc),
                    defaultPrimarySmsc
            );
            String secondary = SmscConfigSchema.normalizeSmscOrDefault(
                    prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, defaultSecondarySmsc),
                    defaultSecondarySmsc
            );
            Set<String> targets = SmscConfigSchema.parseAndNormalizeTargetPackages(
                    prefs.getString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, SmscConfigSchema.DEFAULT_TARGET_PACKAGES_CSV)
            );
            boolean diagnostics = romDiagnosticsEnabled || prefs.getBoolean(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, false);
            SmscSelectionConfig config = SmscRuntimeConfig.buildConfig(primary, secondary, targets, schemaVersion);
            return new Snapshot(config, diagnostics);
        } catch (Throwable error) {
            logger.info("config_load_failed");
            return new Snapshot(buildDefaultConfig(), romDiagnosticsEnabled);
        }
    }

    SmscSelectionConfig buildDefaultConfig() {
        return SmscRuntimeConfig.buildDefaultConfig(defaultPrimarySmsc, defaultSecondarySmsc);
    }
}
