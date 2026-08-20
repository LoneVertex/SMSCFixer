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
}
