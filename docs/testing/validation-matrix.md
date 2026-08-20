# Validation Matrix

This matrix distinguishes automated repository checks from validation that requires an Android runtime, LSPosed, SIM subscriptions, or a real carrier delivery path. A repository build alone does not establish telephony compatibility.

| Layer | Environment | Coverage | Required evidence | Release gate |
|---|---|---|---|---|
| Unit | Java 17, Android SDK build host | Package-scope fallback, SMSC validation, policy decisions, MCC/MNC normalization, exact hook registry matching, cache TTL/bounds | Passing `./gradlew test` report | Required for every change |
| Lint/build | Java 17, Android SDK 36 | Manifest/resources, Java compilation, debug/release APK assembly | Passing `./gradlew lint assembleDebug assembleRelease`; APK paths | Required before review |
| Instrumentation | Emulator or physical Android device | Settings persistence/migration, save status, lifecycle, accessibility announcements, manifest surface | Instrumentation report with device/API image | Required before release |
| Rooted LSPosed | Rooted physical test device with LSPosed | Module loading, supported signature hook installation, config visibility, scope behavior, process restart/reboot | Log extract with redacted event names and device/LSPosed versions | Required for a supported profile |
| Dual-SIM routing | Controlled dual-SIM device | SIM1/SIM2 routing, unknown slot, conflicting signals, config changes, preserved original SMSC when uncertain | Per-case outcome row in compatibility registry | Required for dual-SIM rollout |
| Carrier delivery | Approved test destination and test SIM | Actual SMS delivery and carrier response | Controlled test record without message body or private numbers | Required for staged rollout |
| Rollback | Rooted device with known-good signed APK | Disable/reinstall/reboot/verify recovery | Timestamped rollback rehearsal record | Required before production rollout |

## Required rooted-device cases

| ID | Scenario | Expected result |
|---|---|---|
| D-01 | Module scoped to `android` plus a supported messaging package | `hook_applied` event occurs only in the intended scope. |
| D-02 | Send through a registered `sendTextMessage` signature using SIM1 | A replacement occurs only when slot 0 is resolved; diagnostic reason is `SLOT_PRIMARY`. |
| D-03 | Send through a registered signature using SIM2 | A replacement occurs only when slot 1 is resolved; diagnostic reason is `SLOT_SECONDARY`. |
| D-04 | Slot unknown, valid MCC/MNC fallback | Replacement occurs with `MCCMNC_FALLBACK`. |
| D-05 | Slot unknown, valid carrier-name fallback | Replacement occurs with `CARRIER_FALLBACK`. |
| D-06 | Slot unknown and no carrier signals | Original SMSC is preserved; no forced primary fallback. |
| D-07 | Slot unknown with conflicting MCC/MNC and carrier mappings | Original SMSC is preserved; diagnostic reason is `AMBIGUOUS_CARRIER_SIGNALS`. |
| D-08 | Unsupported or vendor-specific `send*` method | Method is not hooked and no argument is changed. |
| D-09 | Malformed stored target-package CSV | Runtime uses the validated default package scope; arbitrary packages are not hooked. |
| D-10 | Change settings then reboot/restart scoped process | New valid configuration becomes active and no sensitive default logs are emitted. |
| D-11 | LSPosed API 93+ with `xposedsharedprefs` metadata | Saving settings uses managed preference storage; the hooked process reads validated configuration without legacy directory permission changes. |
| D-12 | Legacy or unsupported preference manager | The manager-mode request is rejected, the single XML fallback remains readable, and no app-data or shared_prefs directory permission is broadened. |

## Evidence handling

Do not record SMS bodies, raw destination numbers, IMSI/ICCID, full device fingerprints, original SMSC values, or full carrier identifiers in repository issues or CI logs. Record only the test case ID, app package, device/ROM/API/LSPosed versions, SIM count, redacted event result, date, operator, and link to secured detailed evidence if needed.
