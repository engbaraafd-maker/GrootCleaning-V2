# GrootCleaning — Technical Constraints

## What Android permits

An enabled `AccessibilityService` can retrieve window content when configured with `canRetrieveWindowContent=true`; it can inspect the Accessibility tree and perform supported actions such as clicks, scrolling, and global back navigation. citeturn755895search2turn755895search0

## What is intentionally not attempted

- Root / Magisk / exploits
- Private framework APIs
- Direct deletion of another application's private data from the GrootCleaning process
- Direct arbitrary deletion of protected accounts through `AccountManager`
- Bypass of confirmation dialogs, privacy controls, authentication, or enterprise/OEM protections

## Storage verification

`StorageStatsManager.queryStatsForPackage()` is available from API 26 but querying another package requires Usage Access. The app treats this as optional evidence rather than a prerequisite for all operations. citeturn645590search4

## Android 8.1 baseline

Android 8.1 is API 27, so it is used as the minimum supported version. citeturn755895search1

## Accessibility policy note

The service is **not** marked as `isAccessibilityTool=true`, because GrootCleaning is not a disability-assistance tool. Any Play-distributed build must use a prominent in-app disclosure/consent and complete the applicable Accessibility declaration. Google Play permits deterministic rule-based automation but prohibits autonomous initiation/planning/execution and disallows bypassing platform security controls. citeturn276520search0turn276520search2


## Account visibility on Android 8+

`AccountManager` only exposes accounts that are visible to the calling app on modern Android. The app therefore requests the ordinary `GET_ACCOUNTS` permission where applicable, but does not treat that permission as universal access; an authenticator or explicit user-granted visibility may still be required. citeturn106121search0turn106121search3
