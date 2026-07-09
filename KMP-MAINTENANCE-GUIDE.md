# YYCD KMP Maintenance Guide

Technical documentation for maintaining the Yes You Can Dance (YYCD) Kotlin Multiplatform mobile application.

**Last updated:** July 2026  
**Supersedes:** the Android-only sections of TECHNICAL-REFERENCE.md

---

## 1. Architecture Overview

YYCD is a Kotlin Multiplatform (KMP) project. All networking, data models, and API logic live in a single shared Kotlin module consumed by two native UI layers.

```
┌─────────────────────┐        ┌─────────────────────┐
│   Android UI        │        │   iOS UI            │
│   (Kotlin, XML,     │        │   (Swift, SwiftUI)  │
│   Activities)       │        │                     │
└─────────┬───────────┘        └─────────┬───────────┘
          │ Gradle project dep           │ CocoaPods framework
          ▼                              ▼
┌─────────────────────────────────────────────────────┐
│               :shared (KMP module)                  │
│                                                     │
│  commonMain      YYCDRepository, Models, ApiConfig  │
│  androidMain     Ktor OkHttp engine                 │
│  iosMain         Ktor Darwin engine                 │
└─────────────────────────┬───────────────────────────┘
                          │ HTTPS (Ktor)
                          ▼
        WordPress REST API (custom ds/v1 namespace)
        https://dandysite.com/yycd/wp-json/ds/v1
```

Rule of thumb for maintainers: **if it touches the network or parses JSON, it belongs in `shared/commonMain`. If it touches a screen, it belongs in the platform app.**

---

## 2. Repository Layout

```
yycd/
├── app/                          # Android application module
│   └── src/main/
│       ├── java/com/dabbled/wordpressnewsletter/
│       │   ├── MainActivity.kt           # Splash + location spinner (entry point)
│       │   ├── ArticleActivity.kt        # Post list, infinite scroll, contact dialog
│       │   ├── ArticleDetailActivity.kt  # WebView article rendering
│       │   └── ImageLoader.kt            # Android-only image loading helper
│       └── res/                          # Layouts, drawables, colors
│
├── shared/                       # KMP shared module
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── ApiConfig.kt                          # Base URL + page size constants
│       │   └── com/dabbled/yycd/
│       │       ├── YYCDRepository.kt                 # All API calls
│       │       ├── model/Models.kt                   # Data classes + custom serializers
│       │       └── network/HttpClientFactory.kt      # expect declaration
│       ├── androidMain/kotlin/com/dabbled/yycd/
│       │   └── network/HttpClientFactory.kt          # actual: OkHttp engine
│       └── iosMain/kotlin/com/dabbled/yycd/
│           └── network/HttpClientFactory.kt          # actual: Darwin engine
│
├── iosApp/                       # iOS application (SwiftUI)
│   ├── Podfile                   # Links the shared pod
│   └── iosApp/
│       ├── iosAppApp.swift               # @main entry point
│       ├── ContentView.swift             # Splash screen; owns the repository
│       ├── LocationPickerView.swift      # Location list
│       ├── ArticleListView.swift         # Post list, infinite scroll, contact sheet
│       ├── ArticleDetailView.swift       # WKWebView article rendering
│       └── ContactView.swift             # Call/text/email/directions sheet
│
├── build.gradle.kts              # Root build (plugin declarations)
├── settings.gradle.kts           # Includes :app and :shared
└── gradle/libs.versions.toml     # Version catalog (single source for versions)
```

---

## 3. The Shared Module

### 3.1 YYCDRepository

`shared/src/commonMain/.../YYCDRepository.kt` is the only class either platform should use for data access.

| Function | Endpoint | Returns |
|---|---|---|
| `getSplashData()` | `GET /splash` | `SplashData` |
| `getLocations()` | `GET /locations` | `List<Location>` (id + name) |
| `getLocationDetail(locationId)` | `GET /locations` (filtered client-side) | `LocationDetail` |
| `getPostsForLocation(locationId, page)` | `GET /locations/{id}/posts?per_page=10&page=N` | `List<WordPressPost>` |
| `close()` | n/a | Closes the Ktor client |

All fetch functions are `suspend`. On Android call them from `lifecycleScope`; on iOS they surface as `async` functions callable with `try await`.

**Lifecycle rule:** each `YYCDRepository` instance owns an HTTP client. Call `close()` when the owner is destroyed (Android `onDestroy`). On iOS, `ContentView` creates one instance and passes it down the view hierarchy; it lives for the app session.

### 3.2 Data Models and API Quirks

`Models.kt` contains all data classes. The WordPress plugin returns several non-standard shapes. **These quirks are load-bearing. Do not "simplify" them without verifying against the live API.**

| Quirk | Where handled |
|---|---|
| `featured_image` is a String URL **or** boolean `false` | `FeaturedImageSerializer` normalizes to String ("" = no image) |
| `latitude`/`longitude` arrive as **Strings**, not numbers | `LatLngStringSerializer` parses with `toDoubleOrNull()`, defaults 0.0 |
| `/locations` returns a wrapped object `{ "locations": [...] }`, not a bare array | `LocationsResponse` |
| Posts response is doubly nested `{ "posts": { "items": [...] } }` | `PostsResponse` + `PostsItems` |
| `LocationsResponse` must deserialize `List<LocationDetail>` (not `List<Location>`) or phone/email/lat/lng are silently lost | Declared in `Models.kt`; see Contact feature notes below |

The Ktor JSON config uses `ignoreUnknownKeys = true` and `coerceInputValues = true`, so new API fields will not break existing clients.

### 3.3 expect/actual: HttpClientFactory

`commonMain` declares `internal expect fun createHttpClient(): HttpClient`. Each platform provides the engine:

- **androidMain** uses the OkHttp engine
- **iosMain** uses the Darwin engine (NSURLSession under the hood)

Both install ContentNegotiation (JSON) and Logging (INFO level). If you need to change JSON behavior or add a timeout, change **both** actuals so behavior stays identical across platforms.

---

## 4. Platform Notes

### 4.1 Android

- UI is classic Views (XML layouts + Activities), not Compose.
- Activities create their own `YYCDRepository` and must call `repository.close()` in `onDestroy()`.
- Network calls are launched in coroutines and results are applied on `Dispatchers.Main`.
- Pagination: `ArticleActivity` uses a `RecyclerView.OnScrollListener`, fetching the next page when within 2 items of the bottom. `hasMorePages` is true while a page returns exactly `POSTS_PER_PAGE` (10) items.
- Article content is raw HTML from the API. It must be rendered in a WebView (`ArticleDetailActivity`), never a TextView.

### 4.2 iOS

- SwiftUI throughout; navigation via `NavigationView`/`NavigationLink`.
- Kotlin `suspend` functions are consumed as Swift `async` via the generated framework. Kotlin `Int` becomes `Int32` in Swift, so calls look like `repository.getPostsForLocation(locationId: Int32(location.id), page: nextPage)`.
- Infinite scroll: `ArticleListView` triggers `loadMorePosts()` in `onAppear` of the last row.
- Article HTML is rendered in a `WKWebView` wrapped in `UIViewRepresentable` (`ArticleDetailView.swift`), with injected CSS for mobile-friendly rendering.

#### The contact sheet timing pattern (important)

`sheet(isPresented:)` with Kotlin-backed optional state races SwiftUI's render cycle: the sheet can present before the data binding lands, showing a blank sheet. The working pattern in `ArticleListView.swift` is:

```swift
struct IdentifiableLocationDetail: Identifiable {
    let id: Int32
    let detail: LocationDetail
}

@State private var contactDetail: IdentifiableLocationDetail? = nil

.sheet(item: $contactDetail) { item in
    ContactView(location: item.detail)
}
```

`sheet(item:)` only presents once the item is non-nil, eliminating the race. The wrapper struct exists because conforming the imported Kotlin type `LocationDetail` directly to `Identifiable` produces a compiler warning about retroactive conformance. **Keep this pattern for any future sheet fed by Kotlin data.**

---

## 5. Build and Development Workflows

### 5.1 Prerequisites

| Tool | Windows (Android only) | Mac (Android + iOS) |
|---|---|---|
| Android Studio + Kotlin Multiplatform plugin | ✅ | ✅ |
| JDK 17 | ✅ | ✅ (`brew install --cask zulu@17`) |
| Xcode + command line tools | — | ✅ (select in Xcode > Settings > Locations) |
| CocoaPods | — | ✅ (`sudo gem install cocoapods`; requires `LC_ALL=en_US.UTF-8` in `~/.zprofile`) |

Verify a Mac environment with `kdoctor` (`brew install kdoctor`). A warning about the "Kotlin Multiplatform Mobile plugin" not being installed is a false positive; the plugin was renamed to "Kotlin Multiplatform."

### 5.2 Android build

Open the repo root in Android Studio, sync, and run the `app` configuration. Or from the CLI:

```bash
./gradlew :app:assembleDebug
```

### 5.3 iOS build

Always open the **workspace**, not the project:

```bash
open iosApp/iosApp.xcworkspace
```

(You can confirm the workspace is open by the presence of a `Pods` project in the navigator.)

Build/run with Cmd+B / Cmd+R. The Xcode build invokes Gradle to compile the shared framework automatically via the CocoaPods integration.

### 5.4 After changing the shared module

Shared code changes flow to Android automatically on the next Gradle build. For iOS:

```bash
./gradlew :shared:podInstall      # if the pod spec / structure changed
cd iosApp && pod install          # re-link
```

For ordinary code changes inside existing files, a normal Xcode build is enough.

### 5.5 Common failure modes

| Symptom | Cause | Fix |
|---|---|---|
| `podInstall` fails: Podfile doesn't exist | Podfile path in `shared/build.gradle.kts` cocoapods block doesn't match | Ensure `iosApp/Podfile` exists at repo root level (`../iosApp/Podfile` relative to shared) |
| No `Shared` framework in Xcode | Opened `.xcodeproj` instead of `.xcworkspace`, or framework never built | Open the workspace; run `./gradlew :shared:assembleDebug` then `pod install` |
| `Error resolving plugin com.android.library ... already on the classpath` | Version specified on a plugin the root project already loads | In `shared/build.gradle.kts` use `id("com.android.library")` without a version |
| `androidMain` missing from IDE tree | `androidTarget()` not declared in shared kotlin block | Add `androidTarget { }` and re-sync |
| Blank contact sheet on iOS | `sheet(isPresented:)` raced Kotlin data | Use the `sheet(item:)` pattern in section 4.2 |
| Contact fields all empty | `LocationsResponse` deserializing `List<Location>` instead of `List<LocationDetail>` | Keep `LocationsResponse(val locations: List<LocationDetail>)` |
| `gradlew: permission denied` (Mac) | Executable bit lost | `chmod +x gradlew` |

---

## 6. API Reference

Base URL: `https://dandysite.com/yycd/wp-json/ds/v1` (defined once in `ApiConfig.kt`).

| Endpoint | Method | Response shape | Notes |
|---|---|---|---|
| `/splash` | GET | `{ title_url, splash_url, splash_text }` | Splash screen assets |
| `/locations` | GET | `{ locations: [ { id, name, phone, email, latitude, longitude } ] }` | Wrapped object; lat/lng are strings |
| `/locations/{id}/posts` | GET | `{ posts: { items: [...] } }` | Paginated: `?per_page=10&page=N` (1-indexed) |

Post item fields: `id, title, url, excerpt, content (raw HTML), date, featured_image (String or false), sticky`.

The backend is a custom WordPress plugin. If the plugin changes response shapes, update `Models.kt` serializers and wrappers first, then verify both apps.

---

## 7. Adding a Feature: Checklist

Example: adding a new endpoint or field.

1. Hit the live endpoint and inspect the **actual** JSON (see `YYCD.rest` for request templates). Do not trust assumed shapes; this API has non-standard fields.
2. Add/modify data classes in `shared/commonMain/.../model/Models.kt`. Add custom serializers for any polymorphic or string-typed numeric fields.
3. Add the repository function in `YYCDRepository.kt` (suspend, returns domain types, unwraps response wrappers).
4. Android: call from `lifecycleScope`, update UI on Main.
5. iOS: call with `try await`; remember Kotlin `Int` maps to `Int32`.
6. Rebuild the shared framework for iOS (section 5.4).
7. Test both platforms against the live API before merging.

---

## 8. Version Management

All dependency versions live in `gradle/libs.versions.toml`. Key pins:

| Component | Version | Notes |
|---|---|---|
| Kotlin | 2.2.10 | Shared by KMP plugin and serialization plugin |
| AGP | 9.0.x | `com.android.library` applied without version in shared |
| Ktor | 3.1.3 | All client artifacts share this version ref |
| kotlinx.serialization | 1.8.1 | |
| kotlinx.coroutines | 1.10.2 | |
| iOS deployment target | 14.0 | Set in both the cocoapods block and Podfile; keep in sync |

When upgrading Kotlin or AGP, upgrade in the version catalog only, sync, and rebuild both platforms. Kotlin and Ktor upgrades require regenerating the iOS framework (`./gradlew :shared:assembleDebug` + `pod install`).

---

## 9. Known Technical Debt

Items identified in code review, in priority order:

1. **Git hygiene:** stale `app/iosApp/` files, `.kotlin/` build metadata, `.idea/`, `Pods/`, and `xcuserdata` files are tracked. Untrack them and extend `.gitignore`.
2. **Coroutine scopes:** Android Activities use `CoroutineScope(Dispatchers.IO)` instead of `lifecycleScope`; requests are not cancelled if the Activity is destroyed mid-flight.
3. **Duplicate Gradle dependencies:** `app/build.gradle.kts` declares appcompat, material, constraintlayout, core-ktx, and junit multiple times at different versions.
4. **Template leftovers:** `Platform.kt` (+ android/ios actuals) and Example test files from the module wizard are dead code.
5. **File/package mismatches:** `ApiConfig.kt` and `YYCDRepository.kt` directory paths don't match their package declarations.
6. **No HTTP timeouts:** Ktor clients use engine defaults; add `HttpTimeout` to both actuals.
7. **`getLocationDetail` robustness:** re-fetches the full location list per call and throws if the ID is missing; consider caching and `firstOrNull`.
8. **No shared-module tests:** the serializers (the riskiest code in the project) have no unit tests. Adding `commonTest` cases for `FeaturedImageSerializer` and `LatLngStringSerializer` with real API payload samples would be the highest-value tests to write.
