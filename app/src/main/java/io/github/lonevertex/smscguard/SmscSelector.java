package io.github.lonevertex.smscguard;

import java.util.Locale;

/** Pure routing policy with no Android or Xposed dependencies. */
public final class SmscSelector {
    public enum DecisionReason {
        SLOT_PRIMARY,
        SLOT_SECONDARY,
        MCCMNC_FALLBACK,
        CARRIER_FALLBACK,
        AMBIGUOUS_CARRIER_SIGNALS,
        UNKNOWN_ROUTING_SIGNALS
    }

    public static final class SelectionResult {
        /** Candidate SMSC; null when the original argument must be preserved. */
        public final String smsc;
        public final DecisionReason reason;
        public final boolean replacementAuthorized;

        private SelectionResult(String smsc, DecisionReason reason, boolean replacementAuthorized) {
            this.smsc = smsc;
            this.reason = reason;
            this.replacementAuthorized = replacementAuthorized;
        }

        static SelectionResult replace(String smsc, DecisionReason reason) {
            return new SelectionResult(smsc, reason, true);
        }

        static SelectionResult preserve(DecisionReason reason) {
            return new SelectionResult(null, reason, false);
        }
    }

    private SmscSelector() {
    }

    /**
     * Returns a replacement SMSC only when the policy has sufficient routing confidence.
     * Callers must preserve the original SMSC when this method returns null.
     */
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
        if (config == null) {
            return SelectionResult.preserve(DecisionReason.UNKNOWN_ROUTING_SIGNALS);
        }
        if (slotIndex == 1) {
            return SelectionResult.replace(config.secondarySmsc, DecisionReason.SLOT_SECONDARY);
        }
        if (slotIndex == 0) {
            return SelectionResult.replace(config.primarySmsc, DecisionReason.SLOT_PRIMARY);
        }

        String normalizedMccMnc = normalizeMccMnc(carrierMccMnc);
        String mccMncMatched = normalizedMccMnc.isEmpty()
                ? null
                : config.mccMncFallbacks.get(normalizedMccMnc);

        String normalizedCarrierName = normalizeCarrierName(carrierName);
        String carrierMatched = normalizedCarrierName.isEmpty()
                ? null
                : config.carrierNameFallbacks.get(normalizedCarrierName);

        if (isConfigured(mccMncMatched) && isConfigured(carrierMatched)
                && !mccMncMatched.equals(carrierMatched)) {
            return SelectionResult.preserve(DecisionReason.AMBIGUOUS_CARRIER_SIGNALS);
        }
        if (isConfigured(mccMncMatched)) {
            return SelectionResult.replace(mccMncMatched, DecisionReason.MCCMNC_FALLBACK);
        }
        if (isConfigured(carrierMatched)) {
            return SelectionResult.replace(carrierMatched, DecisionReason.CARRIER_FALLBACK);
        }
        return SelectionResult.preserve(DecisionReason.UNKNOWN_ROUTING_SIGNALS);
    }

    public static String normalizeMccMnc(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.matches("^[0-9\\s-]+$")) {
            return "";
        }
        String normalized = trimmed.replaceAll("[^0-9]", "");
        return normalized.matches("^[0-9]{5,6}$") ? normalized : "";
    }

    public static String normalizeCarrierName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static boolean isConfigured(String value) {
        return value != null && !value.isEmpty();
    }
}
