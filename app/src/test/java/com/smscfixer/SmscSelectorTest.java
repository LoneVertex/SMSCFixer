package com.smscfixer;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;

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
        assertEquals(SECONDARY, SmscSelector.selectSmsc(1, "", "", config()));
    }

    @Test
    public void selectsPrimaryForPrimarySlot() {
        assertEquals(PRIMARY, SmscSelector.selectSmsc(0, "", "", config()));
    }

    @Test
    public void fallsBackUsingMccMncWhenSlotUnknown() {
        assertEquals(SECONDARY, SmscSelector.selectSmsc(-1, "60201", "", config()));
    }

    @Test
    public void fallsBackUsingCarrierNameWhenSlotAndMccMncUnknown() {
        assertEquals(PRIMARY, SmscSelector.selectSmsc(-1, "", "Vodafone Egypt", config()));
    }

    @Test
    public void fallsBackToPrimaryWhenNoSignals() {
        assertEquals(PRIMARY, SmscSelector.selectSmsc(-1, "", "", config()));
    }

    @Test
    public void normalizesMccMncAndCarrierValues() {
        assertEquals("60201", SmscSelector.normalizeMccMnc(" 602-01 "));
        assertEquals("orange egypt", SmscSelector.normalizeCarrierName(" Orange   Egypt "));
    }

    @Test
    public void ambiguousSignalsReturnPrimarySafeFallback() {
        assertEquals(
                SmscSelector.DecisionReason.AMBIGUOUS_CARRIER_SIGNALS,
                SmscSelector.selectSmscDetailed(-1, "60201", "Vodafone Egypt", config()).reason
        );
        assertEquals(PRIMARY, SmscSelector.selectSmsc(-1, "60201", "Vodafone Egypt", config()));
    }
}
