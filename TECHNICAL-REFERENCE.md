# TECHNICAL-REFERENCE.md

> ⚠️ **IMPORTANT — READ THIS FIRST ON EVERY NEW SESSION**
> This document is the single source of truth for this project. **Never guess at names, keys, file paths, class names, function names, or API endpoints.** Always verify here first. If something is missing or has changed, update this document before continuing work.

---

## 1. Architecture Overview

### Platform Summary
| Platform | Language | UI Framework | Package |
|---|---|---|---|
| Android | Kotlin | XML Layouts + View Binding | `com.dabbled.wordpressnewsletter` |
| iOS | Swift | UIKit | `[REPO_NAME_IOS — to be filled in]` |

> ⚠️ **Note:** Despite earlier planning, there is **no KMP shared module** in the current Android codebase. The Android app is fully self-contained. iOS is a separate repo.

### How the Pieces Connect
- The **Android app** is a standalone Kotlin app. All networking, parsing, and UI is done directly within Activities — no shared module.
- API calls are made using raw **`HttpURLConnection`** on `Dispatchers.IO` coroutines — no Retrofit, no Ktor.
- JSON is parsed manually using **`org.json.JSONObject` / `JSONArray`** — no Gson, no Moshi, no kotlinx.serialization.
- The **iOS app** is a separate repo using UIKit — see iOS repo for its own details.
- Both apps communicate with a **custom WordPress REST API plugin** at `https://dandysite.com/yycd/wp-json/ds/v1` — this is **not** the standard WP REST API.
- There is **no local storage or caching** except for a simple in-memory bitmap cache in `ImageLoader`.
- There is **no dependency injection framework** — dependencies are instantiated directly in Activities.

### Key Dependencies (Android — `build.gradle.kts`)
| Dependency | Purpose |
|---|---|
| `appcompat` | AppCompatActivity base, action bar support |
| `material` | Material3 theme, CardView styling |
| `constraintlayout` | Layout system |
| `recyclerview` | Post list in ArticleActivity |
| `cardview` | Post cards (`item_post.xml`) |
| `kotlinx.coroutines.core` + `.android` | Async API calls on IO/Main dispatchers |
| `core.ktx` | Kotlin Android extensions |
| `navigation.fragment` + `.ui` | Declared but not actively used yet |

---

## 2. Data Model / Structure

### WordPress REST API
> ⚠️ This is a **custom API plugin** (`ds/v1`), not the standard WordPress REST API (`wp/v2`).

Base URL: `https://dandysite.com/yycd/wp-json/ds/v1`

#### Core Endpoints Used
| Endpoint | Method | Returns | Notes |
|---|---|---|---|
| `/splash` | GET | `SplashData` object | Title image URL, splash image URL, splash text |
| `/locations` | GET | `{ locations: [...] }` object | Full list with id, name, phone, email, lat/lng |
| `/locations/{id}/posts` | GET | `{ location, posts: { items: [...] } }` | Paginated. Params: `?per_page=10&page=1` |

> ⚠️ **The locations endpoint returns a wrapped object, not a bare array.** Must call `jsonObject.getJSONArray("locations")` — not parse the response directly as a JSONArray.

> ⚠️ **The posts endpoint is doubly nested.** Response shape is `{ posts: { items: [...] } }` — must call `jsonObject.getJSONObject("posts").getJSONArray("items")`.

#### Key Data Classes (Android — all defined inline in source files)

**`Location`** — defined in `MainActivity.kt`
```
Location
  ├── id: Int
  └── name: String
```

**`SplashData`** — defined in `MainActivity.kt`
```
SplashData
  ├── titleUrl: String      // maps to API field: title_url
  ├── splashUrl: String     // maps to API field: splash_url
  └── splashText: String    // maps to API field: splash_text
```

**`WordPressPost`** — defined in `ArticleActivity.kt`
```
WordPressPost
  ├── id: Int
  ├── title: String
  ├── url: String
  ├── excerpt: String
  ├── content: String       // raw HTML — must render in WebView, not TextView
  ├── date: String
  ├── featuredImage: String // URL string OR empty string (API returns false boolean when no image)
  └── sticky: Boolean
```

**`LocationDetail`** — defined in `ArticleActivity.kt`
```
LocationDetail
  ├── id: Int
  ├── title: String
  ├── phone: String
  ├── email: String
  ├── latitude: Double      // stored as String in API, parsed with toDoubleOrNull()
  └── longitude: Double
```

#### Pagination
- Posts are paginated: `?per_page=10&page=1` (1-indexed)
- `hasMorePages` is set to `false` when a page returns fewer items than `postsPerPage`
- Infinite scroll is implemented via `RecyclerView.OnScrollListener` — triggers when within 2 items of the bottom

---

## 3. File & Folder Structure

### Android Repo
```
app/
├── src/main/
│   ├── AndroidManifest.xml               # App config, activity declarations, permissions
│   ├── java/com/dabbled/wordpressnewsletter/
│   │   ├── MainActivity.kt               # Splash screen + location picker (entry point)
│   │   ├── ArticleActivity.kt            # Post list for a selected location
│   │   │                                 # Also contains: WordPressPost, LocationDetail,
│   │   │                                 #   PostAdapter (RecyclerView adapter)
│   │   ├── ArticleDetailActivity.kt      # Full article rendered in WebView
│   │   └── ImageLoader.kt               # Singleton image loader with in-memory cache
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml         # Splash/location picker screen
│       │   ├── activity_article.xml      # Post list screen with contact button
│       │   ├── activity_article_detail.xml # WebView article detail screen
│       │   ├── item_post.xml             # RecyclerView card item
│       │   └── dialog_contact.xml        # Contact options dialog (call/text/email/directions)
│       ├── drawable/
│       │   ├── button_primary_background.xml  # Solid primary color, radius_base corners
│       │   ├── card_border.xml           # White card with border_light stroke
│       │   ├── card_border_primary.xml   # White card with primary border stroke (2dp)
│       │   ├── logo.png                  # Current app logo
│       │   └── old_logo.png              # ⚠️ Legacy — do not use
│       ├── menu/
│       │   └── menu_main.xml             # Settings overflow menu item
│       ├── values/
│       │   ├── colors.xml                # ⚠️ Source of truth for all colors
│       │   ├── dimens.xml                # ⚠️ Source of truth for spacing/sizes
│       │   ├── strings.xml               # All user-facing strings + WordPress URL
│       │   └── themes.xml                # Theme.YesYouCanDance (Material3 DayNight)
│       ├── values-v23/
│       │   └── styles.xml                # ⚠️ Active style definitions — YYCD text/button/card styles
│       ├── values-night/
│       │   └── themes.xml                # Dark mode theme override
│       ├── values-land/
│       │   └── dimens.xml                # Landscape dimen overrides (fab_margin: 48dp)
│       ├── values-w600dp/
│       │   └── dimens.xml                # Tablet (600dp+) dimen overrides
│       └── values-w1240dp/
│           └── dimens.xml                # Large screen (1240dp+) dimen overrides
└── build.gradle.kts                      # App dependencies and build config
```

> ⚠️ **Never edit directly:** Anything under `build/` — all auto-generated.
> ⚠️ **`old_logo.png`** is present in `drawable/` but should not be used — `logo.png` is current.
> ⚠️ **Styles are in `values-v23/styles.xml`**, not `values/styles.xml` — this is the active file for all YYCD text, button, and card styles.

---

## 4. Naming Conventions

### Kotlin (Android)
| Type | Convention | Example |
|---|---|---|
| Classes | PascalCase | `MainActivity`, `PostAdapter`, `ArticleDetailActivity` |
| Functions | camelCase | `fetchPosts()`, `parsePostsJson()`, `setupViews()` |
| Variables | camelCase | `locationDetail`, `isLoading`, `currentPage` |
| Constants | SCREAMING_SNAKE_CASE | `WORDPRESS_URL`, `postsPerPage` (note: some are `val` not `const`) |
| Files | PascalCase matching class | `ArticleActivity.kt`, `ImageLoader.kt` |
| Packages | lowercase dot-separated | `com.dabbled.wordpressnewsletter` |

### View IDs (XML Layouts)
The project uses **snake_case** for View IDs:

| Pattern | Example |
|---|---|
| `[widget_type]_[description]` | `post_title`, `post_excerpt`, `post_date` |
| `[purpose]_[widget_type]` | `contact_button`, `recycler_view`, `location_spinner` |
| `btn_[action]` | `btn_call`, `btn_text`, `btn_email`, `btn_directions` |
| `[name]_image` | `logo_image`, `location_title_sp` (splash image) |

### XML Layouts
| Type | Convention | Example |
|---|---|---|
| Activity layouts | `activity_[screen].xml` | `activity_main.xml`, `activity_article.xml` |
| List item layouts | `item_[type].xml` | `item_post.xml` |
| Dialog layouts | `dialog_[name].xml` | `dialog_contact.xml` |

### Style / Resource Naming
| Type | Prefix | Example |
|---|---|---|
| Text styles | `TextAppearance.YYCD.` | `TextAppearance.YYCD.Heading`, `.Body`, `.Caption` |
| Button styles | `Widget.YYCD.Button.` | `Widget.YYCD.Button.Primary` |
| Card styles | `Widget.YYCD.CardView` | `Widget.YYCD.CardView`, `.Featured` |
| Drawables — buttons | `button_[description]_background` | `button_primary_background` |
| Drawables — cards | `card_[variant]` | `card_border`, `card_border_primary` |

---

## 5. Design System

### Colors

#### Android (`res/values/colors.xml`)

**Primary Palette**
| Name | Hex | Usage |
|---|---|---|
| `primary` | `#38427A` | Primary brand color, action buttons, borders |
| `primary_medium` | `#5A65A1` | Hover/pressed states, secondary actions |
| `primary_light` | `#7A84B5` | Subtle highlights, disabled states |
| `background_tint` | `#F5F7FA` | Screen backgrounds, list backgrounds |
| `white` | `#FFFFFF` | Surfaces, cards, overlays |
| `black` | `#000000` | High-contrast text, overlays |

**Text Colors**
| Name | Hex | Usage |
|---|---|---|
| `text_primary` | `#38427A` | Headings, primary labels |
| `text_secondary` | `#5A65A1` | Subheadings, links |
| `text_body` | `#666666` | Body copy, descriptions |
| `text_meta` | `#999999` | Timestamps, captions, meta info |

**Borders**
| Name | Hex | Usage |
|---|---|---|
| `border_light` | `#F0F0F0` | Dividers, subtle separators |
| `border_primary` | `#38427A` | Focused inputs, active borders |

#### iOS (`Assets.xcassets / Color Set`)
| Name | Light Hex | Dark Hex | Usage |
|---|---|---|---|
| `[ColorName]` | `[#______]` | `[#______]` | `[usage]` |

### Typography (from `values-v23/styles.xml`)
| Style Name | Size | Color | Weight | Font | Usage |
|---|---|---|---|---|---|
| `TextAppearance.YYCD.Heading` | `24sp` | `text_primary` (`#38427A`) | Bold | serif | Screen/section headings |
| `TextAppearance.YYCD.Subheading` | `18sp` | `text_primary` (`#38427A`) | Bold | default | Post titles in cards |
| `TextAppearance.YYCD.Body` | `16sp` | `text_body` (`#666666`) | Regular | default | Post excerpts, body copy |
| `TextAppearance.YYCD.Caption` | `12sp` | `text_meta` (`#999999`) | Regular | default | Post dates, meta info |

### Spacing Scale
| Token | Value | Usage |
|---|---|---|
| `spacing_xs` | `4dp` | Tight inner padding |
| `spacing_sm` | `8dp` | Standard inner padding |
| `spacing_md` | `12dp` | Default section spacing |
| `spacing_base` | `16dp` | Base layout padding |
| `spacing_lg` | `24dp` | Large section gaps |
| `spacing_xl` | `32dp` | Screen-level margins |

### Border Radius Scale
| Token | Value | Usage |
|---|---|---|
| `radius_sm` | `8dp` | Small cards, chips, badges |
| `radius_base` | `12dp` | Default card radius |
| `radius_lg` | `16dp` | Large cards, bottom sheets |

### Component Sizes
| Token | Value | Usage |
|---|---|---|
| `badge_size_small` | `40dp` | Small avatar/badge |
| `badge_size_large` | `60dp` | Large avatar/badge |
| `news_image_height` | `160dp` | News card thumbnail height |
| `hero_image_height` | `200dp` | Hero/featured image height |

### Key Style / Theme References
| Resource | File | What it controls |
|---|---|---|
| `Theme.YesYouCanDance` | `values/themes.xml` | Active app theme — extends Material3 DayNight |
| `Base.Theme.YesYouCanDance` | `values-night/themes.xml` | Dark mode variant (no custom overrides yet) |
| `AppTheme` | `values-v23/styles.xml` | AppCompat theme with `colorPrimary = primary`, `colorAccent = primary_medium` |
| `TextAppearance.YYCD.Heading` | `values-v23/styles.xml` | 24sp serif bold, `text_primary` color |
| `TextAppearance.YYCD.Subheading` | `values-v23/styles.xml` | 18sp bold, `text_primary` color |
| `TextAppearance.YYCD.Body` | `values-v23/styles.xml` | 16sp regular, `text_body` color, 24sp line height |
| `TextAppearance.YYCD.Caption` | `values-v23/styles.xml` | 12sp regular, `text_meta` color |
| `Widget.YYCD.Button.Primary` | `values-v23/styles.xml` | `button_primary_background`, white text, 16sp, min 48dp height |
| `Widget.YYCD.CardView` | `values-v23/styles.xml` | White bg, `radius_base` corners, 2dp elevation |
| `Widget.YYCD.CardView.Featured` | `values-v23/styles.xml` | Extends CardView — adds `primary` stroke 2dp |

> ⚠️ **Style file location gotcha:** All custom YYCD styles live in `values-v23/styles.xml`, not `values/styles.xml`. There is no `values/styles.xml`.

---

## 6. Key Functions / Helpers

### Android — `MainActivity.kt`
| Function | What it does |
|---|---|
| `fetchSplashData()` | Coroutine — calls `/splash`, populates logo, splash image, title text |
| `fetchLocations()` | Coroutine — calls `/locations`, populates location `Spinner` |
| `getSplashDataFromWordPress()` | Suspend — raw HTTP GET to `/splash`, returns `SplashData` |
| `getLocationsFromWordPress()` | Suspend — raw HTTP GET to `/locations`, returns `List<Location>` |
| `parseSplashJson(jsonString)` | Parses splash JSON — reads `title_url`, `splash_url`, `splash_text` |
| `parseLocationsJson(jsonString)` | Parses locations JSON — unwraps `{ locations: [...] }` wrapper |
| `setupLocationSpinner()` | Wires Spinner to navigate to `ArticleActivity` with `location_id` extra |

### Android — `ArticleActivity.kt`
| Function | What it does |
|---|---|
| `fetchLocationDetails()` | Coroutine — calls `/locations`, finds matching ID, sets action bar title |
| `fetchPosts(page)` | Coroutine — calls `/locations/{id}/posts`, handles pagination and adapter updates |
| `getLocationDetails(locationId)` | Suspend — fetches `/locations`, finds and returns matching `LocationDetail` |
| `getPostsForLocation(locationId, page)` | Suspend — fetches `/locations/{id}/posts?per_page=10&page=N` |
| `parsePostsJson(jsonString)` | Parses post JSON — unwraps `{ posts: { items: [...] } }` double nesting |
| `openArticleDetail(post)` | Launches `ArticleDetailActivity` with `article_content`, `article_title`, `featured_image_url` extras |
| `showContactDialog()` | Inflates `dialog_contact.xml`, wires call/text/email/directions buttons from `LocationDetail` |
| `setupViews()` | Wires RecyclerView, adapter, contact button, and infinite scroll listener |

### Android — `ArticleDetailActivity.kt`
| Function | What it does |
|---|---|
| `setupWebView()` | Enables JS, DOM storage, zoom, wide viewport |
| `onCreate()` | Receives `article_content` + `article_title` + `featured_image_url` extras, wraps in HTML template with inline CSS, loads into WebView |

### Android — `ImageLoader.kt` (singleton object)
| Function | What it does |
|---|---|
| `loadImage(imageUrl, imageView)` | Loads image from URL into ImageView. Checks in-memory cache first, falls back to network. Hides ImageView on failure. |
| `clearCache()` | Clears the in-memory bitmap cache (`mutableMapOf<String, Bitmap>`) |

---

## 7. Gotchas & Lessons Learned

> This list grows over time. Add a new entry every time something breaks unexpectedly or a non-obvious decision is made.

1. **The API is a custom plugin (`ds/v1`), not standard WordPress REST.** Do not assume standard WP REST API conventions — endpoint shapes, response structures, and field names differ entirely. Base URL: `https://dandysite.com/yycd/wp-json/ds/v1`.
2. **`/locations` returns a wrapped object, not a bare array.** Response is `{ "locations": [...] }`. Must call `jsonObject.getJSONArray("locations")` — parsing the response directly as a JSONArray will crash.
3. **`/locations/{id}/posts` is doubly nested.** Response shape is `{ "posts": { "items": [...] } }`. Must do `jsonObject.getJSONObject("posts").getJSONArray("items")`.
4. **`featured_image` can be a String URL or boolean `false`.** The API returns `false` (not `null`, not an empty string) when there is no image. The code handles this with `postObject.get("featured_image")` and type-checking the result.
5. **`latitude` and `longitude` in LocationDetail come back as Strings, not numbers.** Parse with `.toDoubleOrNull() ?: 0.0` — do not call `.getDouble()` directly.
6. **All style definitions are in `values-v23/styles.xml`, not `values/styles.xml`.** There is no `values/styles.xml`. Editing the wrong file will have no effect.
7. **`old_logo.png` is still in `drawable/` — do not use it.** Use `logo.png`. The old one exists for historical reference only.
8. **`ImageLoader` is an in-memory cache only — it does not persist across app sessions.** The cache is a `mutableMapOf<String, Bitmap>` on the singleton object. It will be cleared when the app process is killed.
9. **`WORDPRESS_URL` is hardcoded as a private `val` inside each Activity, not centralised.** If the URL changes, it must be updated in both `MainActivity.kt` and `ArticleActivity.kt`.
10. **Article content is raw HTML and must always be rendered in a WebView.** Never try to display `post.content` in a `TextView` — it will show raw HTML tags.
11. **The `location_title_spacer` TextView (75dp tall, empty) appears in all three activity layouts as a manual spacer.** It is not a real UI element — it creates space for the action bar.
12. **`usesCleartextTraffic="true"` is set in the manifest.** The app allows HTTP traffic. Be aware when testing with production URLs.
13. **`[ADD MORE AS DISCOVERED]`**

---

## 8. Level-Set Checklist

> Read this at the start of every session to re-orient quickly. Numbers are for reference only — not priority order.

1. App name: **"Yes, You Can Dance!"** — package: `com.dabbled.wordpressnewsletter` — company: Dabbled Studios.
2. This is a **consumer-facing Android app** (Kotlin + XML layouts). iOS is a **separate repo** in Swift/UIKit.
3. There is **no KMP shared module** in the Android codebase — all logic lives directly in Activities.
4. The backend is a **custom WordPress REST API plugin** (`ds/v1`), not the standard WP REST API (`wp/v2`).
5. API base URL: `https://dandysite.com/yycd/wp-json/ds/v1` — do not use `yesyoucandance.org` directly for API calls.
6. **No authentication** — all API calls are public.
7. **No local persistence** — only an in-memory bitmap cache in `ImageLoader`.
8. **No dependency injection, no Retrofit, no Ktor, no Gson** — raw `HttpURLConnection` + `org.json` only.
9. All networking uses **Kotlin coroutines** on `Dispatchers.IO`, with results posted back on `Dispatchers.Main`.
10. **Three screens:** `MainActivity` (splash + location picker) → `ArticleActivity` (post list) → `ArticleDetailActivity` (WebView).
11. Data classes are defined **inline in the Activity files** that first use them — not in separate model files.
12. **`/locations` response is wrapped:** `{ "locations": [...] }` — never parse as bare JSONArray.
13. **`/posts` response is doubly nested:** `{ "posts": { "items": [...] } }` — always go two levels deep.
14. **`featured_image` can be boolean `false` or a String URL** — always type-check, never call `.getString()` directly.
15. **`latitude`/`longitude` are Strings in the API** — parse with `.toDoubleOrNull()`.
16. **Article content is raw HTML** — always render in WebView, never in TextView.
17. All custom styles live in **`values-v23/styles.xml`** — there is no `values/styles.xml`.
18. Style prefix is **`YYCD`** — e.g. `TextAppearance.YYCD.Body`, `Widget.YYCD.CardView`.
19. Color source of truth: `res/values/colors.xml`. Spacing/sizes: `res/values/dimens.xml`. Strings: `res/values/strings.xml`.
20. **`old_logo.png` exists in drawable but must not be used** — use `logo.png`.
21. **`WORDPRESS_URL` is hardcoded in both `MainActivity` and `ArticleActivity`** — update both if the URL changes.
22. **`location_title_spacer`** (75dp empty TextView) appears in all layouts as a manual spacer for action bar offset — do not remove it.
23. The app has `usesCleartextTraffic="true"` — HTTP (non-HTTPS) traffic is allowed.
24. `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`.
25. `[ADD: Most recent major change or refactor — what changed and why]`.

---

*Last updated: 2026-04-07 — update this line whenever the document changes.*