package com.smscfixer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class SettingsActivity extends Activity {
    private static final String PREFS_NAME = "smscfixer_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ensureSchemaVersion(prefs);

        EditText primaryEdit = findViewById(R.id.primarySmscInput);
        EditText secondaryEdit = findViewById(R.id.secondarySmscInput);
        EditText targetPackagesEdit = findViewById(R.id.targetPackagesInput);
        CheckBox diagnosticsCheck = findViewById(R.id.diagnosticsCheck);
        TextView statusText = findViewById(R.id.statusText);
        Button saveButton = findViewById(R.id.saveButton);
        Button selfCheckButton = findViewById(R.id.selfCheckButton);

        primaryEdit.setText(prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, SmscFixer.DEFAULT_SMSC_PRIMARY));
        secondaryEdit.setText(prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, SmscFixer.DEFAULT_SMSC_SECONDARY));
        targetPackagesEdit.setText(prefs.getString(
                SmscConfigSchema.KEY_TARGET_PACKAGES_CSV,
                SmscConfigSchema.DEFAULT_TARGET_PACKAGES_CSV
        ));
        diagnosticsCheck.setChecked(prefs.getBoolean(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, false));

        saveButton.setOnClickListener(v -> {
            String rawPrimary = primaryEdit.getText().toString();
            String rawSecondary = secondaryEdit.getText().toString();
            String rawTargets = targetPackagesEdit.getText().toString();
            if (!SmscConfigSchema.isSmscInputAcceptable(rawPrimary)
                    || !SmscConfigSchema.isSmscInputAcceptable(rawSecondary)) {
                announceStatus(statusText, R.string.settings_validation_invalid_smsc);
                return;
            }
            if (!SmscConfigSchema.isTargetPackagesCsvAcceptable(rawTargets)) {
                announceStatus(statusText, R.string.settings_validation_invalid_packages);
                return;
            }

            String primary = SmscConfigSchema.normalizeSmscOrDefault(rawPrimary, SmscFixer.DEFAULT_SMSC_PRIMARY);
            String secondary = SmscConfigSchema.normalizeSmscOrDefault(rawSecondary, SmscFixer.DEFAULT_SMSC_SECONDARY);
            String targets = SmscConfigSchema.normalizeTargetPackagesCsv(rawTargets);
            boolean diagnostics = diagnosticsCheck.isChecked();

            saveButton.setEnabled(false);
            announceStatus(statusText, R.string.settings_saving);
            new Thread(() -> {
                boolean stored = prefs.edit()
                        .putInt(SmscConfigSchema.KEY_SCHEMA_VERSION, SmscConfigSchema.CURRENT_VERSION)
                        .putString(SmscConfigSchema.KEY_PRIMARY_SMSC, primary)
                        .putString(SmscConfigSchema.KEY_SECONDARY_SMSC, secondary)
                        .putString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, targets)
                        .putBoolean(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, diagnostics)
                        .commit();
                boolean readable = stored && makePrefsReadableForXposed();
                runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    if (stored && readable) {
                        announceStatus(statusText, R.string.settings_saved_reboot);
                    } else if (stored) {
                        announceStatus(statusText, R.string.settings_saved_readability_failed);
                    } else {
                        announceStatus(statusText, R.string.settings_save_failed);
                    }
                });
            }, "SmscFixerSettingsSave").start();
        });

        selfCheckButton.setOnClickListener(v -> runSelfCheck(prefs, statusText));
    }

    private static void ensureSchemaVersion(SharedPreferences prefs) {
        if (prefs.getInt(SmscConfigSchema.KEY_SCHEMA_VERSION, 0) >= SmscConfigSchema.CURRENT_VERSION) {
            return;
        }
        String normalizedPrimary = SmscConfigSchema.normalizeSmscOrDefault(
                prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, SmscFixer.DEFAULT_SMSC_PRIMARY),
                SmscFixer.DEFAULT_SMSC_PRIMARY
        );
        String normalizedSecondary = SmscConfigSchema.normalizeSmscOrDefault(
                prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, SmscFixer.DEFAULT_SMSC_SECONDARY),
                SmscFixer.DEFAULT_SMSC_SECONDARY
        );
        String normalizedTargets = SmscConfigSchema.normalizeTargetPackagesCsv(
                prefs.getString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, SmscConfigSchema.DEFAULT_TARGET_PACKAGES_CSV)
        );
        prefs.edit()
                .putInt(SmscConfigSchema.KEY_SCHEMA_VERSION, SmscConfigSchema.CURRENT_VERSION)
                .putString(SmscConfigSchema.KEY_PRIMARY_SMSC, normalizedPrimary)
                .putString(SmscConfigSchema.KEY_SECONDARY_SMSC, normalizedSecondary)
                .putString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, normalizedTargets)
                .apply();
    }

    private static void runSelfCheck(SharedPreferences prefs, TextView statusText) {
        int schemaVersion = prefs.getInt(SmscConfigSchema.KEY_SCHEMA_VERSION, 0);
        String primary = prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, "");
        String secondary = prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, "");
        String targetsCsv = prefs.getString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, "");

        boolean schemaOk = schemaVersion >= SmscConfigSchema.CURRENT_VERSION;
        boolean smscOk = SmscConfigSchema.isSmscFormatValid(primary) && SmscConfigSchema.isSmscFormatValid(secondary);
        boolean packagesOk = SmscConfigSchema.isTargetPackagesCsvAcceptable(targetsCsv);
        Set<String> parsedTargets = SmscConfigSchema.parseAndNormalizeTargetPackages(targetsCsv);
        boolean targetScopeOk = !parsedTargets.isEmpty();
        if (schemaOk && smscOk && packagesOk && targetScopeOk) {
            announceStatus(statusText, R.string.settings_self_check_ok);
        } else {
            announceStatus(statusText, R.string.settings_self_check_failed);
        }
    }

    /**
     * XSharedPreferences needs a readable XML file on older LSPosed-compatible environments.
     * Deliberately avoid changing the application data directory or shared_prefs directory,
     * because broad directory traversal is an unnecessary expansion of the exposure boundary.
     */
    private boolean makePrefsReadableForXposed() {
        File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, PREFS_NAME + ".xml");
        if (!isSafePrefsPath(prefsDir, prefsFile) || !prefsFile.exists()) {
            return false;
        }
        boolean readableBefore = prefsFile.canRead();
        return prefsFile.setReadable(true, false) || readableBefore;
    }

    private static boolean isSafePrefsPath(File prefsDir, File prefsFile) {
        File dataDir = prefsDir.getParentFile();
        if (dataDir == null) {
            return false;
        }
        try {
            String dataDirPath = dataDir.getCanonicalPath();
            String prefsDirPath = prefsDir.getCanonicalPath();
            String prefsFilePath = prefsFile.getCanonicalPath();
            return prefsDirPath.startsWith(dataDirPath + File.separator)
                    && prefsFilePath.startsWith(prefsDirPath + File.separator);
        } catch (IOException error) {
            return false;
        }
    }

    private static void announceStatus(TextView statusText, int messageResId) {
        statusText.setText(messageResId);
        statusText.announceForAccessibility(statusText.getText());
    }
}
