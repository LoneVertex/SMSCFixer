package io.github.lonevertex.smscguard;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class SmscRuntimeConfig {
    private SmscRuntimeConfig() {
    }

    static SmscSelectionConfig buildDefaultConfig(String primarySmsc, String secondarySmsc) {
        return buildConfig(
                primarySmsc,
                secondarySmsc,
                SmscConfigSchema.defaultTargetPackages(),
                SmscConfigSchema.CURRENT_VERSION
        );
    }

    static SmscSelectionConfig buildConfig(
            String primarySmsc,
            String secondarySmsc,
            Set<String> targetPackages,
            int configVersion
    ) {
        Set<String> effectiveTargets = targetPackages == null || targetPackages.isEmpty()
                ? SmscConfigSchema.defaultTargetPackages()
                : new LinkedHashSet<>(targetPackages);
        return new SmscSelectionConfig(
                configVersion,
                primarySmsc,
                secondarySmsc,
                defaultMccMncFallbacks(primarySmsc, secondarySmsc),
                defaultCarrierNameFallbacks(primarySmsc, secondarySmsc),
                effectiveTargets
        );
    }

    /**
     * The Android framework process is always a required LSPosed scope. An empty/invalid target
     * set must fail closed for other packages; it must never mean "intercept everything".
     */
    static boolean shouldHandlePackage(String packageName, Set<String> targetPackages) {
        if ("android".equals(packageName)) {
            return true;
        }
        return targetPackages != null && !targetPackages.isEmpty()
                && targetPackages.contains(packageName);
    }

    static Set<String> parsePackages(String csv) {
        return SmscConfigSchema.parsePackages(csv);
    }

    private static Map<String, String> defaultMccMncFallbacks(String primary, String secondary) {
        Map<String, String> map = new HashMap<>();
        map.put("60202", primary);   // Vodafone EG
        map.put("60201", secondary); // Orange EG
        return map;
    }

    private static Map<String, String> defaultCarrierNameFallbacks(String primary, String secondary) {
        Map<String, String> map = new HashMap<>();
        map.put(SmscSelector.normalizeCarrierName("vodafone"), primary);
        map.put(SmscSelector.normalizeCarrierName("vodafone egypt"), primary);
        map.put(SmscSelector.normalizeCarrierName("orange"), secondary);
        map.put(SmscSelector.normalizeCarrierName("orange egypt"), secondary);
        return map;
    }
}
