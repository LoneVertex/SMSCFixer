package io.github.lonevertex.smscguard;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

final class SmscConfigSchema {
    static final int CURRENT_VERSION = 1;
    static final String KEY_SCHEMA_VERSION = "config_schema_version";
    static final String KEY_PRIMARY_SMSC = "primary_smsc";
    static final String KEY_SECONDARY_SMSC = "secondary_smsc";
    static final String KEY_TARGET_PACKAGES_CSV = "target_packages_csv";
    static final String KEY_DIAGNOSTICS_ENABLED = "diagnostics_enabled";
    static final String DEFAULT_TARGET_PACKAGES_CSV = "com.google.android.apps.messaging,com.android.mms";

    private static final Pattern SMSC_PATTERN = Pattern.compile("^\\+?[0-9]{5,20}$");
    private static final Pattern STRICT_E164_PATTERN = Pattern.compile("^\\+[0-9]{5,20}$");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$");

    private SmscConfigSchema() {
    }

    static boolean isStrictE164(String value) {
        return value != null && STRICT_E164_PATTERN.matcher(value.trim()).matches();
    }

    static String normalizeSmscOrDefault(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || !isSmscFormatValid(trimmed)) {
            return fallback;
        }
        return trimmed.startsWith("+") ? trimmed : "+" + trimmed;
    }

    static boolean isSmscInputAcceptable(String raw) {
        return raw == null || raw.trim().isEmpty() || isSmscFormatValid(raw.trim());
    }

    static boolean isSmscFormatValid(String value) {
        return value != null && SMSC_PATTERN.matcher(value).matches();
    }

    /**
     * Produces a non-empty, validated scope for runtime use. Values supplied by an external or
     * manually edited preference file are treated as untrusted; invalid-only values fall back to
     * the known-safe default scope rather than enabling all packages.
     */
    static Set<String> parseAndNormalizeTargetPackages(String csv) {
        Set<String> parsed = SmscRuntimeConfig.parsePackages(csv);
        if (parsed.isEmpty()) {
            return defaultTargetPackages();
        }
        Set<String> valid = new LinkedHashSet<>();
        for (String pkg : parsed) {
            if (isValidPackageName(pkg)) {
                valid.add(pkg);
            }
        }
        return valid.isEmpty() ? defaultTargetPackages() : valid;
    }

    /**
     * UI validation is stricter than runtime recovery: any invalid non-empty package entry must
     * be corrected by the user instead of being silently dropped.
     */
    static boolean isTargetPackagesCsvAcceptable(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return true;
        }
        Set<String> parsed = SmscRuntimeConfig.parsePackages(csv);
        if (parsed.isEmpty()) {
            return false;
        }
        for (String pkg : parsed) {
            if (!isValidPackageName(pkg)) {
                return false;
            }
        }
        return true;
    }

    static String normalizeTargetPackagesCsv(String csv) {
        return String.join(",", parseAndNormalizeTargetPackages(csv));
    }

    private static Set<String> defaultTargetPackages() {
        return new LinkedHashSet<>(SmscRuntimeConfig.parsePackages(DEFAULT_TARGET_PACKAGES_CSV));
    }

    private static boolean isValidPackageName(String pkg) {
        return pkg != null && PACKAGE_PATTERN.matcher(pkg).matches();
    }
}
