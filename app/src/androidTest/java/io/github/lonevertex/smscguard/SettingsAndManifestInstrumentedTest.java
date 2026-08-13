package io.github.lonevertex.smscguard;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SettingsAndManifestInstrumentedTest {
    @Test
    public void manifestDeclaresExpectedLsposedMetadata() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                context.getPackageName(),
                PackageManager.GET_META_DATA
        );

        assertNotNull(applicationInfo.metaData);
        assertTrue(applicationInfo.metaData.getBoolean("xposedmodule"));
        assertTrue(applicationInfo.metaData.getBoolean("xposedsharedprefs"));
        assertEquals("82", applicationInfo.metaData.getString("xposedminversion"));

        ActivityInfo activityInfo = packageManager.getActivityInfo(
                new ComponentName(context, SettingsActivity.class),
                0
        );
        assertTrue(activityInfo.exported);
    }

    @Test
    public void settingsRedesignPreservesControlsAndAccessibleStatus() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            scenario.onActivity(activity -> {
                EditText primary = activity.findViewById(R.id.primarySmscInput);
                EditText secondary = activity.findViewById(R.id.secondarySmscInput);
                EditText targets = activity.findViewById(R.id.targetPackagesInput);
                CheckBox diagnostics = activity.findViewById(R.id.diagnosticsCheck);
                Button save = activity.findViewById(R.id.saveButton);
                Button selfCheck = activity.findViewById(R.id.selfCheckButton);
                TextView status = activity.findViewById(R.id.statusText);

                assertNotNull(primary);
                assertNotNull(secondary);
                assertNotNull(targets);
                assertNotNull(diagnostics);
                assertNotNull(save);
                assertNotNull(selfCheck);
                assertNotNull(status);
                assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, status.getAccessibilityLiveRegion());
                assertEquals(activity.getString(R.string.settings_save), save.getText().toString());
                assertEquals(activity.getString(R.string.settings_self_check_action), selfCheck.getText().toString());
                assertEquals(activity.getString(R.string.settings_status_initial), status.getText().toString());
            });
        }
    }

    @Test
    public void validatedConfigurationRoundTripsThroughPrivatePreferences() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("instrumentation_config", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();

        String primary = SmscConfigSchema.normalizeSmscOrDefault("20105996500", "+fallback");
        String secondary = SmscConfigSchema.normalizeSmscOrDefault("+20122000020", "+fallback");
        String targets = SmscConfigSchema.normalizeTargetPackagesCsv("com.example.one,com.example.two");
        boolean stored = prefs.edit()
                .putInt(SmscConfigSchema.KEY_SCHEMA_VERSION, SmscConfigSchema.CURRENT_VERSION)
                .putString(SmscConfigSchema.KEY_PRIMARY_SMSC, primary)
                .putString(SmscConfigSchema.KEY_SECONDARY_SMSC, secondary)
                .putString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, targets)
                .commit();

        assertTrue(stored);
        assertEquals(SmscConfigSchema.CURRENT_VERSION,
                prefs.getInt(SmscConfigSchema.KEY_SCHEMA_VERSION, 0));
        assertEquals("+20105996500", prefs.getString(SmscConfigSchema.KEY_PRIMARY_SMSC, ""));
        assertEquals("+20122000020", prefs.getString(SmscConfigSchema.KEY_SECONDARY_SMSC, ""));
        assertEquals("com.example.one,com.example.two",
                prefs.getString(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, ""));
    }

    @Test
    public void invalidTargetListIsRejectedBeforePersistence() {
        assertFalse(SmscConfigSchema.isTargetPackagesCsvAcceptable("com.example.good,bad package"));
        assertTrue(SmscConfigSchema.parseAndNormalizeTargetPackages("bad package")
                .contains("com.google.android.apps.messaging"));
    }
}
