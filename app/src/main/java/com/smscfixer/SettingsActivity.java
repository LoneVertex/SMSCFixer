package com.smscfixer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

public class SettingsActivity extends Activity {
    private static final String PREFS_NAME = "smscfixer_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        EditText primaryEdit = findViewById(R.id.primarySmscInput);
        EditText secondaryEdit = findViewById(R.id.secondarySmscInput);
        EditText targetPackagesEdit = findViewById(R.id.targetPackagesInput);
        CheckBox diagnosticsCheck = findViewById(R.id.diagnosticsCheck);
        TextView statusText = findViewById(R.id.statusText);
        Button saveButton = findViewById(R.id.saveButton);

        primaryEdit.setText(prefs.getString("primary_smsc", SmscFixer.DEFAULT_SMSC_PRIMARY));
        secondaryEdit.setText(prefs.getString("secondary_smsc", SmscFixer.DEFAULT_SMSC_SECONDARY));
        targetPackagesEdit.setText(prefs.getString("target_packages_csv",
                "com.google.android.apps.messaging,com.android.mms"));
        diagnosticsCheck.setChecked(prefs.getBoolean("diagnostics_enabled", false));

        saveButton.setOnClickListener(v -> {
            String primary = safeValue(primaryEdit.getText().toString(),
                    SmscFixer.DEFAULT_SMSC_PRIMARY);
            String secondary = safeValue(secondaryEdit.getText().toString(),
                    SmscFixer.DEFAULT_SMSC_SECONDARY);
            String targets = safeValue(targetPackagesEdit.getText().toString(),
                    "com.google.android.apps.messaging,com.android.mms");

            boolean stored = prefs.edit()
                    .putString("primary_smsc", primary)
                    .putString("secondary_smsc", secondary)
                    .putString("target_packages_csv", targets)
                    .putBoolean("diagnostics_enabled", diagnosticsCheck.isChecked())
                    .commit();

            boolean readable = makePrefsReadableForXposed();
            if (stored && readable) {
                statusText.setText("Saved. Reboot device (or restart target apps) to reload hooks.");
            } else if (stored) {
                statusText.setText("Saved, but prefs file readability update failed for Xposed.");
            } else {
                statusText.setText("Save failed.");
            }
        });
    }

    private static String safeValue(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private boolean makePrefsReadableForXposed() {
        File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, PREFS_NAME + ".xml");
        boolean dirReadable = prefsDir.exists() && prefsDir.setReadable(true, false);
        boolean fileReadable = prefsFile.exists() && prefsFile.setReadable(true, false);
        return dirReadable && fileReadable;
    }
}
