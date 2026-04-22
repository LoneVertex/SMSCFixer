package com.smscfixer;

import java.util.Locale;

public final class SmscSelector {
    public enum DecisionReason {
        SLOT_PRIMARY,
        SLOT_SECONDARY,
        MCCMNC_FALLBACK,
        CARRIER_FALLBACK,
        AMBIGUOUS_CARRIER_SIGNALS,
        DEFAULT_PRIMARY
    }

    public static final class SelectionResult {
        public final String smsc;
        public final DecisionReason reason;

        SelectionResult(String smsc, DecisionReason reason) {
            this.smsc = smsc;
            this.reason = reason;
        }
    }

    private SmscSelector() {
    }

    public static String selectSmsc(
            int slotIndex,
            String carrierMccMnc,
            String carrierName,
            SmscSelectionConfig config
    ) {
        return selectSmscDetailed(slotIndex, carrierMccMnc, carrierName, config).smsc;
    }

    public static SelectionResult selectSmscDetailed(
            int slotIndex,
            String carrierMccMnc,
            String carrierName,
            SmscSelectionConfig config
    ) {
        if (slotIndex == 1) {
            return new SelectionResult(config.secondarySmsc, DecisionReason.SLOT_SECONDARY);
        }
        if (slotIndex == 0) {
            return new SelectionResult(config.primarySmsc, DecisionReason.SLOT_PRIMARY);
        }

        String normalizedMccMnc = normalizeMccMnc(carrierMccMnc);
        String mccMncMatched = null;
        if (!normalizedMccMnc.isEmpty()) {
            mccMncMatched = config.mccMncFallbacks.get(normalizedMccMnc);
        }

        String normalizedCarrierName = normalizeCarrierName(carrierName);
        String carrierMatched = null;
        if (!normalizedCarrierName.isEmpty()) {
            carrierMatched = config.carrierNameFallbacks.get(normalizedCarrierName);
        }

        if (mccMncMatched != null && !mccMncMatched.isEmpty()
                && carrierMatched != null && !carrierMatched.isEmpty()
                && !mccMncMatched.equals(carrierMatched)) {
            return new SelectionResult(config.primarySmsc, DecisionReason.AMBIGUOUS_CARRIER_SIGNALS);
        }

        if (mccMncMatched != null && !mccMncMatched.isEmpty()) {
            return new SelectionResult(mccMncMatched, DecisionReason.MCCMNC_FALLBACK);
        }

        if (carrierMatched != null && !carrierMatched.isEmpty()) {
            return new SelectionResult(carrierMatched, DecisionReason.CARRIER_FALLBACK);
        }

        return new SelectionResult(config.primarySmsc, DecisionReason.DEFAULT_PRIMARY);
    }

    public static String normalizeMccMnc(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[^0-9]", "");
    }

    public static String normalizeCarrierName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
