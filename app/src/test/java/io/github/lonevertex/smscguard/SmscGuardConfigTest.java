package io.github.lonevertex.smscguard;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmscGuardConfigTest {
    @Test
    public void parsePackagesTrimsDeduplicatesAndIgnoresEmptyValues() {
        Set<String> parsed = SmscRuntimeConfig.parsePackages(" com.test.one,com.test.two, ,com.test.one ");
        assertEquals(new LinkedHashSet<>(Arrays.asList("com.test.one", "com.test.two")), parsed);
    }

    @Test
    public void parsePackagesEmptyReturnsEmptySet() {
        assertTrue(SmscRuntimeConfig.parsePackages("").isEmpty());
        assertTrue(SmscRuntimeConfig.parsePackages("   ").isEmpty());
        assertTrue(SmscRuntimeConfig.parsePackages(null).isEmpty());
    }

    @Test
    public void shouldHandlePackageAlwaysAllowsFrameworkButFailsClosedForEmptyTargets() {
        assertTrue(SmscRuntimeConfig.shouldHandlePackage("android", new LinkedHashSet<String>()));
        assertFalse(SmscRuntimeConfig.shouldHandlePackage("com.any.app", new LinkedHashSet<String>()));
        assertFalse(SmscRuntimeConfig.shouldHandlePackage("com.any.app", null));
    }

    @Test
    public void shouldHandlePackageMatchesOnlyTargetedPackages() {
        Set<String> targets = new LinkedHashSet<>(Arrays.asList("com.foo", "com.bar"));
        assertTrue(SmscRuntimeConfig.shouldHandlePackage("com.foo", targets));
        assertFalse(SmscRuntimeConfig.shouldHandlePackage("com.baz", targets));
    }

    @Test
    public void defaultConfigHasExpectedDefaults() {
        SmscSelectionConfig config = SmscRuntimeConfig.buildDefaultConfig("+111", "+222");
        assertEquals("+111", config.primarySmsc);
        assertEquals("+222", config.secondarySmsc);
        assertTrue(config.targetPackages.contains("com.google.android.apps.messaging"));
        assertTrue(config.targetPackages.contains("com.android.mms"));
        assertTrue(config.configVersion >= 1);
    }

    @Test
    public void schemaNormalizesSmscAndPackages() {
        assertEquals("+20105996500", SmscConfigSchema.normalizeSmscOrDefault("20105996500", "+fallback"));
        assertEquals("+fallback", SmscConfigSchema.normalizeSmscOrDefault("bad-smsc", "+fallback"));
        assertTrue(SmscConfigSchema.isTargetPackagesCsvAcceptable("com.good.one,com.good.two"));
        assertFalse(SmscConfigSchema.isTargetPackagesCsvAcceptable("com.good.one, bad package"));
    }

    @Test
    public void invalidOnlyRuntimeTargetsRecoverToDefaultScope() {
        Set<String> normalized = SmscConfigSchema.parseAndNormalizeTargetPackages("bad package,still bad");
        assertEquals(
                new LinkedHashSet<>(Arrays.asList(
                        "com.google.android.apps.messaging",
                        "com.android.mms"
                )),
                normalized
        );
        assertFalse(SmscConfigSchema.isTargetPackagesCsvAcceptable("bad package,still bad"));
    }

    @Test
    public void emptyRuntimeTargetsRecoverToDefaultScope() {
        Set<String> normalized = SmscConfigSchema.parseAndNormalizeTargetPackages("  ");
        assertTrue(normalized.contains("com.google.android.apps.messaging"));
        assertTrue(normalized.contains("com.android.mms"));
    }

    @Test
    public void mixedValidAndInvalidTargetsAreRejectedByTheUi() {
        assertFalse(SmscConfigSchema.isTargetPackagesCsvAcceptable("com.good.one,bad package"));
    }

    @Test
    public void configurationRepositoryLoadsDefaultWhenPrefsNull() {
        DiagnosticLogger logger = new DiagnosticLogger("TestTag");
        ConfigurationRepository repo = new ConfigurationRepository(
                "io.github.lonevertex.smscguard",
                "smscguard_prefs",
                "+20105996500",
                "+20122000020",
                logger
        );
        ConfigurationRepository.Snapshot snapshot = repo.load(null, false);
        assertEquals("+20105996500", snapshot.selectionConfig.primarySmsc);
        assertEquals("+20122000020", snapshot.selectionConfig.secondarySmsc);
        assertFalse(snapshot.diagnosticsEnabled);

        ConfigurationRepository.Snapshot romDiagSnapshot = repo.load(null, true);
        assertTrue(romDiagSnapshot.diagnosticsEnabled);
    }

    @Test
    public void configurationRepositoryLoadsFromSharedPreferences() {
        DiagnosticLogger logger = new DiagnosticLogger("TestTag");
        ConfigurationRepository repo = new ConfigurationRepository(
                "io.github.lonevertex.smscguard",
                "smscguard_prefs",
                "+fallbackPrimary",
                "+fallbackSecondary",
                logger
        );

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put(SmscConfigSchema.KEY_SCHEMA_VERSION, 2);
        data.put(SmscConfigSchema.KEY_PRIMARY_SMSC, "+20101111111");
        data.put(SmscConfigSchema.KEY_SECONDARY_SMSC, "+20122222222");
        data.put(SmscConfigSchema.KEY_TARGET_PACKAGES_CSV, "com.custom.sms");
        data.put(SmscConfigSchema.KEY_DIAGNOSTICS_ENABLED, true);

        android.content.SharedPreferences prefs = (android.content.SharedPreferences) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{android.content.SharedPreferences.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getInt".equals(name)) {
                        Object val = data.get(args[0]);
                        return val instanceof Integer ? val : args[1];
                    }
                    if ("getString".equals(name)) {
                        Object val = data.get(args[0]);
                        return val instanceof String ? val : args[1];
                    }
                    if ("getBoolean".equals(name)) {
                        Object val = data.get(args[0]);
                        return val instanceof Boolean ? val : args[1];
                    }
                    return null;
                }
        );

        ConfigurationRepository.Snapshot snapshot = repo.load(prefs, false);
        assertEquals("+20101111111", snapshot.selectionConfig.primarySmsc);
        assertEquals("+20122222222", snapshot.selectionConfig.secondarySmsc);
        assertTrue(snapshot.selectionConfig.targetPackages.contains("com.custom.sms"));
        assertTrue(snapshot.diagnosticsEnabled);
    }

    @Test
    public void xposedScopeArrayDeclaresCanonicalTargetPackages() throws Exception {
        java.io.File arraysFile = new java.io.File("src/main/res/values/arrays.xml");
        if (!arraysFile.exists()) {
            arraysFile = new java.io.File("app/src/main/res/values/arrays.xml");
        }
        assertTrue("arrays.xml must exist", arraysFile.exists());

        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(arraysFile);
        org.w3c.dom.NodeList arrayNodes = doc.getElementsByTagName("string-array");

        Set<String> scopeItems = new LinkedHashSet<>();
        for (int i = 0; i < arrayNodes.getLength(); i++) {
            org.w3c.dom.Element elem = (org.w3c.dom.Element) arrayNodes.item(i);
            if ("xposed_scope".equals(elem.getAttribute("name"))) {
                org.w3c.dom.NodeList items = elem.getElementsByTagName("item");
                for (int j = 0; j < items.getLength(); j++) {
                    scopeItems.add(items.item(j).getTextContent().trim());
                }
            }
        }

        assertTrue("xposed_scope must contain android", scopeItems.contains("android"));
        assertTrue("xposed_scope must contain com.google.android.apps.messaging",
                scopeItems.contains("com.google.android.apps.messaging"));
        assertTrue("xposed_scope must contain com.android.mms",
                scopeItems.contains("com.android.mms"));
        assertEquals(3, scopeItems.size());
    }
}
