# Copilot / AI Agent Instructions for YesYouCanDance (YYCD)

Short, actionable guidance to help AI agents make code changes quickly and safely.

## Big picture
- This is a small Android app (single module `app`) that reads content from a WordPress REST API. See `YYCD.rest` for the canonical endpoints and examples.
- App package: `com.dabbled.wordpressnewsletter`. UI + navigation live under `app/src/main/java` and resources under `app/src/main/res`.
- Build outputs and generated code (data-binding/navigation) appear in `app/build/` — prefer editing source files under `app/src/` and not touching generated files.

## Key files & locations
- Gradle configuration: `build.gradle.kts` (root), `settings.gradle.kts`, and `app/build.gradle.kts`.
- App entry and permissions: `app/src/main/AndroidManifest.xml`.
- REST API examples: `YYCD.rest` at repo root.
- Proguard: `app/proguard-rules.pro` (release build rules).

## Build / test / debug workflows (Windows / PowerShell)
- Build debug APK: `.
  .\\gradlew.bat assembleDebug` (run from repo root).
- Run unit tests: `.
  .\\gradlew.bat test`.
- Run connected Android tests (device/emulator required): `.
  .\\gradlew.bat connectedAndroidTest`.
- Install debug APK via adb: `adb install -r app\\build\\outputs\\apk\\debug\\app-debug.apk`.

Note: prefer using the provided `gradlew.bat` for reproducible toolchain.

## Project-specific patterns & conventions
- View binding is enabled (`buildFeatures.viewBinding = true`) — access views via generated binding classes rather than `findViewById`.
- Navigation component is used (see `libs.navigation.*` in `app/build.gradle.kts`) — update navigation XML under `res/navigation` when adding flows.
- Coroutines are used for background work (`kotlinx.coroutines` dependencies). Follow existing coroutine scopes and dispatchers used in the app.
- Target and source compatibility are Java 11 (see `compileOptions` / `kotlinOptions.jvmTarget`).

## Integration points / external dependencies
- WordPress REST API: URLs and example calls in `YYCD.rest`. Any API changes should be mirrored in that file and in network client code under `app/src/main/java`.
- Third-party libs are managed via the `libs.versions.toml` (see `gradle/libs.versions.toml`) and referenced via `libs.*` in Gradle files.

## Making safe changes
- Do not edit generated files under `app/build/` — change source under `app/src/` instead.
- When modifying app ID, versionName, or SDK versions, update `app/build.gradle.kts` and ensure CI/build scripts are still valid.
- For any network-related change, update `YYCD.rest` with example requests and test against a local emulator or device.

## Examples (quick references)
- Change minSdk / targetSdk: edit `app/build.gradle.kts` -> `defaultConfig`.
- Add a coroutine dependency: update `app/build.gradle.kts` and add imports/usages in `app/src/main/java`.
- Add a navigation screen: create layout in `res/layout`, add destination in `res/navigation/*.xml`, update fragment/activity code in `app/src/main/java`.

## When in doubt
- Preserve existing style and APIs. Keep changes minimal and focused.
- If a change affects build configuration or generated outputs, run `.
  .\\gradlew.bat assembleDebug` locally and report build issues.

If anything is unclear or you want the agent to include a different level of detail (tests, CI, release flow), tell me which area to expand.
