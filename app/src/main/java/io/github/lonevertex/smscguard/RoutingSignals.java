package io.github.lonevertex.smscguard;

/** Immutable routing inputs collected from the Android telephony environment. */
final class RoutingSignals {
    static final int INVALID_SUBSCRIPTION_ID = -1;
    static final int INVALID_SLOT_INDEX = -1;

    final int subscriptionId;
    final int slotIndex;
    final String carrierName;
    final String mccMnc;

    RoutingSignals(int subscriptionId, int slotIndex, String carrierName, String mccMnc) {
        this.subscriptionId = subscriptionId;
        this.slotIndex = slotIndex;
        this.carrierName = carrierName == null ? "" : carrierName;
        this.mccMnc = mccMnc == null ? "" : mccMnc;
    }
}
