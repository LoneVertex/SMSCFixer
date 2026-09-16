package io.github.lonevertex.smscguard;

import android.telephony.SmsManager;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HookSignatureRegistryPlatformTest {

    @Test
    public void verifyAllRegisteredSignaturesExistInPlatformSmsManager() {
        List<HookSignatureRegistry.HookSignature> signatures = HookSignatureRegistry.supportedSignatures();
        assertTrue("Registered signatures list must not be empty", signatures.size() >= 6);

        for (HookSignatureRegistry.HookSignature signature : signatures) {
            Method foundMethod = findMethodInClass(SmsManager.class, signature);
            assertNotNull(
                    "Signature " + signature.diagnosticName() + " must exist in android.telephony.SmsManager",
                    foundMethod
            );

            // Verify return type is void
            assertEquals(
                    "Method " + signature.methodName + " must return void",
                    void.class,
                    foundMethod.getReturnType()
            );

            // Verify parameter at smscArgumentIndex is String
            Class<?>[] parameterTypes = foundMethod.getParameterTypes();
            assertTrue(
                    "Parameter list length must exceed smscArgumentIndex",
                    parameterTypes.length > signature.smscArgumentIndex
            );
            assertEquals(
                    "Parameter at SMSC index must be java.lang.String",
                    String.class,
                    parameterTypes[signature.smscArgumentIndex]
            );

            // Verify match(Method) identifies this signature
            HookSignatureRegistry.HookSignature matched = HookSignatureRegistry.match(foundMethod);
            assertNotNull("Registry must match platform method: " + foundMethod, matched);
            assertEquals(signature.diagnosticName(), matched.diagnosticName());
        }
    }

    @Test
    public void rejectsNonTargetPlatformMethods() {
        Method[] methods = SmsManager.class.getDeclaredMethods();
        for (Method method : methods) {
            if (!method.getName().startsWith("send")) {
                assertNull(
                        "Non-send method " + method.getName() + " must not match hook registry",
                        HookSignatureRegistry.match(method)
                );
            }
        }
    }

    @Test
    public void signatureMatchingEqualityAndDiagnosticProperties() {
        HookSignatureRegistry.HookSignature sig = HookSignatureRegistry.supportedSignatures().get(0);
        assertNotNull(sig.compatibilityNote);
        assertTrue(sig.diagnosticName().startsWith(sig.methodName));
        assertEquals(HookSignatureRegistry.SMSC_ARGUMENT_INDEX, sig.smscArgumentIndex);

        // Matching with exact parameters
        assertTrue(sig.matches(sig.methodName, sig.parameterTypeNames));

        // Mismatched method name
        assertNull(HookSignatureRegistry.match("differentMethod", "void", sig.parameterTypeNames));

        // Mismatched parameter count
        assertNull(HookSignatureRegistry.match(sig.methodName, "void", new String[]{"java.lang.String"}));

        // Null checks
        assertNull(HookSignatureRegistry.match(null));
        assertNull(HookSignatureRegistry.match(sig.methodName, "void", null));
    }

    private Method findMethodInClass(Class<?> clazz, HookSignatureRegistry.HookSignature signature) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(signature.methodName)) {
                continue;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != signature.parameterTypeNames.length) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < paramTypes.length; i++) {
                if (!paramTypes[i].getName().equals(signature.parameterTypeNames[i])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return method;
            }
        }
        return null;
    }
}
