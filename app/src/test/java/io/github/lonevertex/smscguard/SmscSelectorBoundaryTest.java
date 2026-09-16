package io.github.lonevertex.smscguard;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SmscSelectorBoundaryTest {

    private static final String PRIMARY_SMSC = "+20105996500";
    private static final String SECONDARY_SMSC = "+20122000020";

    private SmscSelectionConfig config;

    @Before
    public void setUp() {
        Map<String, String> mccMnc = new HashMap<>();
        mccMnc.put("60202", PRIMARY_SMSC);
        mccMnc.put("60201", SECONDARY_SMSC);

        Map<String, String> carrier = new HashMap<>();
        carrier.put(SmscSelector.normalizeCarrierName("vodafone egypt"), PRIMARY_SMSC);
        carrier.put(SmscSelector.normalizeCarrierName("orange egypt"), SECONDARY_SMSC);

        config = new SmscSelectionConfig(
                PRIMARY_SMSC,
                SECONDARY_SMSC,
                mccMnc,
                carrier,
                new HashSet<>(Collections.singleton("com.google.android.apps.messaging"))
        );
    }

    @Test
    public void nullConfigSafelyPreservesOriginal() {
        SmscSelector.SelectionResult result = SmscSelector.selectSmscDetailed(0, "60202", "Vodafone Egypt", null);
        assertNotNull(result);
        assertFalse(result.replacementAuthorized);
        assertNull(result.smsc);
        assertEquals(SmscSelector.DecisionReason.UNKNOWN_ROUTING_SIGNALS, result.reason);
    }

    @Test
    public void multiSimSlotTwoFallsThroughToFallbackRules() {
        // Slot 2 on triple-SIM device: should not assume slot 0 or 1, must use fallback rules
        SmscSelector.SelectionResult result = SmscSelector.selectSmscDetailed(2, "60201", "Orange Egypt", config);
        assertTrue(result.replacementAuthorized);
        assertEquals(SmscSelector.DecisionReason.MCCMNC_FALLBACK, result.reason);
        assertEquals(SECONDARY_SMSC, result.smsc);

        // Slot 2 with unknown carrier signals: must fail closed and preserve
        SmscSelector.SelectionResult unkResult = SmscSelector.selectSmscDetailed(2, "", "", config);
        assertFalse(unkResult.replacementAuthorized);
        assertNull(unkResult.smsc);
        assertEquals(SmscSelector.DecisionReason.UNKNOWN_ROUTING_SIGNALS, unkResult.reason);
    }

    @Test
    public void negativeUnknownSlotsFallThroughToFallbacks() {
        SmscSelector.SelectionResult resNeg1 = SmscSelector.selectSmscDetailed(-1, "60202", "", config);
        assertTrue(resNeg1.replacementAuthorized);
        assertEquals(SmscSelector.DecisionReason.MCCMNC_FALLBACK, resNeg1.reason);
        assertEquals(PRIMARY_SMSC, resNeg1.smsc);

        SmscSelector.SelectionResult resNeg99 = SmscSelector.selectSmscDetailed(-99, "", "Orange Egypt", config);
        assertTrue(resNeg99.replacementAuthorized);
        assertEquals(SmscSelector.DecisionReason.CARRIER_FALLBACK, resNeg99.reason);
        assertEquals(SECONDARY_SMSC, resNeg99.smsc);
    }

    @Test
    public void conflictingCarrierSignalsFailClosedToPreserve() {
        // MCC/MNC is Vodafone, but Carrier name says Orange -> Ambiguous conflict!
        SmscSelector.SelectionResult conflictResult = SmscSelector.selectSmscDetailed(
                -1,
                "60202", // Vodafone
                "Orange Egypt", // Orange
                config
        );
        assertFalse(conflictResult.replacementAuthorized);
        assertNull(conflictResult.smsc);
        assertEquals(SmscSelector.DecisionReason.AMBIGUOUS_CARRIER_SIGNALS, conflictResult.reason);
    }

    @Test
    public void concordantCarrierSignalsAgreeOnFallback() {
        // MCC/MNC is Vodafone, and Carrier name is Vodafone -> Concordant
        SmscSelector.SelectionResult result = SmscSelector.selectSmscDetailed(
                -1,
                "60202",
                "Vodafone Egypt",
                config
        );
        assertTrue(result.replacementAuthorized);
        assertEquals(SmscSelector.DecisionReason.MCCMNC_FALLBACK, result.reason);
        assertEquals(PRIMARY_SMSC, result.smsc);
    }

    @Test
    public void unconfiguredOrEmptyFallbackEntriesAreIgnored() {
        Map<String, String> mccMnc = new HashMap<>();
        mccMnc.put("60202", ""); // Empty configured value

        Map<String, String> carrier = new HashMap<>();
        carrier.put("orange egypt", SECONDARY_SMSC);

        SmscSelectionConfig sparseConfig = new SmscSelectionConfig(
                PRIMARY_SMSC,
                SECONDARY_SMSC,
                mccMnc,
                carrier,
                new HashSet<>()
        );

        // MCC/MNC matched empty string, should skip and match carrier fallback
        SmscSelector.SelectionResult result = SmscSelector.selectSmscDetailed(
                -1,
                "60202",
                "Orange Egypt",
                sparseConfig
        );
        assertTrue(result.replacementAuthorized);
        assertEquals(SmscSelector.DecisionReason.CARRIER_FALLBACK, result.reason);
        assertEquals(SECONDARY_SMSC, result.smsc);
    }

    @Test
    public void mccMncNormalizationExhaustive() {
        // Valid 5-digit and 6-digit formats
        assertEquals("60202", SmscSelector.normalizeMccMnc("60202"));
        assertEquals("60201", SmscSelector.normalizeMccMnc(" 602 01 "));
        assertEquals("60202", SmscSelector.normalizeMccMnc("602-02"));
        assertEquals("310410", SmscSelector.normalizeMccMnc("310-410"));
        assertEquals("310410", SmscSelector.normalizeMccMnc("  310 410  "));

        // Invalid lengths or characters
        assertEquals("", SmscSelector.normalizeMccMnc(null));
        assertEquals("", SmscSelector.normalizeMccMnc(""));
        assertEquals("", SmscSelector.normalizeMccMnc("    "));
        assertEquals("", SmscSelector.normalizeMccMnc("6020")); // 4 digits
        assertEquals("", SmscSelector.normalizeMccMnc("6020101")); // 7 digits
        assertEquals("", SmscSelector.normalizeMccMnc("6020a")); // alphanumeric
        assertEquals("", SmscSelector.normalizeMccMnc("6020+1")); // symbols
        assertEquals("", SmscSelector.normalizeMccMnc("invalid_sim"));
    }

    @Test
    public void carrierNameNormalizationExhaustive() {
        assertEquals("", SmscSelector.normalizeCarrierName(null));
        assertEquals("", SmscSelector.normalizeCarrierName(""));
        assertEquals("", SmscSelector.normalizeCarrierName("   "));
        assertEquals("vodafone", SmscSelector.normalizeCarrierName("Vodafone"));
        assertEquals("vodafone egypt", SmscSelector.normalizeCarrierName("  VODAFONE   EGYPT  "));
        assertEquals("orange egypt", SmscSelector.normalizeCarrierName("Orange\t\nEgypt"));
        assertEquals("etisalat misr", SmscSelector.normalizeCarrierName("  Etisalat \t Misr  "));
    }

    @Test
    public void zeroPiiExposureGuaranteed() {
        SmscSelector.SelectionResult result = SmscSelector.selectSmscDetailed(0, "60202", "Vodafone Egypt", config);
        // Reason must be an enum name, not user data
        assertNotNull(result.reason.name());
        // String representation must not contain message body or recipient
        assertFalse(result.reason.name().contains("message"));
        assertFalse(result.reason.name().contains("recipient"));
    }
}
