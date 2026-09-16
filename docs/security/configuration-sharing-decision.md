# Configuration-Sharing Architecture Decision Record (ADR)

## ADR-002: Migration to Libxposed API 102 Service IPC (Current)

### Status
**Accepted & Implemented** (2026-09-16, Release `v2.0.0`)

### Context
In earlier iterations (ADR-001), SMSCFixer used legacy `xposedsharedprefs` and requested `Context.MODE_WORLD_READABLE` when obtaining preferences. On modern Android versions (Android 7.0+ through Android 16 API 36), `MODE_WORLD_READABLE` has been strictly deprecated and triggers `SecurityException`, while modern SELinux policies prevent cross-package reading of private application data directories.

Modern LSPosed and the Libxposed ecosystem introduce the official `io.github.libxposed.service` contract, providing IPC-based configuration synchronization without any filesystem permission compromises.

### Decision
1. **Adopt Libxposed Service IPC:** Integrate `io.github.libxposed:service:102.0.0` and register `io.github.libxposed.service.XposedProvider` in `AndroidManifest.xml` with authority `${applicationId}.xposedprovider`.
2. **Permanent Removal of Legacy Filesystem Sharing:** Completely remove `xposedsharedprefs` metadata, `Context.MODE_WORLD_READABLE`, and manual file permission modification scripts (`makePrefsReadableForXposed()`).
3. **Reactive Synchronization:** In `PreferencesManager.kt`, use `XposedServiceHelper.registerListener` to dynamically bind to the framework service and mirror configuration into `RemotePreferences`. On disconnection or service death, automatically recover and reconnect.
4. **Isolated Direct Fallback:** For local UI operations and standalone testing, `ConfigurationRepository` accesses standard application `SharedPreferences`. The hook engine reads from the service-synchronized `RemotePreferences`.

### Consequences & Benefits
- **Zero World-Readable Filesystem Exposure:** Module configuration XML files remain strictly private to `io.github.lonevertex.smscguard`.
- **SELinux Compliance:** Completely bypasses SELinux file traversal blocks because communication occurs via Binder IPC through the framework service.
- **Dynamic Hot Reloading:** Settings changes saved in the Compose UI are instantly propagated to the framework service and available to hooked processes without requiring immediate device reboots.

---

## ADR-001: Legacy Managed XSharedPreferences (Superseded)

### Status
**Superseded by ADR-002** (2026-09-16)

### Context
Legacy Xposed and early LSPosed (API 82–93) relied on `XSharedPreferences` reading directly from `/data/data/<package>/shared_prefs/`. This approach required either framework-level path redirection (`xposedsharedprefs=true`) or world-readable file modes.

### Historical Decision
Adopted LSPosed managed mode with fallback file chmod. Superseded because it failed to provide deterministic cross-process synchronization on Android 14+ without root namespace compromises.
