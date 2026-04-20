package com.smscfixer;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SmscFixer implements IXposedHookLoadPackage {
    private static final String TAG = "SmscFixer";
    private static final String FORCED_SMSC = "+20105996500";

    private static volatile boolean hookedInProcess;
    private static final Object HOOK_LOCK = new Object();

    private static final XC_MethodHook FORCE_SMSC_HOOK = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            if (param.args == null || param.args.length < 2) {
                return;
            }
            Object original = param.args[1];
            if (FORCED_SMSC.equals(original)) {
                return;
            }
            param.args[1] = FORCED_SMSC;
            XposedBridge.log(TAG + ": " + param.method.getName() + " scAddress="
                    + (original == null ? "null" : String.valueOf(original))
                    + " -> " + FORCED_SMSC);
        }
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        synchronized (HOOK_LOCK) {
            if (hookedInProcess) {
                return;
            }
        }

        final Class<?> smsManagerClass;
        try {
            smsManagerClass = XposedHelpers.findClass("android.telephony.SmsManager", lpparam.classLoader);
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": SmsManager not found: " + e);
            return;
        }

        int hookedCount = hookCompatibleSendMethods(smsManagerClass);
        synchronized (HOOK_LOCK) {
            hookedInProcess = hookedCount > 0;
        }
    }

    private static int hookCompatibleSendMethods(Class<?> clazz) {
        int hookedCount = 0;
        Set<String> seenSignatures = new HashSet<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (!isCompatibleSmsSendMethod(method)) {
                continue;
            }
            String signature = method.toString();
            if (!seenSignatures.add(signature)) {
                continue;
            }
            try {
                XposedBridge.hookMethod(method, FORCE_SMSC_HOOK);
                hookedCount++;
                XposedBridge.log(TAG + ": hooked " + method.getName() + " " + signature);
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": hook error in " + method.getName() + ": " + t);
            }
        }
        XposedBridge.log(TAG + ": compatible methods hooked=" + hookedCount);
        return hookedCount;
    }

    private static boolean isCompatibleSmsSendMethod(Method method) {
        if (!method.getName().startsWith("send")) {
            return false;
        }
        if (method.getReturnType() != Void.TYPE) {
            return false;
        }
        Class<?>[] params = method.getParameterTypes();
        return params.length >= 2
                && params[0] == String.class
                && params[1] == String.class;
    }
}
