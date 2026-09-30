# Suggested physical-device test matrix

| OS | Baseline | Test scope |
|---|---|---|
| Android 8.1 (API 27) | Required | Settings navigation, Accessibility tree, Storage page, account page |
| Android 9–11 | Recommended | Storage wording/state variants |
| Android 12–14 | Recommended | package visibility, Settings changes, disclosure flow |
| Android 15–16 | Required before release | target behavior, new Settings layouts, background lifecycle |
| Samsung One UI | Recommended | adapter/resource IDs |
| Xiaomi MIUI/HyperOS | Recommended | Manage Space flow |
| Huawei EMUI | Recommended | Settings/account variations |
| OPPO/ColorOS | Recommended | Storage/account variations |

The test must start in Simulation Mode, then move to non-destructive detection, then to controlled destructive tests on dedicated test accounts/apps.
