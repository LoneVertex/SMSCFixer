package com.smscfixer;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class SmscSelectionConfig {
    public final int configVersion;
    public final String primarySmsc;
    public final String secondarySmsc;
    public final Map<String, String> mccMncFallbacks;
    public final Map<String, String> carrierNameFallbacks;
    public final Set<String> targetPackages;

    public SmscSelectionConfig(
            String primarySmsc,
            String secondarySmsc,
            Map<String, String> mccMncFallbacks,
            Map<String, String> carrierNameFallbacks,
            Set<String> targetPackages
    ) {
        this(
                SmscConfigSchema.CURRENT_VERSION,
                primarySmsc,
                secondarySmsc,
                mccMncFallbacks,
                carrierNameFallbacks,
                targetPackages
        );
    }

    public SmscSelectionConfig(
            int configVersion,
            String primarySmsc,
            String secondarySmsc,
            Map<String, String> mccMncFallbacks,
            Map<String, String> carrierNameFallbacks,
            Set<String> targetPackages
    ) {
        this.configVersion = Math.max(configVersion, SmscConfigSchema.CURRENT_VERSION);
        this.primarySmsc = primarySmsc;
        this.secondarySmsc = secondarySmsc;
        this.mccMncFallbacks = mccMncFallbacks == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(mccMncFallbacks));
        this.carrierNameFallbacks = carrierNameFallbacks == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(carrierNameFallbacks));
        this.targetPackages = targetPackages == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(targetPackages));
    }
}
