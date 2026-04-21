package com.smscfixer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

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
            String primary = safeValue(primaryEdit.getText() == null ? null : primaryEdit.getText().toString(),
                    SmscFixer.DEFAULT_SMSC_PRIMARY);
            String secondary = safeValue(secondaryEdit.getText() == null ? null : secondaryEdit.getText().toString(),
                    SmscFixer.DEFAULT_SMSC_SECONDARY);
            String targets = safeValue(targetPackagesEdit.getText() == null ? null : targetPackagesEdit.getText().toString(),
                    "com.google.android.apps.messaging,com.android.mms");

            prefs.edit()
                    .putString("primary_smsc", primary)
                    .putString("secondary_smsc", secondary)
                    .putString("target_packages_csv", targets)
                    .putBoolean("diagnostics_enabled", diagnosticsCheck.isChecked())
                    .apply();

            statusText.setText("Saved. Reboot device (or restart target apps) to reload hooks.");
        });
    }

    private static String safeValue(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
