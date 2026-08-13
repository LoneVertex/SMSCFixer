package com.smscfixer;

import android.app.Application;

import de.robv.android.xposed.XposedHelpers;

/**
 * Android/Xposed adapter for subscription, slot, and carrier signals. It deliberately returns an
 * incomplete signal set on reflection failure; policy then preserves the original SMSC.
 */
final class RoutingSignalResolver {
    private static final int PRIMARY_SLOT_INDEX = 0;
    private static final int SECONDARY_SLOT_INDEX = 1;
    private static final long REFLECTION_LOG_THROTTLE_MS = 120_000L;

    private final DiagnosticLogger logger;

    RoutingSignalResolver(DiagnosticLogger logger) {
        this.logger = logger;
    }

    RoutingSignals resolve(Object smsManager) {
        int subscriptionId = resolveSubscriptionId(smsManager);
        int slotIndex = resolveSlotIndex(subscriptionId, smsManager);
        CarrierInfo carrierInfo = resolveCarrierInfo(subscriptionId);
        return new RoutingSignals(subscriptionId, slotIndex, carrierInfo.carrierName, carrierInfo.mccMnc);
    }

    private int resolveSubscriptionId(Object smsManager) {
        Integer fromSmsManager = tryReadIntMethod(smsManager, "getSubscriptionId");
        if (isValidSubscriptionId(fromSmsManager)) {
            return fromSmsManager;
        }
        Integer legacySubId = tryReadIntMethod(smsManager, "getSubId");
        if (isValidSubscriptionId(legacySubId)) {
            return legacySubId;
        }
        Integer fieldSubId = tryReadIntField(smsManager, "mSubId");
        return isValidSubscriptionId(fieldSubId)
                ? fieldSubId
                : RoutingSignals.INVALID_SUBSCRIPTION_ID;
    }

    private int resolveSlotIndex(int subscriptionId, Object smsManager) {
        if (subscriptionId != RoutingSignals.INVALID_SUBSCRIPTION_ID) {
            Integer slotFromSubscriptionManager = resolveSlotFromSubscriptionManager(subscriptionId);
            if (isValidSlot(slotFromSubscriptionManager)) {
                return slotFromSubscriptionManager;
            }
        }
        Integer slotFromSmsManager = tryReadIntMethod(smsManager, "getSlotIndex");
        if (isValidSlot(slotFromSmsManager)) {
            return slotFromSmsManager;
        }
        Integer slotFromSimSlot = tryReadIntMethod(smsManager, "getSimSlotIndex");
        if (isValidSlot(slotFromSimSlot)) {
            return slotFromSimSlot;
        }
        Integer slotField = tryReadIntField(smsManager, "mSlotIndex");
        return isValidSlot(slotField) ? slotField : RoutingSignals.INVALID_SLOT_INDEX;
    }

    private Integer resolveSlotFromSubscriptionManager(int subscriptionId) {
        Integer viaGetSlotIndex = callSubscriptionSlotMethod("getSlotIndex", subscriptionId);
        if (isValidSlot(viaGetSlotIndex)) {
            return viaGetSlotIndex;
        }
        Integer viaGetPhoneId = callSubscriptionSlotMethod("getPhoneId", subscriptionId);
        if (isValidSlot(viaGetPhoneId)) {
            return viaGetPhoneId;
        }
        Integer viaGetSlotId = callSubscriptionSlotMethod("getSlotId", subscriptionId);
        return isValidSlot(viaGetSlotId) ? viaGetSlotId : null;
    }

    private CarrierInfo resolveCarrierInfo(int subscriptionId) {
        String carrierName = "";
        String mccMnc = "";
        Object application = getCurrentApplication();
        if (!(application instanceof Application)) {
            return new CarrierInfo(carrierName, mccMnc);
        }
        try {
            Object telephony = XposedHelpers.callMethod(application, "getSystemService", "phone");
            if (telephony == null) {
                return new CarrierInfo(carrierName, mccMnc);
            }
            Object scopedTelephony = telephony;
            if (subscriptionId >= 0) {
                try {
                    Object candidate = XposedHelpers.callMethod(telephony, "createForSubscriptionId", subscriptionId);
                    if (candidate != null) {
                        scopedTelephony = candidate;
                    }
                } catch (Throwable error) {
                    logReflectionFailure("carrier:create_for_subscription", error);
                }
            }
            try {
                Object resultName = XposedHelpers.callMethod(scopedTelephony, "getSimOperatorName");
                if (resultName instanceof String) {
                    carrierName = (String) resultName;
                }
            } catch (Throwable error) {
                logReflectionFailure("carrier:get_name", error);
            }
            try {
                Object resultMccMnc = XposedHelpers.callMethod(scopedTelephony, "getSimOperator");
                if (resultMccMnc instanceof String) {
                    mccMnc = (String) resultMccMnc;
                }
            } catch (Throwable error) {
                logReflectionFailure("carrier:get_operator", error);
            }
        } catch (Throwable error) {
            logReflectionFailure("carrier:get_service", error);
        }
        return new CarrierInfo(carrierName, mccMnc);
    }

    private Integer callSubscriptionSlotMethod(String methodName, int subscriptionId) {
        try {
            Class<?> subscriptionManager = Class.forName("android.telephony.SubscriptionManager");
            Object result = XposedHelpers.callStaticMethod(subscriptionManager, methodName, subscriptionId);
            if (result instanceof Integer) {
                int slot = (Integer) result;
                return slot >= 0 ? slot : null;
            }
        } catch (Throwable error) {
            logReflectionFailure("subscription_slot:" + methodName, error);
        }
        return null;
    }

    private Object getCurrentApplication() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            return XposedHelpers.callStaticMethod(activityThread, "currentApplication");
        } catch (Throwable error) {
            logReflectionFailure("current_application", error);
            return null;
        }
    }

    private Integer tryReadIntMethod(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Object value = XposedHelpers.callMethod(target, methodName);
            if (value instanceof Integer) {
                int result = (Integer) value;
                return result >= 0 ? result : null;
            }
        } catch (Throwable error) {
            logReflectionFailure("read_method:" + methodName, error);
        }
        return null;
    }

    private Integer tryReadIntField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        try {
            Object value = XposedHelpers.getObjectField(target, fieldName);
            if (value instanceof Integer) {
                int result = (Integer) value;
                return result >= 0 ? result : null;
            }
        } catch (Throwable error) {
            logReflectionFailure("read_field:" + fieldName, error);
        }
        return null;
    }

    private void logReflectionFailure(String operation, Throwable error) {
        logger.throttledFailure(operation, error, REFLECTION_LOG_THROTTLE_MS);
    }

    private static boolean isValidSubscriptionId(Integer value) {
        return value != null && value >= 0;
    }

    private static boolean isValidSlot(Integer slot) {
        return slot != null && (slot == PRIMARY_SLOT_INDEX || slot == SECONDARY_SLOT_INDEX);
    }

    private static final class CarrierInfo {
        final String carrierName;
        final String mccMnc;

        CarrierInfo(String carrierName, String mccMnc) {
            this.carrierName = carrierName == null ? "" : carrierName;
            this.mccMnc = mccMnc == null ? "" : mccMnc;
        }
    }
}
