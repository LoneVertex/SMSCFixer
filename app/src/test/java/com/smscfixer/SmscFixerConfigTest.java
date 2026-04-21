package com.smscfixer;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmscFixerConfigTest {
    @Test
    public void parsePackagesTrimsDeduplicatesAndIgnoresEmptyValues() {
        Set<String> parsed = SmscFixer.parsePackages(" com.test.one,com.test.two, ,com.test.one ");
        assertEquals(new LinkedHashSet<>(Arrays.asList("com.test.one", "com.test.two")), parsed);
    }

    @Test
    public void parsePackagesEmptyReturnsEmptySet() {
        assertTrue(SmscFixer.parsePackages("").isEmpty());
        assertTrue(SmscFixer.parsePackages("   ").isEmpty());
        assertTrue(SmscFixer.parsePackages(null).isEmpty());
    }

    @Test
    public void shouldHandlePackageAllowsAndroidAndEmptyTargetList() {
        assertTrue(SmscFixer.shouldHandlePackage("android", new LinkedHashSet<>()));
        assertTrue(SmscFixer.shouldHandlePackage("com.any.app", new LinkedHashSet<>()));
        assertTrue(SmscFixer.shouldHandlePackage("com.any.app", null));
    }

    @Test
    public void shouldHandlePackageMatchesOnlyTargetedPackages() {
        Set<String> targets = new LinkedHashSet<>(Arrays.asList("com.foo", "com.bar"));
        assertTrue(SmscFixer.shouldHandlePackage("com.foo", targets));
        assertFalse(SmscFixer.shouldHandlePackage("com.baz", targets));
    }

    @Test
    public void defaultConfigHasExpectedDefaults() {
        SmscSelectionConfig config = SmscFixer.buildDefaultConfig();
        assertEquals(SmscFixer.DEFAULT_SMSC_PRIMARY, config.primarySmsc);
        assertEquals(SmscFixer.DEFAULT_SMSC_SECONDARY, config.secondarySmsc);
        assertTrue(config.targetPackages.contains("com.google.android.apps.messaging"));
        assertTrue(config.targetPackages.contains("com.android.mms"));
    }
}
