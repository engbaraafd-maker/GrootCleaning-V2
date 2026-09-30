# GrootCleaning

**App & Account Automation**

GrootCleaning is an Android 8.1+ automation dashboard for deterministic application/account maintenance workflows. It is designed around AccessibilityService, state detection, verification, logging and safe manual intervention — without Root, Magisk, exploits or hidden privileges.

## Project modules

- `ui/` — Dashboard, Applications, Accounts, Cleaning Progress, Logs, Settings, Automation Test.
- `automation/engine/` — Task queue, preflight, retry, storage/account automation.
- `automation/detection/` — Accessibility bridge, node matching and Arabic/English text catalogs.
- `automation/verification/` — storage statistics and verification policy.
- `automation/adapters/` — System UI abstraction for stock/OEM variants.
- `service/accessibility/` — actual AccessibilityService entry point.
- `data/` — Room entities/DAOs/database and selection repository.
- `models/` — task/run/step state models.

## UI implementation

The current implementation follows the GrootCleaning visual direction:

- Royal Black main background.
- Deep Navy Blue surfaces.
- Maroon primary controls.
- Emerald Green success/verified states.
- Minimal rounded cards and technical dashboard styling.

The mockup image is treated as a product specification, while the Android source remains the source of truth.

## Build

Open the `GrootCleaning` directory in a current Android Studio release compatible with AGP 9.4.0. The project requires Gradle 9.6.0 and JDK 17; AGP 9.4.0 supports API levels up to 37 and requires at least Gradle 9.6.0.

Install Android SDK Platform 36 and Build Tools 36.0.0, then sync and build the `app` module.

The current coding environment does not contain a local Android SDK/Gradle dependency cache, so an APK was not produced here.

## Safe test flow

1. Install/debug build on an Android 8.1+ device.
2. Launch GrootCleaning.
3. Accept the Accessibility disclosure.
4. Enable GrootCleaning in Android Accessibility settings.
5. Open Applications and choose targets/operations.
6. Enable Simulation Mode in Settings.
7. Run `Automation Test` first.
8. Run START CLEANING in Simulation Mode and confirm that controls are detected but no destructive action is performed.
9. After device/OEM validation, disable Simulation Mode for real execution.

## Expected automation states

- `SUCCESS`
- `FAILED`
- `SKIPPED`
- `REQUIRES_USER_ACTION`
- `VERIFICATION_UNCERTAIN`

A click is never treated as success by itself.
