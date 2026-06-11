# Motadata Android RUM SDK — Productization & Customer Onboarding

This document captures everything needed to ship the Motadata Android RUM SDK and onboard customers, with the **minimum number of steps and the lowest possible human error**.

---

## 1. What customers will install

A rebranded fork of `dd-sdk-android` 3.10.0, published as:

```
com.motadata:motadata-rum-android:1.0.0
```

It's API-compatible with the Datadog SDK they were already using — customers get a clean Motadata namespace instead.

Optional add-on (only needed if the customer wants resource events — see Section 4):

```
com.motadata:motadata-rum-android-okhttp:1.0.0
```

Both artifacts come from the same forked repo (`dd-sdk-android`), just different Gradle modules.

---

## 2. The three-layer alignment rule

Events flow only when all three layers agree on the scheme (HTTP vs HTTPS).

| Layer | HTTPS deployment | HTTP deployment |
|---|---|---|
| **Motadata server config** | `rum.listener.protocol=https` + cert PEM files | `rum.listener.protocol=http` |
| **Android SDK snippet** | `useCustomEndpoint("https://…")` | `useCustomEndpoint("http://…")` + `.allowClearTextHttp()` |
| **Customer's AndroidManifest** | nothing extra | `usesCleartextTraffic="true"` |

If any one row is misaligned, no events arrive. Silently. Logcat will show TLS/cleartext errors.

The Motadata UI must read the server config and generate the matching snippet automatically. Customer makes zero scheme decisions.

---

## 3. Customer-facing onboarding — basic (4 steps)

This captures **views, actions, errors, sessions, long tasks**. Resource events (network calls) are a separate optional step — see Section 4.

### Step 1 — Add the dependency

`build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.motadata:motadata-rum-android:1.0.0")
}
```

Or `build.gradle` (Groovy):
```groovy
dependencies {
    implementation 'com.motadata:motadata-rum-android:1.0.0'
}
```

### Step 2 — Set up your Application class

If you don't already have a custom `Application` class:

**Create `MyApplication.kt`:**
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // ← Motadata init goes here (Step 3)
    }
}
```

**Register it in `AndroidManifest.xml`:**
```xml
<application
    android:name=".MyApplication"
    ...>
```

If you already have a custom Application class, just add the Motadata init to its existing `onCreate()`.

### Step 3 — Paste the snippet from Motadata UI

The UI generates the exact code with your token, application ID, environment, and endpoint URL already filled in. Two variants depending on your Motadata deployment scheme.

#### HTTPS deployment — Kotlin

```kotlin
import android.app.Application
import com.motadata.android.Motadata
import com.motadata.android.core.configuration.Configuration
import com.motadata.android.privacy.TrackingConsent
import com.motadata.android.rum.Rum
import com.motadata.android.rum.RumConfiguration
import com.motadata.android.rum.tracking.ActivityViewTrackingStrategy

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val configuration = Configuration.Builder(
            clientToken = "{{client.token}}",
            env = "{{env}}",
            variant = "release"
        ).build()

        Motadata.initialize(this, configuration, TrackingConsent.GRANTED)

        val rumConfiguration = RumConfiguration.Builder("{{application.id}}")
            .useCustomEndpoint("{{endpoint.url}}")
            .trackUserInteractions()
            .trackLongTasks(100L)
            .useViewTrackingStrategy(ActivityViewTrackingStrategy(false))
            .build()

        Rum.enable(rumConfiguration)
    }
}
```

#### HTTPS deployment — Java

```java
import android.app.Application;
import com.motadata.android.Motadata;
import com.motadata.android.core.configuration.Configuration;
import com.motadata.android.privacy.TrackingConsent;
import com.motadata.android.rum.Rum;
import com.motadata.android.rum.RumConfiguration;
import com.motadata.android.rum.tracking.ActivityViewTrackingStrategy;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Configuration configuration = new Configuration.Builder(
                "{{client.token}}",
                "{{env}}",
                "release"
        ).build();

        Motadata.initialize(this, configuration, TrackingConsent.GRANTED);

        RumConfiguration rumConfiguration = new RumConfiguration.Builder("{{application.id}}")
                .useCustomEndpoint("{{endpoint.url}}")
                .trackUserInteractions()
                .trackLongTasks(100L)
                .useViewTrackingStrategy(new ActivityViewTrackingStrategy(false))
                .build();

        Rum.enable(rumConfiguration);
    }
}
```

#### HTTP deployment — Kotlin

Same as above, with one extra builder call:

```kotlin
val configuration = Configuration.Builder(
    clientToken = "{{client.token}}",
    env = "{{env}}",
    variant = "release"
)
    .allowClearTextHttp()   // ← required for http:// endpoints
    .build()
```

#### HTTP deployment — Java

```java
Configuration configuration = new Configuration.Builder(
        "{{client.token}}",
        "{{env}}",
        "release"
)
        .allowClearTextHttp()   // ← required for http:// endpoints
        .build();
```

### Step 4 — (HTTP deployments only) Allow cleartext in AndroidManifest

Skip this entire step if your Motadata endpoint uses HTTPS.

Choose **one** option:

#### Option A — Allow all cleartext (simplest)

```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

#### Option B — Allow only the Motadata host (recommended)

Create `app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">{{endpoint.host}}</domain>
    </domain-config>
</network-security-config>
```

Reference it in `AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

Where `{{endpoint.host}}` is just the host portion of your endpoint (e.g. `10.20.40.253` or `rum.acme.internal`), no scheme, no port.

#### Option C — Self-signed HTTPS certificate

Trust your CA instead of allowing cleartext:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config>
        <domain includeSubdomains="true">{{endpoint.host}}</domain>
        <trust-anchors>
            <certificates src="@raw/motadata_ca"/>
            <certificates src="system"/>
        </trust-anchors>
    </domain-config>
</network-security-config>
```

Drop your CA cert at `app/src/main/res/raw/motadata_ca.crt`.

### Verification

1. Build and launch the app.
2. Open Logcat, filter by tag `Motadata`.
3. Within a few seconds you should see `Motadata SDK initialized`.
4. Within ~30 seconds your first session appears in the Motadata RUM dashboard.

---

## 4. Optional — Resource (network call) event tracking

Resource events record every HTTP request your app makes: URL, method, response time, status code, payload size, errors. They appear in the Motadata RUM dashboard, correlated with the view they happened in.

**Only works with OkHttp or Retrofit.** Apps using `HttpURLConnection`, Volley, Ktor, or custom networking will not get resource events. All other event types still work.

### They need 3 things

#### 1. Add the OkHttp integration dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.motadata:motadata-rum-android:1.0.0")
    implementation("com.motadata:motadata-rum-android-okhttp:1.0.0")   // ← add this
}
```

They probably already have OkHttp:
```kotlin
implementation("com.squareup.okhttp3:okhttp:<version>")
```

#### 2. Add `MotadataInterceptor` to the existing shared `OkHttpClient`

**Java**

Before:
```java
OkHttpClient client = new OkHttpClient.Builder()
        .build();
```

After:
```java
OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(new MotadataInterceptor.Builder(Collections.emptyMap()).build())
        .build();
```

**Kotlin**

Before:
```kotlin
val client = OkHttpClient.Builder()
    .build()
```

After:
```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(MotadataInterceptor.Builder(emptyMap()).build())
    .build()
```

#### 3. Make sure app traffic uses that client

For Retrofit, for example:

**Java**
```java
Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .build();
```

**Kotlin**
```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(client)
    .build()
```

That's it. Every network request through `client` becomes a resource event in Motadata.

### Important rules

1. **Use this OkHttpClient everywhere** you want tracked. Multiple OkHttpClients in the same app = only the instrumented one captures resources.
2. **Resource events require an active view** — the base setup with `ActivityViewTrackingStrategy` already handles this.
3. **Empty map / list is fine** — captures every request regardless of host.

### Verification

Trigger a network request in your app. Logcat shows:
```
I/Motadata: RUM Resource started: <id>
I/Motadata: RUM Resource stopped: <id>
```
The request appears under the matching view's "Resources" panel in the dashboard.

### Common gotchas

| Symptom | Cause | Fix |
|---|---|---|
| No resource events | Dependency missing | Add `motadata-rum-android-okhttp` |
| No resource events | Interceptor not on OkHttpClient | Add `.addInterceptor(MotadataInterceptor.Builder(emptyMap()).build())` |
| Some requests captured, some not | Multiple OkHttpClient instances | Attach interceptor to all of them, or share one client |
| Retrofit calls not captured | Retrofit using its default OkHttpClient | Pass the instrumented client via `Retrofit.Builder.client(...)` |
| Request fires before view starts | Network call in `Application.onCreate()` | Move it to first Activity's `onStart()` or later |

### What the Motadata UI must do

In the Android tab, add an opt-in checkbox:

```
☐ I want to track network requests (resource events)
   Requires OkHttp or Retrofit. Adds one dependency and
   one interceptor line to your existing HTTP client.
```

If checked → show the dependency line + the OkHttpClient interceptor snippet. If unchecked → omit entirely.

---

## 5. Generated snippet placeholders

| Placeholder | Source in `RUMServiceProfileConfigStore` |
|---|---|
| `{{client.token}}` | `rum.profile.client.token` |
| `{{application.id}}` | `id` (as string) |
| `{{env}}` | profile environment, default `"production"` |
| `{{endpoint.url}}` | derived from `MotadataConfigUtil.getRUMListenerProtocol()` + host + `/api/v2/rum` |
| `{{endpoint.host}}` | host portion of endpoint URL only |

---

## 6. Backend changes needed for Android support

### 6.1 Add Android snippet templates to `RUMConstant.java`

Multiple constants alongside the existing `RUM_JS_SNIPPET`:

```java
public static final String RUM_ANDROID_SNIPPET_HTTPS = "...";
public static final String RUM_ANDROID_SNIPPET_HTTP  = "...";
public static final String RUM_ANDROID_SNIPPET_RESOURCE_ADDON = "...";  // OkHttp interceptor block, appended if resource tracking enabled
```

### 6.2 Add `getAndroidSnippet()` method in `RUMServiceProfile.java`

```java
private String getAndroidSnippet(JsonObject application) {
    String protocol = MotadataConfigUtil.getRUMListenerProtocol();
    boolean isHttp = "http".equalsIgnoreCase(protocol);
    String endpointUrl = protocol + "://" + resolveHost(application) + "/api/v2/rum";
    String endpointHost = resolveHost(application);
    boolean resourceTracking = application.getBoolean(RUM_PROFILE_RESOURCE_TRACKING, false);

    String template = isHttp
            ? RUMConstant.RUM_ANDROID_SNIPPET_HTTP
            : RUMConstant.RUM_ANDROID_SNIPPET_HTTPS;

    String result = template
            .replace("$$$client.token$$$",   application.getString(RUM_PROFILE_CLIENT_TOKEN))
            .replace("$$$application.id$$$", String.valueOf(application.getLong(ID)))
            .replace("$$$env$$$",            application.getString(RUM_PROFILE_ENVIRONMENT, "production"))
            .replace("$$$endpoint.url$$$",   endpointUrl)
            .replace("$$$endpoint.host$$$",  endpointHost);

    if (resourceTracking) {
        result += "\n\n" + RUMConstant.RUM_ANDROID_SNIPPET_RESOURCE_ADDON;
    }
    return result;
}
```

Mirror the existing JS snippet pattern.

### 6.3 Add new profile field

In `RUMServiceProfile.java` constants:
```java
public static final String RUM_PROFILE_RESOURCE_TRACKING = "rum.profile.resource.tracking";  // Boolean
```

Populated from the profile-creation UI form (the opt-in checkbox).

### 6.4 Re-enable validation before production

`RUMEventListener.java` currently has two DEBUG bypasses:

| Line | Current | Production |
|---|---|---|
| 229 | `if (true)  // rate-limit bypassed` | restore real rate-limit check |
| 252 | `if (false) // md-api-key validation bypassed` | restore `validate(token)` call |

### 6.5 Close the appId/token binding gap

Today the server validates token and reads `application.id` from the event body independently. Add a check in `RUMEventProcessor`:

> The `application.id` in the event body must belong to the profile associated with the request's `md-api-key`.

Reject events that fail this check with `403 Forbidden`.

---

## 7. SDK fork changes (one-time)

Single repo (`dd-sdk-android`), multiple Gradle modules published as separate Maven artifacts.

| # | Change | Module(s) | Effort |
|---|---|---|---|
| 1 | Rename packages `com.datadog.android.*` → `com.motadata.android.*` | all | IDE refactor across the repo |
| 2 | Rename public classes: `Datadog`, `DatadogSite`, `DatadogInterceptor`, `DatadogEventListener`, etc. | core, rum, okhttp | ~20 public types |
| 3 | Rename Maven coords `com.datadoghq:dd-sdk-android-*` → `com.motadata:motadata-rum-android-*` | all | `MavenConfig.kt`, every module's `build.gradle.kts` |
| 4 | Set `telemetrySampleRate = 0f` default in `RumFeature.kt` | rum | 1 line |
| 5 | Remove `internal` modifier on `Configuration.Builder.allowClearTextHttp()` | core | 1 line |
| 6 | Update lint check ID `DatadogInternalApiUsage` → `MotadataInternalApiUsage` | tools/lint | small |
| 7 | Update consumer ProGuard rules to reference renamed packages | all | `consumer-rules.pro` files |
| 8 | Regenerate API surface files | all | `./gradlew apiDumpAll` |
| 9 | Keep Datadog Apache 2.0 copyright headers in source files; add Motadata copyright below | all | Legal — required by the license |

### 7.1 Maven artifacts to publish

After rebrand, your repo publishes:

| Artifact | Replaces | Required for customer |
|---|---|---|
| `com.motadata:motadata-rum-android-core:1.0.0` | `com.datadoghq:dd-sdk-android-core` | Yes (transitive) |
| `com.motadata:motadata-rum-android:1.0.0` | `com.datadoghq:dd-sdk-android-rum` | Yes |
| `com.motadata:motadata-rum-android-okhttp:1.0.0` | `com.datadoghq:dd-sdk-android-okhttp` | Optional — only for resource events |

Customers add `-core` transitively when they add `-rum`, so they declare:
- `motadata-rum-android` always
- `motadata-rum-android-okhttp` only if they want resource tracking

### 7.2 Publishing target

Pick one:

- **Maven Central** — required for public SaaS distribution. Needs GPG signing + group ID registration (1-2 weeks setup).
- **Internal Artifactory / Nexus** — for on-prem enterprise; customers add a custom repo URL to their `settings.gradle.kts`.
- **GitHub Packages** — works if Motadata is on GitHub.

Most products start internal and move to Maven Central when going public.

---

## 8. Things to remember (not customer-facing)

| # | Item |
|---|---|
| 1 | Token format is `pub` + 32 hex chars — already produced by `generateClientToken()`, no change needed |
| 2 | Application ID is a numeric `Long` — Android SDK accepts any string, works as-is |
| 3 | Server already decompresses gzip (`RUMEventListener.decodeBody()`) — Android compression is a non-issue |
| 4 | Server already expects `md-api-key` query param — matches what the rebranded SDK will send |
| 5 | Internal telemetry (separate from RUM) defaults to Datadog intake — fixed by setting `telemetrySampleRate = 0f` in the fork |
| 6 | `DatadogSite.US5` in the current snippet acts as a fallback for any feature without `useCustomEndpoint` — for safety, harden the default in the fork to fail loudly if forgotten |
| 7 | Trailing slash mismatch (`/api/v2/rum/` vs `/api/v2/rum`) — verify the route accepts both in Vert.x |
| 8 | Don't rebase the fork on upstream Datadog after rename — freeze at 3.10.0 and apply security patches manually |
| 9 | Build a sample app that does a single event submission on launch — give to new customers for smoke testing during onboarding |
| 10 | Resource tracking requires OkHttp or Retrofit — explicitly document the unsupported libraries (HttpURLConnection, Volley, Ktor) rather than letting customers debug silently |
| 11 | The `MotadataInterceptor` must be on the SAME OkHttpClient used by the app; multiple clients = partial capture |
| 12 | Pass `emptyMap()` to `MotadataInterceptor.Builder(...)` — every request through that OkHttpClient is captured regardless of host |

---

## 9. Common failure modes (Troubleshooting)

### Base SDK / event delivery

| Symptom | Cause | Fix |
|---|---|---|
| No events after 5 min | Cleartext blocked | Verify Section 3 Step 4 matches endpoint scheme |
| Logcat: `CleartextNotPermittedException` | HTTP endpoint, no manifest opt-in | Add `usesCleartextTraffic` or `network_security_config.xml` |
| Logcat: `SSLHandshakeException` | HTTPS endpoint with invalid/self-signed cert | Use Section 3 Step 4 Option C |
| Logcat: `SSLException: Connection closed` | URL says `https://` but server is HTTP | Match schemes on both sides |
| Logcat: `Connection refused` | Wrong port or server down | Verify with `curl <endpoint>` |
| Events 401 | Token typo or profile archived | Regenerate in Motadata UI |
| App ID `null` in dashboard | Missing argument to `RumConfiguration.Builder` | First constructor arg is required |
| Delayed by 30+ seconds | Normal — batched | Wait, or force flush in dev mode |

### Resource events specifically

| Symptom | Cause | Fix |
|---|---|---|
| No resource events at all | OkHttp integration dependency missing | Add `motadata-rum-android-okhttp` to gradle |
| No resource events at all | Interceptor not attached | Verify `.addInterceptor(MotadataInterceptor.Builder(emptyMap()).build())` is on the OkHttpClient builder |
| Resource events from Retrofit missing | Retrofit using its default OkHttpClient | Pass an instrumented client via `Retrofit.Builder.client(...)` |
| Some resource events missing | App uses multiple OkHttpClient instances | Attach interceptor to all of them, or share one |
| Resource events from background work missing | No active RUM view | Start a view manually via `GlobalRumMonitor.get().startView(...)` |
| Customer uses Volley / Ktor / HttpURLConnection | Not supported | Inform customer; suggest migration to OkHttp if resource events are critical |

---

## 10. End-to-end customer journey

1. Customer installs Motadata (SaaS signup or on-prem appliance).
2. Customer creates a **RUM Service** in Motadata UI: name, env, version, tags.
3. **Optional during profile creation:** customer checks "track network resource events".
4. Backend generates `applicationId` (Long) + `clientToken` (`pub…`) and stores the profile.
5. UI shows three tabs: **Web** | **Android** | **iOS** — each with copy-pasteable snippet. Android tab includes the OkHttp section if resource tracking is enabled.
6. Customer picks Android, copies the snippet (basic + optional resource-tracking block + manifest config if HTTP deployment).
7. Customer pastes into their Application class, adds the interceptor to their OkHttpClient, builds, ships.
8. App runs → SDK batches events → POSTs to `{{endpoint.url}}` with `?md-api-key={{client.token}}`.
9. Server decompresses gzip, validates token, routes by `application.id`, processes.
10. Customer sees their data in the Motadata RUM dashboard within ~30 seconds — including resource events under each view if enabled.

---

## 11. Summary

**Customer sees:** 4 base steps (views, actions, errors, sessions, long tasks) + optional resource-tracking add-on (~3 extra lines if they use OkHttp/Retrofit).

**You do:**
- SDK rebrand of the single `dd-sdk-android` repo (one-time fork work)
- Telemetry off (1 line)
- `allowClearTextHttp()` made public (1 line)
- License headers preserved (legal)
- Publish 3 Maven artifacts: `-core`, `-rum`, `-okhttp`
- Backend: add Android snippet template + `getAndroidSnippet()` method + optional resource add-on
- Backend: add profile field for resource tracking opt-in
- Backend: re-enable validation, close appId/token gap

**Errors prevented by design:** the Motadata UI reads the server's protocol config and emits the right snippet (HTTP vs HTTPS) — customer makes no scheme decision. Resource tracking is opt-in per profile and only shown when the customer needs it.

**Customer support shortcut:** "Are you using OkHttp or Retrofit?" — that single question determines whether resource events are possible. If no, they get all other event types but not resources. Simple, honest, no rabbit holes.
