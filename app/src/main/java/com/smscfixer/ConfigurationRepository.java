package com.smscfixer;

import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

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

    private final String modulePackage;
    private final String prefsName;
    private final String defaultPrimarySmsc;
    private final String defaultSecondarySmsc;
    private final DiagnosticLogger logger;

    ConfigurationRepository(
            String modulePackage,
            String prefsName,
            String defaultPrimarySmsc,
            String defaultSecondarySmsc,
            DiagnosticLogger logger
    ) {
        this.modulePackage = modulePackage;
        this.prefsName = prefsName;
        this.defaultPrimarySmsc = defaultPrimarySmsc;
        this.defaultSecondarySmsc = defaultSecondarySmsc;
        this.logger = logger;
    }

    Snapshot load(boolean romDiagnosticsEnabled) {
        try {
            XSharedPreferences prefs = new XSharedPreferences(modulePackage, prefsName);
            if (prefs.getFile() == null || !prefs.getFile().canRead()) {
                logger.info("config_unreadable");
                return new Snapshot(buildDefaultConfig(), romDiagnosticsEnabled);
            }
            prefs.reload();
            int schemaVersion = prefs.getInt(
                    SmscConfigSchema.KEY_SCHEMA_VERSION,
                    SmscConfigSchema.CURRENT_VERSION
            );
            String primary = SmscConfigSchema.normalizeSmscOrDefault(
                    prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, defaultPrimarySmsc),
                    defaultPrimarySmsc
            );
            String secondary = SmscConfigSchema.normalizeSmscOrDefault(
                    prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, defaultSecondarySmsc),
                    defaultSecondarySmsc
            );
            Set<String> targets = SmscConfigSchema.parseAndNormalizeTargetPackages(
                    prefs.getString(
                            SmscConfigSchema.KEY_TARGET_PACKAGES_CSV,
                            SmscConfigSchema.DEFAULT_TARGET_PACKAGES_CSV
                    )
            );
            boolean diagnostics = romDiagnosticsEnabled
                    || prefs.getBoolean(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, false);
            SmscSelectionConfig config = SmscRuntimeConfig.buildConfig(
                    primary,
                    secondary,
                    targets,
                    schemaVersion
            );
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
