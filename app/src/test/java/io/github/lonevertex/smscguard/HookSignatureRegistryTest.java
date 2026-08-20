package io.github.lonevertex.smscguard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HookSignatureRegistryTest {
    private static final String VOID = "void";
    private static final String STRING = "java.lang.String";
    private static final String PENDING_INTENT = "android.app.PendingIntent";

    @Test
    public void acceptsExactPublicTextMessageSignature() {
        HookSignatureRegistry.HookSignature signature = HookSignatureRegistry.match(
                "sendTextMessage",
                VOID,
                new String[]{STRING, STRING, STRING, PENDING_INTENT, PENDING_INTENT}
        );
        assertNotNull(signature);
        assertEquals(1, signature.smscArgumentIndex);
    }

    @Test
    public void acceptsExactPublicDataMessageSignature() {
        HookSignatureRegistry.HookSignature signature = HookSignatureRegistry.match(
                "sendDataMessage",
                VOID,
                new String[]{STRING, STRING, "short", "[B", PENDING_INTENT, PENDING_INTENT}
        );
        assertNotNull(signature);
        assertEquals(1, signature.smscArgumentIndex);
    }

    @Test
    public void rejectsSameNameWithUnknownParameterShape() {
        assertNull(HookSignatureRegistry.match(
                "sendTextMessage",
                VOID,
                new String[]{STRING, STRING, STRING, STRING}
        ));
    }

    @Test
    public void rejectsGenericSendMethodThatOnlyLooksCompatible() {
        assertNull(HookSignatureRegistry.match(
                "sendVendorMessage",
                VOID,
                new String[]{STRING, STRING, STRING, PENDING_INTENT, PENDING_INTENT}
        ));
    }

    @Test
    public void rejectsNonVoidReturnTypes() {
        assertNull(HookSignatureRegistry.match(
                "sendTextMessage",
                "boolean",
                new String[]{STRING, STRING, STRING, PENDING_INTENT, PENDING_INTENT}
        ));
    }
}
