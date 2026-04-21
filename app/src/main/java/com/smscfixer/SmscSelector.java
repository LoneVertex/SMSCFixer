package com.smscfixer;

import java.util.Locale;

public final class SmscSelector {
    private SmscSelector() {
    }

    public static String selectSmsc(
            int slotIndex,
            String carrierMccMnc,
            String carrierName,
            SmscSelectionConfig config
    ) {
        if (slotIndex == 1) {
            return config.secondarySmsc;
        }
        if (slotIndex == 0) {
            return config.primarySmsc;
        }

        String normalizedMccMnc = normalizeMccMnc(carrierMccMnc);
        if (!normalizedMccMnc.isEmpty()) {
            String mccMncMatched = config.mccMncFallbacks.get(normalizedMccMnc);
            if (mccMncMatched != null && !mccMncMatched.isEmpty()) {
                return mccMncMatched;
            }
        }

        String normalizedCarrierName = normalizeCarrierName(carrierName);
        if (!normalizedCarrierName.isEmpty()) {
            String carrierMatched = config.carrierNameFallbacks.get(normalizedCarrierName);
            if (carrierMatched != null && !carrierMatched.isEmpty()) {
                return carrierMatched;
            }
        }

        return config.primarySmsc;
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
