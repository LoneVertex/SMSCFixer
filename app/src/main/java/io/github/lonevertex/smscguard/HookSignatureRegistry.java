package io.github.lonevertex.smscguard;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Explicit allowlist for SmsManager methods whose second parameter is the SMSC address.
 *
 * <p>Only signatures verified against the public Android SmsManager API are eligible. New
 * signatures must be added here with a corresponding unit test; method-name heuristics are not
 * permitted because vendor/API methods can reuse send* names with different parameter semantics.</p>
 */
final class HookSignatureRegistry {
    static final int SMSC_ARGUMENT_INDEX = 1;
    private static final String VOID = Void.TYPE.getName();
    private static final String STRING = String.class.getName();
    private static final String SHORT = Short.TYPE.getName();
    private static final String LONG = Long.TYPE.getName();
    private static final String BYTE_ARRAY = byte[].class.getName();
    private static final String PENDING_INTENT = "android.app.PendingIntent";
    private static final String LIST = "java.util.List";
    private static final String ARRAY_LIST = "java.util.ArrayList";

    private static final List<HookSignature> SUPPORTED_SIGNATURES = Collections.unmodifiableList(Arrays.asList(
            signature(
                    "sendTextMessage",
                    STRING, STRING, STRING, PENDING_INTENT, PENDING_INTENT
            ),
            signature(
                    "sendTextMessage",
                    STRING, STRING, STRING, PENDING_INTENT, PENDING_INTENT, LONG
            ),
            signature(
                    "sendDataMessage",
                    STRING, STRING, SHORT, BYTE_ARRAY, PENDING_INTENT, PENDING_INTENT
            ),
            signature(
                    "sendMultipartTextMessage",
                    STRING, STRING, ARRAY_LIST, ARRAY_LIST, ARRAY_LIST
            ),
            signature(
                    "sendMultipartTextMessage",
                    STRING, STRING, LIST, LIST, LIST, LONG
            ),
            signature(
                    "sendMultipartTextMessage",
                    STRING, STRING, LIST, LIST, LIST, STRING, STRING
            )
    ));

    private HookSignatureRegistry() {
    }

    static HookSignature match(Method method) {
        if (method == null || !VOID.equals(method.getReturnType().getName())) {
            return null;
        }
        String[] parameterTypeNames = new String[method.getParameterTypes().length];
        for (int index = 0; index < method.getParameterTypes().length; index++) {
            parameterTypeNames[index] = method.getParameterTypes()[index].getName();
        }
        return match(method.getName(), method.getReturnType().getName(), parameterTypeNames);
    }

    static HookSignature match(String methodName, String returnTypeName, String[] parameterTypeNames) {
        if (!VOID.equals(returnTypeName) || parameterTypeNames == null) {
            return null;
        }
        for (HookSignature signature : SUPPORTED_SIGNATURES) {
            if (signature.matches(methodName, parameterTypeNames)) {
                return signature;
            }
        }
        return null;
    }

    static List<HookSignature> supportedSignatures() {
        return SUPPORTED_SIGNATURES;
    }

    private static HookSignature signature(String methodName, String... parameterTypeNames) {
        return new HookSignature(methodName, parameterTypeNames, SMSC_ARGUMENT_INDEX, "Android public API level 4+");
    }

    static final class HookSignature {
        final String methodName;
        final String[] parameterTypeNames;
        final int smscArgumentIndex;
        final String compatibilityNote;

        HookSignature(
                String methodName,
                String[] parameterTypeNames,
                int smscArgumentIndex,
                String compatibilityNote
        ) {
            this.methodName = methodName;
            this.parameterTypeNames = parameterTypeNames.clone();
            this.smscArgumentIndex = smscArgumentIndex;
            this.compatibilityNote = compatibilityNote;
        }

        boolean matches(String candidateName, String[] candidateParameterTypeNames) {
            return methodName.equals(candidateName)
                    && Arrays.equals(parameterTypeNames, candidateParameterTypeNames);
        }

        String diagnosticName() {
            return methodName + Arrays.toString(parameterTypeNames);
        }
    }
}
