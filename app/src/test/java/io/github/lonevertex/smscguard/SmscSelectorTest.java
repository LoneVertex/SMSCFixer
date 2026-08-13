package io.github.lonevertex.smscguard;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SmscSelectorTest {
    private static final String PRIMARY = "+20105996500";
    private static final String SECONDARY = "+20122000020";

    private static SmscSelectionConfig config() {
        Map<String, String> mccMnc = new HashMap<>();
        mccMnc.put("60202", PRIMARY);
        mccMnc.put("60201", SECONDARY);

        Map<String, String> carrier = new HashMap<>();
        carrier.put(SmscSelector.normalizeCarrierName("vodafone egypt"), PRIMARY);
        carrier.put(SmscSelector.normalizeCarrierName("orange egypt"), SECONDARY);

        return new SmscSelectionConfig(
                PRIMARY,
                SECONDARY,
                mccMnc,
                carrier,
                new HashSet<>(Collections.singleton("com.google.android.apps.messaging"))
        );
    }

    @Test
    public void selectsSecondaryForSecondarySlot() {
        SmscSelector.SelectionResult result = SmscSelector.selectSmscDetailed(1, "", "", config());
        assertTrue(result.replacementAuthorized);
        assertEquals(SmscSelector.DecisionReason.SLOT_SECONDARY, result.reason);
        assertEquals(SECONDARY, result.smsc);
    }

    @Test
    public void selectsPrimaryForPrimarySlot() {
        assertEquals(PRIMARY, SmscSelector.selectSmsc(0, "", "", config()));
    }

    @Test
    public void fallsBackUsingMccMncWhenSlotUnknown() {
        SmscSelector.SelectionResult result = SmscSelector.selectSmscDetailed(-1, "60201", "", config());
        assertTrue(result.replacementAuthorized);
        assertEquals(SmscSelector.DecisionReason.MCCMNC_FALLBACK, result.reason);
        assertEquals(SECONDARY, result.smsc);
    }

    @Test
    public void fallsBackUsingCarrierNameWhenSlotAndMccMncUnknown() {
        assertEquals(PRIMARY, SmscSelector.selectSmsc(-1, "", "Vodafone Egypt", config()));
    }

    @Test
    public void preservesOriginalWhenNoSignalsAreAvailable() {
        SmscSelector.SelectionResult result = SmscSelector.selectSmscDetailed(-1, "", "", config());
        assertFalse(result.replacementAuthorized);
        assertEquals(SmscSelector.DecisionReason.UNKNOWN_ROUTING_SIGNALS, result.reason);
        assertNull(result.smsc);
    }

    @Test
    public void normalizesOnlyValidMccMncAndCarrierValues() {
        assertEquals("60201", SmscSelector.normalizeMccMnc(" 602-01 "));
        assertEquals("", SmscSelector.normalizeMccMnc("60201x"));
        assertEquals("", SmscSelector.normalizeMccMnc("6020"));
        assertEquals("orange egypt", SmscSelector.normalizeCarrierName(" Orange   Egypt "));
    }

    @Test
    public void ambiguousSignalsPreserveOriginalAddress() {
        SmscSelector.SelectionResult result = SmscSelector.selectSmscDetailed(
                -1,
                "60201",
                "Vodafone Egypt",
                config()
        );
        assertFalse(result.replacementAuthorized);
        assertEquals(SmscSelector.DecisionReason.AMBIGUOUS_CARRIER_SIGNALS, result.reason);
        assertNull(result.smsc);
    }
}
