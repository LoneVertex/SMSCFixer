package io.github.lonevertex.smscguard;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class RoutingSignalResolverTest {

    private DiagnosticLogger logger;
    private RoutingSignalResolver resolver;

    @Before
    public void setUp() {
        logger = new DiagnosticLogger("TestTag");
        resolver = new RoutingSignalResolver(logger);
    }

    public static class SmsManagerModernSubId {
        private final int subId;

        public SmsManagerModernSubId(int subId) {
            this.subId = subId;
        }

        public int getSubscriptionId() {
            return subId;
        }
    }

    public static class SmsManagerLegacySubId {
        private final int subId;

        public SmsManagerLegacySubId(int subId) {
            this.subId = subId;
        }

        public int getSubId() {
            return subId;
        }
    }

    public static class SmsManagerFieldSubId {
        private final int mSubId;

        public SmsManagerFieldSubId(int subId) {
            this.mSubId = subId;
        }
    }

    public static class SmsManagerWithSlotMethod {
        private final int slot;

        public SmsManagerWithSlotMethod(int slot) {
            this.slot = slot;
        }

        public int getSlotIndex() {
            return slot;
        }
    }

    public static class SmsManagerWithSimSlotMethod {
        private final int slot;

        public SmsManagerWithSimSlotMethod(int slot) {
            this.slot = slot;
        }

        public int getSimSlotIndex() {
            return slot;
        }
    }

    public static class SmsManagerWithSlotField {
        private final int mSlotIndex;

        public SmsManagerWithSlotField(int slot) {
            this.mSlotIndex = slot;
        }
    }

    public static class SmsManagerFaulty {
        public int getSubscriptionId() {
            throw new SecurityException("Permission denied");
        }

        public int getSlotIndex() {
            throw new RuntimeException("Unexpected error");
        }
    }

    public static class SmsManagerNegativeValues {
        public int getSubscriptionId() {
            return -1;
        }

        public int getSlotIndex() {
            return -1;
        }
    }

    public static class SmsManagerHighSlot {
        public int getSlotIndex() {
            return 3; // > 1 is invalid for dual-SIM primary/secondary slot routing
        }
    }

    @Test
    public void resolveNullSmsManagerReturnsInvalidSignalsSafely() {
        RoutingSignals signals = resolver.resolve(null);
        assertNotNull(signals);
        assertEquals(RoutingSignals.INVALID_SUBSCRIPTION_ID, signals.subscriptionId);
        assertEquals(RoutingSignals.INVALID_SLOT_INDEX, signals.slotIndex);
        assertEquals("", signals.carrierName);
        assertEquals("", signals.mccMnc);
    }

    @Test
    public void resolveModernSubscriptionId() {
        SmsManagerModernSubId target = new SmsManagerModernSubId(42);
        RoutingSignals signals = resolver.resolve(target);
        assertEquals(42, signals.subscriptionId);
    }

    @Test
    public void resolveLegacySubId() {
        SmsManagerLegacySubId target = new SmsManagerLegacySubId(101);
        RoutingSignals signals = resolver.resolve(target);
        assertEquals(101, signals.subscriptionId);
    }

    @Test
    public void resolveFieldSubId() {
        SmsManagerFieldSubId target = new SmsManagerFieldSubId(202);
        RoutingSignals signals = resolver.resolve(target);
        assertEquals(202, signals.subscriptionId);
    }

    @Test
    public void resolveSlotIndexMethod() {
        SmsManagerWithSlotMethod targetPrimary = new SmsManagerWithSlotMethod(0);
        assertEquals(0, resolver.resolve(targetPrimary).slotIndex);

        SmsManagerWithSlotMethod targetSecondary = new SmsManagerWithSlotMethod(1);
        assertEquals(1, resolver.resolve(targetSecondary).slotIndex);
    }

    @Test
    public void resolveSimSlotIndexMethod() {
        SmsManagerWithSimSlotMethod target = new SmsManagerWithSimSlotMethod(1);
        assertEquals(1, resolver.resolve(target).slotIndex);
    }

    @Test
    public void resolveSlotIndexField() {
        SmsManagerWithSlotField target = new SmsManagerWithSlotField(0);
        assertEquals(0, resolver.resolve(target).slotIndex);
    }

    @Test
    public void resolveInvalidOrOutOfRangeSlotReturnsInvalidSlotIndex() {
        SmsManagerHighSlot highSlotTarget = new SmsManagerHighSlot();
        assertEquals(RoutingSignals.INVALID_SLOT_INDEX, resolver.resolve(highSlotTarget).slotIndex);

        SmsManagerNegativeValues negTarget = new SmsManagerNegativeValues();
        assertEquals(RoutingSignals.INVALID_SLOT_INDEX, resolver.resolve(negTarget).slotIndex);
        assertEquals(RoutingSignals.INVALID_SUBSCRIPTION_ID, resolver.resolve(negTarget).subscriptionId);
    }

    @Test
    public void resolveReflectionExceptionsFailSafeWithoutCrashing() {
        SmsManagerFaulty faultyTarget = new SmsManagerFaulty();
        RoutingSignals signals = resolver.resolve(faultyTarget);
        assertNotNull(signals);
        assertEquals(RoutingSignals.INVALID_SUBSCRIPTION_ID, signals.subscriptionId);
        assertEquals(RoutingSignals.INVALID_SLOT_INDEX, signals.slotIndex);
    }

    @Test
    public void cachingBehaviorReturnsCachedInstanceForSameSubscriptionId() {
        SmsManagerModernSubId target = new SmsManagerModernSubId(777);
        RoutingSignals first = resolver.resolve(target);
        RoutingSignals second = resolver.resolve(target);
        assertSame("Subsequent resolution for same subscription ID must return cached instance", first, second);
    }
}
