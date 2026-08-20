# Known-device compatibility registry

| Device / test profile | ROM/API | Dual-SIM | Result | Notes | Last validated |
|---|---|---|---|---|---|
| Samsung A21s (SM-A217F) | Lineage-based Android 16/API 36 | Yes | baseline target | Requires diagnostics for vendor variance | 2026-04-21 |
| User-reported rooted-phone Vodafone path | Not recorded | Not recorded | **Observed pass** | SMS send succeeded. Formal D-case mapping remains pending device/API/LSPosed/SIM-slot metadata. | 2026-08-20 |
| User-reported rooted-phone Orange path | Not recorded | Not recorded | **Inconclusive** | Delivery was attempted without sufficient SIM balance; this is not a product-failure determination. Repeat after provisioning the SIM. | 2026-08-20 |

Update policy:
- Add entry after each staged rollout validation checkpoint.
- Mark regressions with rollback reference and incident link.
