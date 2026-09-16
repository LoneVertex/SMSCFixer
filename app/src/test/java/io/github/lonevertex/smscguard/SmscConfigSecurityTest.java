package io.github.lonevertex.smscguard;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SmscConfigSecurityTest {

    private static final String DEFAULT_PRIMARY = "+20105996500";
    private static final String DEFAULT_SECONDARY = "+20122000020";

    @Test
    public void smscFormatValidationBoundaries() {
        // Boundary: exactly 5 digits (minimum allowed)
        assertTrue(SmscConfigSchema.isSmscFormatValid("+12345"));
        assertTrue(SmscConfigSchema.isSmscFormatValid("12345"));

        // Boundary: 4 digits (sub-minimum, must be rejected)
        assertFalse(SmscConfigSchema.isSmscFormatValid("+1234"));
        assertFalse(SmscConfigSchema.isSmscFormatValid("1234"));

        // Boundary: exactly 20 digits (maximum allowed)
        assertTrue(SmscConfigSchema.isSmscFormatValid("+12345678901234567890"));
        assertTrue(SmscConfigSchema.isSmscFormatValid("12345678901234567890"));

        // Boundary: 21 digits (over-maximum, must be rejected)
        assertFalse(SmscConfigSchema.isSmscFormatValid("+123456789012345678901"));
        assertFalse(SmscConfigSchema.isSmscFormatValid("123456789012345678901"));

        // Invalid formats: symbols, letters, formatting separators
        assertFalse(SmscConfigSchema.isSmscFormatValid("++12345"));
        assertFalse(SmscConfigSchema.isSmscFormatValid("+20-10-599-6500"));
        assertFalse(SmscConfigSchema.isSmscFormatValid("+20 (10) 599-6500"));
        assertFalse(SmscConfigSchema.isSmscFormatValid("+2010599650a"));
        assertFalse(SmscConfigSchema.isSmscFormatValid(""));
        assertFalse(SmscConfigSchema.isSmscFormatValid("    "));
        assertFalse(SmscConfigSchema.isSmscFormatValid(null));
    }

    @Test
    public void normalizeSmscOrDefaultAddsPlusPrefixOrReturnsFallback() {
        assertEquals("+20105996500", SmscConfigSchema.normalizeSmscOrDefault("20105996500", DEFAULT_PRIMARY));
        assertEquals("+20105996500", SmscConfigSchema.normalizeSmscOrDefault("+20105996500", DEFAULT_PRIMARY));
        assertEquals("+20105996500", SmscConfigSchema.normalizeSmscOrDefault("  +20105996500  ", DEFAULT_PRIMARY));

        // Invalid values return fallback untouched
        assertEquals(DEFAULT_PRIMARY, SmscConfigSchema.normalizeSmscOrDefault(null, DEFAULT_PRIMARY));
        assertEquals(DEFAULT_PRIMARY, SmscConfigSchema.normalizeSmscOrDefault("", DEFAULT_PRIMARY));
        assertEquals(DEFAULT_PRIMARY, SmscConfigSchema.normalizeSmscOrDefault("   ", DEFAULT_PRIMARY));
        assertEquals(DEFAULT_PRIMARY, SmscConfigSchema.normalizeSmscOrDefault("bad_smsc", DEFAULT_PRIMARY));
        assertEquals(DEFAULT_PRIMARY, SmscConfigSchema.normalizeSmscOrDefault("+12", DEFAULT_PRIMARY));
    }

    @Test
    public void isSmscInputAcceptableAllowsEmptyOrValid() {
        assertTrue(SmscConfigSchema.isSmscInputAcceptable(null));
        assertTrue(SmscConfigSchema.isSmscInputAcceptable(""));
        assertTrue(SmscConfigSchema.isSmscInputAcceptable("   "));
        assertTrue(SmscConfigSchema.isSmscInputAcceptable("+20105996500"));
        assertFalse(SmscConfigSchema.isSmscInputAcceptable("invalid"));
    }

    @Test
    public void maliciousPackageNamesAreRejectedAndSanitized() {
        String[] attacks = new String[]{
                "com.test; DROP TABLE smsc",
                "com.test && rm -rf /",
                "../../etc/shadow",
                "<script>alert(1)</script>",
                "com..double.dot",
                ".leading.dot",
                "trailing.dot.",
                "1leading.number",
                "singleword"
        };

        for (String attack : attacks) {
            // UI validator must reject any attack input
            assertFalse("UI validator must reject attack: " + attack,
                    SmscConfigSchema.isTargetPackagesCsvAcceptable(attack));

            // Runtime parser must fail safe to known default packages
            Set<String> normalized = SmscConfigSchema.parseAndNormalizeTargetPackages(attack);
            assertNotNull(normalized);
            assertTrue("Must contain com.google.android.apps.messaging on attack fallback",
                    normalized.contains("com.google.android.apps.messaging"));
            assertTrue("Must contain com.android.mms on attack fallback",
                    normalized.contains("com.android.mms"));
            assertFalse("Must never include the malicious string in normalized targets",
                    normalized.contains(attack));
        }
    }

    @Test
    public void mixedPackagesSanitizationBehavior() {
        String mixed = "com.valid.app, invalid name, org.another.valid_app";
        // UI validation strictly rejects the entire string when any entry is invalid
        assertFalse(SmscConfigSchema.isTargetPackagesCsvAcceptable(mixed));

        // Runtime normalization filters out the invalid entry and preserves valid ones
        Set<String> runtimeNormalized = SmscConfigSchema.parseAndNormalizeTargetPackages(mixed);
        assertEquals(2, runtimeNormalized.size());
        assertTrue(runtimeNormalized.contains("com.valid.app"));
        assertTrue(runtimeNormalized.contains("org.another.valid_app"));
        assertFalse(runtimeNormalized.contains("invalid name"));

        String csvOutput = SmscConfigSchema.normalizeTargetPackagesCsv(mixed);
        assertEquals("com.valid.app,org.another.valid_app", csvOutput);
    }

    @Test
    public void validTargetPackagesAccepted() {
        String valid = "com.google.android.apps.messaging,com.android.mms,org.telegram.messenger";
        assertTrue(SmscConfigSchema.isTargetPackagesCsvAcceptable(valid));
        Set<String> normalized = SmscConfigSchema.parseAndNormalizeTargetPackages(valid);
        assertEquals(3, normalized.size());
    }

    @Test
    public void configurationRepositoryRecoversFromNullOrCorruptedStorage() {
        DiagnosticLogger logger = new DiagnosticLogger("TestTag");
        ConfigurationRepository repo = new ConfigurationRepository(
                "io.github.lonevertex.smscguard",
                "smscguard_prefs",
                DEFAULT_PRIMARY,
                DEFAULT_SECONDARY,
                logger
        );

        ConfigurationRepository.Snapshot defaultSnapshot = repo.load(null, false);
        assertEquals(DEFAULT_PRIMARY, defaultSnapshot.selectionConfig.primarySmsc);
        assertEquals(DEFAULT_SECONDARY, defaultSnapshot.selectionConfig.secondarySmsc);
        assertEquals(2, defaultSnapshot.selectionConfig.targetPackages.size());
        assertFalse(defaultSnapshot.diagnosticsEnabled);
    }
}
