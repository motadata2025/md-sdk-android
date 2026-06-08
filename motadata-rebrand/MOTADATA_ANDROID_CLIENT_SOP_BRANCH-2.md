# Motadata Android RUM — Client Integration SOP (Branch 2)

How to add the **Motadata Android RUM SDK** to an Android app and send RUM data to a Motadata
custom endpoint (HTTP or HTTPS). Three steps: **S‑1 Dependencies → S‑2 Init snippet → S‑3 OkHttp
(resource tracking, optional)**.

> **Branch‑2 (`1.0.1`)** = the fully‑rebranded SDK (`com.motadata.android.*`, `Motadata*`, `MD-*`,
> `mdsource`/`mdtags`, `"_md"`) **plus** Motadata‑specific functional additions. The additions are all
> **automatic — no extra app code**:
> - **`md-api-key` query param** on every request (the param your Motadata intake authenticates on)
> - **`view.is_view_completed`** (`"yes"`/`"no"`) on view events
> - **`session.created`** (epoch‑ms) + **`context._timing { navigationStart (ms), relativeTime (ns) }`** on every event
> - a **`Motadata SDK initialized`** Logcat line at init
>
> The only *app‑facing* difference vs Branch‑1: `allowClearTextHttp()` is now a **public** builder
> method (no `_InternalProxy` / `@SuppressLint`). **Use `1.0.1` for any real deployment** — `1.0.0`
> does not send `md-api-key`, so a validating endpoint will reject it (401).

## Prerequisites
- An Android app (`minSdk` ≥ 21; SDK targets Java 17 / AGP 8).
- From the client's Motadata org: **RUM application id** and **client token**.
- A GitHub username + a **PAT with `read:packages`** (the packages are public, but GitHub
  Packages still requires *a* token; any GitHub account's works).
- Artifacts (current version **`1.0.1`**):
  - `com.motadata:motadata-rum-android:1.0.1`
  - `com.motadata:motadata-rum-android-okhttp:1.0.1` *(only if instrumenting OkHttp — see S‑3)*

---

## S‑1 — Dependencies

### 1a. Credentials (global, never commit)
`~/.gradle/gradle.properties` (Windows: `C:\Users\<user>\.gradle\gradle.properties`):
```properties
gpr.user=<GITHUB_USERNAME>
gpr.key=<GITHUB_READ_PACKAGES_PAT>
```

### 1b. Repository — `settings.gradle.kts`
Keep `google()` and `mavenCentral()`; add the Motadata GitHub Packages repo:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "MotadataGitHubPackages"
            url = uri("https://maven.pkg.github.com/motadata2025/md-sdk-android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

### 1c. Dependencies — app module `build.gradle(.kts)`
```kotlin
dependencies {
    implementation("com.motadata:motadata-rum-android:1.0.1")
    implementation("com.motadata:motadata-rum-android-okhttp:1.0.1") // only if using OkHttp (S-3)
}
```
<details><summary>Groovy DSL</summary>

```groovy
dependencies {
    implementation "com.motadata:motadata-rum-android:1.0.1"
    implementation "com.motadata:motadata-rum-android-okhttp:1.0.1" // only if using OkHttp (S-3)
}
```
</details>

> Remove any old `com.datadoghq:dd-sdk-android-*` dependencies — keeping both causes conflicts.

### 1d. Manifest — `AndroidManifest.xml`
Add the INTERNET permission. **Only if your Motadata endpoint is plain HTTP**, allow cleartext
(globally as below, or scoped via a `network-security-config`). For HTTPS endpoints, omit
`usesCleartextTraffic`.
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:usesCleartextTraffic="true">   <!-- only for HTTP endpoints -->
        <!-- android:name is handled in S-2 depending on your app (see below) -->
    </application>
</manifest>
```

---

## S‑2 — Initialize the SDK (one snippet, in `Application.onCreate()`)

Initialize **once**, as early as possible. The single block below does everything: builds the
`Configuration`, calls `Motadata.initialize(...)`, builds the `RumConfiguration` (with your custom
endpoint), and enables RUM.

### Where does this code go? Two cases:

**Case A — your app does NOT have an `Application` subclass yet.**
Create one and register it in the manifest's `<application android:name="…">`:
```xml
<application
    android:name=".MotadataApplication"
    android:usesCleartextTraffic="true">  <!-- only for HTTP -->
```

**Case B — your app ALREADY has an `Application` subclass** (manifest already has
`android:name=".YourApp"`).
**Do not create a new class and do not change `android:name`.** Just paste the body of
`onCreate()` below into your existing `YourApp.onCreate()`, right after `super.onCreate()`.

### Kotlin
```kotlin
import android.app.Application
import com.motadata.android.Motadata
import com.motadata.android.MotadataSite
import com.motadata.android.core.configuration.Configuration
import com.motadata.android.privacy.TrackingConsent
import com.motadata.android.rum.Rum
import com.motadata.android.rum.RumConfiguration
import com.motadata.android.rum.tracking.ActivityViewTrackingStrategy

// Case A: this whole class is new. Case B: copy only the onCreate() body into your existing Application.
class MotadataApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1) Core configuration
        val configuration = Configuration.Builder(
            clientToken = "<MOTADATA_CLIENT_TOKEN>",
            env = "<ENVIRONMENT_NAME>",       // e.g. "prod", "staging"
            variant = "<APP_VARIANT_NAME>"     // e.g. "release", "debug"
        )
            .useSite(MotadataSite.US5)          // ignored when useCustomEndpoint is set, but required
            .allowClearTextHttp()              // HTTP ENDPOINT ONLY — delete this line for https://
            .build()

        // 2) Initialize the SDK
        Motadata.initialize(this, configuration, TrackingConsent.GRANTED)

        // 3) RUM configuration → your Motadata custom endpoint
        val rumConfiguration = RumConfiguration.Builder("<MOTADATA_RUM_APPLICATION_ID>")
            .useCustomEndpoint("http://<your-motadata-host>/api/v2/rum/")
            .trackUserInteractions()            // taps/clicks → action events
            .trackLongTasks(100L)               // main-thread stalls > 100ms → long_task events
            .useViewTrackingStrategy(ActivityViewTrackingStrategy(false)) // activities → view events
            .build()

        // 4) Enable RUM
        Rum.enable(rumConfiguration)
    }
}
```

### Java
```java
import android.app.Application;

import com.motadata.android.Motadata;
import com.motadata.android.MotadataSite;
import com.motadata.android.core.configuration.Configuration;
import com.motadata.android.privacy.TrackingConsent;
import com.motadata.android.rum.Rum;
import com.motadata.android.rum.RumConfiguration;
import com.motadata.android.rum.tracking.ActivityViewTrackingStrategy;

// Case A: this whole class is new. Case B: copy only the onCreate() body into your existing Application.
public class MotadataApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1) Core configuration
        Configuration configuration = new Configuration.Builder(
                "<MOTADATA_CLIENT_TOKEN>",
                "<ENVIRONMENT_NAME>",   // e.g. "prod", "staging"
                "<APP_VARIANT_NAME>"    // e.g. "release", "debug"
        )
                .useSite(MotadataSite.US5)    // ignored when useCustomEndpoint is set, but required
                .allowClearTextHttp()         // HTTP ENDPOINT ONLY — delete this line for https://
                .build();

        // 2) Initialize the SDK
        Motadata.initialize(this, configuration, TrackingConsent.GRANTED);

        // 3) RUM configuration → your Motadata custom endpoint
        RumConfiguration rumConfiguration = new RumConfiguration.Builder("<MOTADATA_RUM_APPLICATION_ID>")
                .useCustomEndpoint("http://<your-motadata-host>/api/v2/rum/")
                .trackUserInteractions()
                .trackLongTasks(100L)
                .useViewTrackingStrategy(new ActivityViewTrackingStrategy(false))
                .build();

        // 4) Enable RUM
        Rum.enable(rumConfiguration);
    }
}
```

> **Site value:** with `useCustomEndpoint(...)` set, `MotadataSite` is not used for routing but the
> builder still requires one.
>
> **Events:** this snippet already produces **view, action, long_task, error, and crash** events
> automatically — each now carrying `session.created` + `context._timing`, and view events carrying
> `view.is_view_completed`. No extra code needed.

### ⚠️ Cleartext HTTP endpoint — requires TWO things
If your Motadata endpoint is plain **`http://`** (not `https://`), you must enable cleartext in
**both** places — either one alone is not enough:
1. **OS / manifest** — `android:usesCleartextTraffic="true"` (or a scoped `network-security-config`), from S‑1d.
2. **SDK** — `.allowClearTextHttp()` in the `Configuration.Builder` chain (S‑2 above), or the SDK
   rejects the `http://` endpoint.

In Branch‑2 `1.0.1`, `allowClearTextHttp()` is a **public** builder method — a normal chained call,
no `_InternalProxy` and no `@SuppressLint` needed (that was the Branch‑1 workaround).

**For an `https://` endpoint:** delete the `.allowClearTextHttp()` line and omit
`usesCleartextTraffic` — neither is needed.

---

## S‑3 — (Extra) Network resource tracking via OkHttp

**Optional — only if the app uses OkHttp and you want `resource` events for HTTP calls.** Requires
the `motadata-rum-android-okhttp` dependency (S‑1c). Add `MotadataInterceptor` to the
`OkHttpClient` that makes your app's network calls.

### Kotlin
```kotlin
import com.motadata.android.okhttp.MotadataInterceptor
import okhttp3.OkHttpClient

val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(MotadataInterceptor.Builder(emptyMap()).build())
    .build()
```

### Java
```java
import com.motadata.android.okhttp.MotadataInterceptor;
import java.util.Collections;
import okhttp3.OkHttpClient;

OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .addInterceptor(new MotadataInterceptor.Builder(Collections.emptyMap()).build())
        .build();
```

> `emptyMap()` = RUM resource tracking only (no distributed tracing). To also propagate trace
> headers to your own backend hosts, pass a `List<String>` of hosts (or a
> `Map<String, Set<TracingHeaderType>>`) to the `Builder` instead of an empty map.

---

## Verify
Build, run, and exercise the app (open screens, tap, make network calls). Confirm:
- Logcat tag is **`Motadata`** (no `Datadog`); shows **`Motadata SDK initialized`** at startup, no
  SDK/upload errors: `adb logcat -s Motadata`
- Request hits your **custom Motadata host**; query is `mdsource=android&md-api-key=<clientToken>`
  (+ `mdtags`), **not** `dd*`
- Headers are `MD-API-KEY`, `MD-EVP-ORIGIN`, `MD-EVP-ORIGIN-VERSION` (= `1.0.1`), `MD-REQUEST-ID`,
  `MD-IDEMPOTENCY-KEY` — **no `DD-*`**
- Event bodies use `"_md"` (not `"_dd"`); `service` is `motadata-rum-android`
- **New fields present:** view events have `view.is_view_completed`; every event has
  `session.created` and `context._timing { navigationStart, relativeTime }`
- Endpoint returns **200/202** (with a valid registered token) — and events appear in the client's
  Motadata org. *(A `401` means the `md-api-key`/token isn't recognized by the intake.)*

---

## Notes
- **Replace all placeholders:** `<GITHUB_USERNAME>`, `<GITHUB_READ_PACKAGES_PAT>`,
  `<MOTADATA_CLIENT_TOKEN>`, `<MOTADATA_RUM_APPLICATION_ID>`, `<ENVIRONMENT_NAME>`,
  `<APP_VARIANT_NAME>`, `http://<your-motadata-host>/api/v2/rum/`.
- The `md-api-key` is taken automatically from your `clientToken` — nothing extra to configure for auth.
- Keep GitHub credentials in global Gradle properties / CI secrets, never in source control.
- Optional add‑ons (only if needed): `Motadata.setUserInfo(...)`, global attributes, manual views/
  actions via `GlobalRumMonitor.get()`, fragment tracking, sampling via
  `RumConfiguration.Builder.setSessionSampleRate(...)`. Add these separately so each can be verified.
- **Field units / meanings** are documented in `EVENT_TYPES.md`; the full list of Branch‑2 changes is
  in `PROGRESS.md`. Note: the synthetic `ApplicationLaunch` view (one per session) can show a small
  **negative** `relativeTime`, because its timestamp is backdated to app‑start (before `session.created`).
- Migrating an app already on `1.0.0`? See `MOTADATA_ANDROID_MIGRATION_SOP_1.0.0-to-1.0.1.md`.
- Future: if these artifacts are published to Maven Central, drop the GitHub Packages repo + PAT and
  rely on `mavenCentral()`.
```
