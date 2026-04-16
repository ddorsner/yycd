# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read First

`app/TECHNICAL-REFERENCE.md` is the single source of truth for API contracts, data models, naming conventions, design system, and known gotchas. Always check it before assuming anything about API shapes, field names, or style references.

## Build Commands (Windows)

```bash
.\gradlew.bat assembleDebug          # Build debug APK
.\gradlew.bat installDebug           # Build and install to connected device/emulator
.\gradlew.bat test                   # Unit tests
.\gradlew.bat connectedAndroidTest   # Instrumented tests (requires device/emulator)
.\gradlew.bat clean                  # Clean build outputs
adb install -r app\build\outputs\apk\debug\app-debug.apk  # Manual install
```

Never edit anything under `app/build/` — it is all auto-generated.

## Architecture

Three-screen native Android app (Kotlin + XML layouts + View Binding) that fetches content from a custom WordPress REST API and displays it. No Retrofit, no Ktor, no Gson — raw `HttpURLConnection` + `org.json` only. No dependency injection, no local database, no shared module.

**Screen flow:** `MainActivity` (splash + location picker) → `ArticleActivity` (post list) → `ArticleDetailActivity` (WebView)

**Networking pattern:** All HTTP calls use `HttpURLConnection` on `Dispatchers.IO` coroutines; UI updates posted back to `Dispatchers.Main`.

**API base URL:** `https://dandysite.com/yycd/wp-json/ds/v1` — this is a **custom plugin** (`ds/v1`), not the standard WordPress REST API (`wp/v2`). See `YYCD.rest` for endpoint examples.

## Critical API Gotchas

- `/locations` returns a **wrapped object** `{ "locations": [...] }` — not a bare array. Use `jsonObject.getJSONArray("locations")`.
- `/locations/{id}/posts` is **doubly nested**: `{ "posts": { "items": [...] } }`. Use `jsonObject.getJSONObject("posts").getJSONArray("items")`.
- `featured_image` can be boolean `false` (not `null`, not empty string). Always type-check the result of `postObject.get("featured_image")`.
- `latitude`/`longitude` are Strings in the API — parse with `.toDoubleOrNull() ?: 0.0`, never `.getDouble()`.
- `post.content` is raw HTML — always render in a WebView, never in a TextView.

## Key Source Files

All source under `app/src/main/java/com/dabbled/wordpressnewsletter/`:

| File | Role |
|---|---|
| `MainActivity.kt` | Entry point. Splash screen + location picker. Also defines `Location`, `SplashData` data classes. |
| `ArticleActivity.kt` | Post list for selected location with infinite scroll. Defines `WordPressPost`, `LocationDetail`, `PostAdapter`. |
| `ArticleDetailActivity.kt` | Renders article HTML in a WebView. |
| `ImageLoader.kt` | Singleton in-memory bitmap cache. Not persistent across process death. |

`WORDPRESS_URL` is hardcoded as a private `val` in both `MainActivity` and `ArticleActivity` — update both if the URL changes.

## Resources

- **Colors:** `res/values/colors.xml` (source of truth — use named tokens, not hardcoded hex)
- **Spacing/sizes:** `res/values/dimens.xml`
- **Styles:** `res/values-v23/styles.xml` — **all custom YYCD styles live here**, not in `values/styles.xml` (which does not exist)
- Style prefix convention: `TextAppearance.YYCD.*`, `Widget.YYCD.*`
- `old_logo.png` is in `drawable/` but must not be used — use `logo.png`

## Dependency Management

Third-party libraries are managed via the Gradle version catalog at `gradle/libs.versions.toml` and referenced as `libs.*` in `app/build.gradle.kts`. Add or update dependencies there, not inline in Gradle files.

## View IDs

Snake_case: `post_title`, `contact_button`, `btn_call`. Layout files follow `activity_[screen].xml`, `item_[type].xml`, `dialog_[name].xml`.

## `location_title_spacer`

A 75dp empty TextView present in all three activity layouts. It is a manual spacer for action bar offset — do not remove it.
