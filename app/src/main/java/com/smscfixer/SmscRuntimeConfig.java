package com.smscfixer;

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
                new LinkedHashSet<>(java.util.Arrays.asList(
                        "com.google.android.apps.messaging",
                        "com.android.mms"
                )),
                SmscConfigSchema.CURRENT_VERSION
        );
    }

    static SmscSelectionConfig buildConfig(
            String primarySmsc,
            String secondarySmsc,
            Set<String> targetPackages
    ) {
        return buildConfig(primarySmsc, secondarySmsc, targetPackages, SmscConfigSchema.CURRENT_VERSION);
    }

    static SmscSelectionConfig buildConfig(
            String primarySmsc,
            String secondarySmsc,
            Set<String> targetPackages,
            int configVersion
    ) {
        return new SmscSelectionConfig(
                configVersion,
                primarySmsc,
                secondarySmsc,
                defaultMccMncFallbacks(primarySmsc, secondarySmsc),
                defaultCarrierNameFallbacks(primarySmsc, secondarySmsc),
                targetPackages
        );
    }

    static boolean shouldHandlePackage(String packageName, Set<String> targetPackages) {
        if ("android".equals(packageName)) {
            return true;
        }
        if (targetPackages == null || targetPackages.isEmpty()) {
            return true;
        }
        return targetPackages.contains(packageName);
    }

    static Set<String> parsePackages(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String item : csv.split(",")) {
            String pkg = item.trim();
            if (!pkg.isEmpty()) {
                out.add(pkg);
            }
        }
        return out;
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
