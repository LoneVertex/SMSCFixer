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
                statusText.setText(R.string.settings_validation_invalid_smsc);
                return;
            }

            if (!SmscConfigSchema.isTargetPackagesCsvAcceptable(rawTargets)) {
                statusText.setText(R.string.settings_validation_invalid_packages);
                return;
            }

            String primary = SmscConfigSchema.normalizeSmscOrDefault(rawPrimary, SmscFixer.DEFAULT_SMSC_PRIMARY);
            String secondary = SmscConfigSchema.normalizeSmscOrDefault(rawSecondary, SmscFixer.DEFAULT_SMSC_SECONDARY);
            String targets = SmscConfigSchema.normalizeTargetPackagesCsv(rawTargets);

            boolean stored = prefs.edit()
                    .putInt(SmscConfigSchema.KEY_SCHEMA_VERSION, SmscConfigSchema.CURRENT_VERSION)
                    .putString(SmscConfigSchema.KEY_PRIMARY_SMSC, primary)
                    .putString(SmscConfigSchema.KEY_SECONDARY_SMSC, secondary)
                    .putString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, targets)
                    .putBoolean(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, diagnosticsCheck.isChecked())
                    .commit();

            boolean readable = makePrefsReadableForXposed();
            if (stored && readable) {
                statusText.setText(R.string.settings_saved_reboot);
            } else if (stored) {
                statusText.setText(R.string.settings_saved_readability_failed);
            } else {
                statusText.setText(R.string.settings_save_failed);
            }
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
            statusText.setText(R.string.settings_self_check_ok);
        } else {
            statusText.setText(R.string.settings_self_check_failed);
        }
    }

    private boolean makePrefsReadableForXposed() {
        File dataDir = new File(getApplicationInfo().dataDir);
        File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, PREFS_NAME + ".xml");
        if (!isSafePrefsPath(dataDir, prefsDir, prefsFile)) {
            return false;
        }
        boolean dataDirReadable = ensureWorldReadable(dataDir, true);
        boolean dirReadable = ensureWorldReadable(prefsDir, true);
        boolean fileReadable = ensureWorldReadable(prefsFile, false);
        return dataDirReadable && dirReadable && fileReadable;
    }

    private static boolean isSafePrefsPath(File dataDir, File prefsDir, File prefsFile) {
        try {
            String dataDirPath = dataDir.getCanonicalPath();
            String prefsDirPath = prefsDir.getCanonicalPath();
            String prefsFilePath = prefsFile.getCanonicalPath();
            return prefsDirPath.startsWith(dataDirPath + File.separator)
                    && prefsFilePath.startsWith(prefsDirPath + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean ensureWorldReadable(File path, boolean executable) {
        if (path == null || !path.exists()) {
            return false;
        }
        boolean readableBefore = path.canRead();
        boolean readableSet = path.setReadable(true, false);
        boolean readable = readableSet ? path.canRead() : readableBefore;
        if (!executable) {
            return readable;
        }
        boolean executableBefore = path.canExecute();
        boolean executableSet = path.setExecutable(true, false);
        boolean traversable = executableSet ? path.canExecute() : executableBefore;
        return readable && traversable;
    }
}
