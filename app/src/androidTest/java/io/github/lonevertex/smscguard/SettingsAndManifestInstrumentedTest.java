package io.github.lonevertex.smscguard;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SettingsAndManifestInstrumentedTest {

    @Test
    public void manifestDeclaresModernLibxposedProvider() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager packageManager = context.getPackageManager();
        String expectedAuthority = context.getPackageName() + ".XposedService";
        ComponentName providerComponent = new ComponentName(
                context.getPackageName(),
                "io.github.libxposed.service.XposedProvider"
        );

        ProviderInfo providerInfo = packageManager.getProviderInfo(providerComponent, 0);
        assertNotNull("XposedProvider must be registered in AndroidManifest", providerInfo);
        assertEquals("XposedProvider authority must match ${applicationId}.XposedService",
                expectedAuthority, providerInfo.authority);
        assertTrue("XposedProvider must be exported for modern framework binding",
                providerInfo.exported);
    }

    @Test
    public void manifestOmitsDeprecatedLegacyMetadata() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                context.getPackageName(),
                PackageManager.GET_META_DATA
        );

        if (applicationInfo.metaData != null) {
            assertFalse("Deprecated metadata xposedsharedprefs must be completely absent from <application>",
                    applicationInfo.metaData.containsKey("xposedsharedprefs"));
            assertFalse("Legacy metadata xposedmodule must be absent in modern libxposed API 102",
                    applicationInfo.metaData.containsKey("xposedmodule"));
            assertFalse("Legacy metadata xposedminversion must be absent in modern libxposed API 102",
                    applicationInfo.metaData.containsKey("xposedminversion"));
        }
    }

    @Test
    public void manifestDeclaresCanonicalXposedScope() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                context.getPackageName(),
                PackageManager.GET_META_DATA
        );

        assertNotNull("Application metadata must be present", applicationInfo.metaData);
        assertTrue("xposedscope meta-data must be declared in AndroidManifest",
                applicationInfo.metaData.containsKey("xposedscope"));

        int scopeResId = applicationInfo.metaData.getInt("xposedscope", 0);
        assertTrue("xposedscope must point to a valid resource ID", scopeResId != 0);

        String[] scopeArray = context.getResources().getStringArray(scopeResId);
        assertNotNull("xposedscope array must not be null", scopeArray);
        java.util.List<String> scopeList = java.util.Arrays.asList(scopeArray);

        assertTrue("xposedscope must contain android", scopeList.contains("android"));
        assertTrue("xposedscope must contain com.google.android.apps.messaging",
                scopeList.contains("com.google.android.apps.messaging"));
        assertTrue("xposedscope must contain com.android.mms",
                scopeList.contains("com.android.mms"));
    }

    @Test
    public void applicationHasDescriptionPopulated() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                context.getPackageName(),
                0
        );

        assertTrue("Application tag must declare android:description resource",
                applicationInfo.descriptionRes != 0);

        CharSequence description = applicationInfo.loadDescription(packageManager);
        assertNotNull("Application description must not be null", description);
        assertTrue("Application description must not be empty", description.length() > 0);
        assertEquals(context.getString(R.string.xposed_description), description.toString());
    }

    @Test
    public void packagedAssetsContainModernLibxposedFiles() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // 1. Verify existence and integrity inside installed APK if available
        String apkPath = context.getPackageCodePath();
        if (apkPath == null || !new File(apkPath).exists()) {
            apkPath = context.getApplicationInfo().publicSourceDir;
        }

        boolean verifiedViaApk = false;
        if (apkPath != null && new File(apkPath).exists()) {
            try (ZipFile zipFile = new ZipFile(apkPath)) {
                ZipEntry moduleProp = zipFile.getEntry("META-INF/xposed/module.prop");
                ZipEntry javaInit = zipFile.getEntry("META-INF/xposed/java_init.list");
                ZipEntry scopeList = zipFile.getEntry("META-INF/xposed/scope.list");

                assertNotNull("META-INF/xposed/module.prop must exist within the APK", moduleProp);
                assertNotNull("META-INF/xposed/java_init.list must exist within the APK", javaInit);
                assertNotNull("META-INF/xposed/scope.list must exist within the APK", scopeList);

                assertTrue("module.prop in APK must not be empty", moduleProp.getSize() > 0);
                assertTrue("java_init.list in APK must not be empty", javaInit.getSize() > 0);
                assertTrue("scope.list in APK must not be empty", scopeList.getSize() > 0);
                verifiedViaApk = true;
            }
        }

        // 2. Verify existence and content via ClassLoader resource stream
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream propStream = classLoader.getResourceAsStream("META-INF/xposed/module.prop")) {
            if (propStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(propStream, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                String content = sb.toString();
                assertTrue("module.prop must declare module id",
                        content.contains("id=io.github.lonevertex.smscguard"));
                assertTrue("module.prop must declare minApiVersion 102",
                        content.contains("minApiVersion=102"));
                assertTrue("module.prop must declare targetApiVersion 102",
                        content.contains("targetApiVersion=102"));
            } else {
                assertTrue("Assets must be verified via APK when resource stream is null", verifiedViaApk);
            }
        }

        try (InputStream initStream = classLoader.getResourceAsStream("META-INF/xposed/java_init.list")) {
            if (initStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(initStream, StandardCharsets.UTF_8));
                String line = reader.readLine();
                assertNotNull("java_init.list must not be empty", line);
                assertTrue("java_init.list must reference SmscGuardModule",
                        line.trim().contains("io.github.lonevertex.smscguard.SmscGuardModule"));
            } else {
                assertTrue("Assets must be verified via APK when resource stream is null", verifiedViaApk);
            }
        }

        try (InputStream scopeStream = classLoader.getResourceAsStream("META-INF/xposed/scope.list")) {
            if (scopeStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(scopeStream, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                String content = sb.toString();
                assertTrue("scope.list must include android",
                        content.contains("android"));
                assertTrue("scope.list must include com.google.android.apps.messaging",
                        content.contains("com.google.android.apps.messaging"));
                assertTrue("scope.list must include com.android.mms",
                        content.contains("com.android.mms"));
            } else {
                assertTrue("Assets must be verified via APK when resource stream is null", verifiedViaApk);
            }
        }
    }

    @Test
    public void settingsActivityIsExported() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager packageManager = context.getPackageManager();
        ActivityInfo activityInfo = packageManager.getActivityInfo(
                new ComponentName(context, SettingsActivity.class),
                0
        );
        assertTrue(activityInfo.exported);
    }

    @Test
    public void settingsComposeDashboardLaunchesWithViewModel() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity);
                assertNotNull(activity.getViewModel());
                assertNotNull(activity.getViewModel().getUiState().getValue());
                assertEquals(SmscGuardModule.DEFAULT_SMSC_PRIMARY, activity.getViewModel().getUiState().getValue().getPrimarySmsc());
                assertEquals(SmscGuardModule.DEFAULT_SMSC_SECONDARY, activity.getViewModel().getUiState().getValue().getSecondarySmsc());
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
