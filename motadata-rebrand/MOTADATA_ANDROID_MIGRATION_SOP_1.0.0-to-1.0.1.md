# Motadata Android RUM SDK — Migration Guide: `1.0.0` → `1.0.1`

**(Branch 1 "rebrand-only" → Branch 2 "rebrand + functional additions")**

This is a **small migration**. `1.0.1` is a **superset** of `1.0.0` — it contains everything in
`1.0.0` plus six functional additions. **All six are internal SDK behavior and require ZERO app code**
(they just start happening). The only app-facing improvement is that `allowClearTextHttp()` is now a
**public** API, so you can drop the `_InternalProxy` workaround.

> **Roles (same split as the Branch-1 guide):**
> **Part A** — the AI agent makes the dependency + code change and builds.
> **Part B** — the human runs the app, hits the buttons, captures the RUM events, and verifies the new fields.

---

## 0. What actually changes (and what doesn't)

### Changes that need NO app code (automatic in `1.0.1`)
| Addition | What you'll see on the wire / Logcat | App change |
|---|---|---|
| `md-api-key` **query param** (auth) | request URL gains `?…&md-api-key=<clientToken>&…` | **none** — taken from the `clientToken` you already pass |
| `view.is_view_completed` | view events carry `view.is_view_completed = "no"`→`"yes"` | **none** |
| `session.created` | every event's `session` object gains `created` (epoch-ms) | **none** |
| `context._timing` | every event's `context` gains `_timing { navigationStart, relativeTime }` | **none** |
| `"Motadata SDK initialized"` log | Logcat (tag `Motadata`) prints it once at init | **none** |

### The ONE app-facing change (optional but recommended)
`Configuration.Builder.allowClearTextHttp()` is now **public**. In `1.0.0` you had to reach it through
the internal `_InternalProxy` shim (with a lint suppression). In `1.0.1` you call it **directly**.

> The old `_InternalProxy.allowClearTextHttp(builder)` call **still works** in `1.0.1` (kept for
> backward compatibility), so this step is technically optional — but the public method is the clean,
> supported path, so migrate it.

### What does NOT change at all
- **Public API is identical:** `Motadata.initialize(...)`, `Rum.enable(...)`,
  `RumConfiguration.Builder(appId).useCustomEndpoint(...).trackUserInteractions().trackLongTasks(...)`,
  `MotadataInterceptor`, `MotadataEventListener`, `MotadataSite`, `TrackingConsent`. Your existing
  init/instrumentation code **compiles unchanged**.
- **Manifest:** still need `android:usesCleartextTraffic="true"` (or a scoped `network-security-config`)
  for an `http://` endpoint, plus the `INTERNET` permission. No change.
- **No new permissions, no ProGuard/R8 changes, no new modules.**

---

## Part A — Agent: change the dependency + code, then build

### A-1. Bump the dependency version `1.0.0` → `1.0.1`

In the app's `build.gradle` / `build.gradle.kts` (and any `libs.versions.toml`), change every Motadata
coordinate from `1.0.0` to `1.0.1`. Example (plain RUM = 3 artifacts; + resource tracking = 7):

```kotlin
// before
implementation("com.motadata:motadata-rum-android:1.0.0")
implementation("com.motadata:motadata-rum-android-okhttp:1.0.0") // if you track resources via OkHttp

// after
implementation("com.motadata:motadata-rum-android:1.0.1")
implementation("com.motadata:motadata-rum-android-okhttp:1.0.1")
```

> You only list the artifacts you already declared — Gradle resolves the rest of the closure
> (`-core`, `-internal`, `-trace*`) transitively from the POM, and they were all published at `1.0.1`.
> The GitHub Packages repo block + credentials in `settings.gradle`/`gradle.properties` stay exactly as
> they were for `1.0.0`.

### A-2. (Recommended) Replace the `_InternalProxy` cleartext-HTTP call with the public API

Find where the SDK config is built (in the sample app: `SampleApplication.java`).

**Before (Branch 1 / `1.0.0`):**
```java
import com.motadata.android._InternalProxy;   // <-- remove this import
...
@SuppressLint("DatadogInternalApiUsage")        // <-- remove if it was only for _InternalProxy
@Override
public void onCreate() {
    super.onCreate();
    Configuration.Builder configurationBuilder = new Configuration.Builder(clientToken, env, variant);
    // ...other config...
    _InternalProxy.Companion.allowClearTextHttp(configurationBuilder);   // <-- replace this line
    Configuration configuration = configurationBuilder.build();
    Motadata.initialize(this, configuration, TrackingConsent.GRANTED);
    // ...Rum.enable(...) etc...
}
```

**After (Branch 2 / `1.0.1`):**
```java
// (no _InternalProxy import, no @SuppressLint needed)
@Override
public void onCreate() {
    super.onCreate();
    Configuration.Builder configurationBuilder = new Configuration.Builder(clientToken, env, variant);
    // ...other config...
    configurationBuilder.allowClearTextHttp();   // <-- public API, called directly
    Configuration configuration = configurationBuilder.build();
    Motadata.initialize(this, configuration, TrackingConsent.GRANTED);
    // ...Rum.enable(...) etc...
}
```

**Kotlin equivalent:**
```kotlin
val configuration = Configuration.Builder(clientToken, env, variant)
    // ...other config...
    .allowClearTextHttp()        // public in 1.0.1
    .build()
Motadata.initialize(this, configuration, TrackingConsent.GRANTED)
```

> If `@SuppressLint("DatadogInternalApiUsage")` was added **only** to silence the `_InternalProxy`
> usage, remove it. If it's there for some other internal-API use, leave it.

**Everything else in `onCreate()` (the builder chain, `Motadata.initialize`, `RumConfiguration.Builder`
`.useCustomEndpoint(...)`, `Rum.enable(...)`, the OkHttp `MotadataInterceptor`) stays exactly as it is.**

### A-3. Refresh dependencies and build

Because the version changed, Gradle fetches the new artifacts. Force a clean re-resolve to be safe:

```
gradlew.bat assembleDebug --refresh-dependencies
```
*(or delete `…\.gradle\caches\modules-2\files-2.1\com.motadata\` then build).*

Confirm the build succeeds. There should be **no compile changes required** beyond A-2 — the public API
is unchanged, so nothing else breaks.

---

## Part B — Human: run, capture, verify

### B-1. Run + check the init log (quick smoke test)
Install and launch the app. In Logcat, filtered to the Motadata tag:
```
adb logcat -s Motadata
```
You should now see **`Motadata SDK initialized`** at startup — this line is **new in `1.0.1`** and is the
fastest confirmation you're actually running the new version.

### B-2. Exercise the app + capture RUM batches
Hit the same buttons / flows you used for the `1.0.0` capture, and capture the outgoing RUM batches the
same way (so the `1.0.1` capture is directly comparable to the `1.0.0` one).

### B-3. Verify the four Branch-2 changes on the wire
Compared to the `1.0.0` captures, the `1.0.1` captures must now show:

1. **Auth query param** — the request URL is now
   `…/api/v2/rum?mdsource=android&md-api-key=<clientToken>&…` (the `md-api-key` is **new**).
2. **`view.is_view_completed`** — inside each **view** event's `view` object: `"no"` on in-progress
   updates, flipping to `"yes"` on the final/authoritative update (e.g. when you navigate to another view).
3. **`session.created`** — inside **every** event's `session` object: an epoch-millisecond timestamp of
   when the session started.
4. **`context._timing`** — inside **every** event's `context` object:
   `{ "navigationStart": <session-start ms>, "relativeTime": <nanoseconds since session start> }`.

> If your Motadata endpoint **validates** `md-api-key`, the intake will now **accept** the events
> (HTTP 200/202). With `1.0.0` (no query param) a validating endpoint would have rejected them (401).
> So `1.0.1` is the version a real deployment needs.

### B-4. Tell the agent the capture location
Hand the new capture batch location back so the changes can be diffed against the `1.0.0` capture and the
migration confirmed.

---

## Quick checklist

- [ ] A-1: dependency `…:1.0.0` → `…:1.0.1` (all declared Motadata artifacts)
- [ ] A-2: `_InternalProxy.allowClearTextHttp(builder)` → `builder.allowClearTextHttp()`; remove the
      `_InternalProxy` import + the (now-unneeded) `@SuppressLint("DatadogInternalApiUsage")`
- [ ] A-3: `assembleDebug --refresh-dependencies`, build green
- [ ] B-1: Logcat shows `Motadata SDK initialized`
- [ ] B-2: capture RUM batches
- [ ] B-3: confirm `md-api-key` query, `is_view_completed`, `session.created`, `context._timing`
- [ ] B-4: share capture location

That's the whole migration. Nothing else in the app needs to change.
