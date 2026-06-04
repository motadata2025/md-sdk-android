# Test the Motadata Android RUM SDK in your sample app (Windows / Android Studio)

**You are an AI agent on a Windows machine.** This repo's sample app currently uses the
**Datadog Android SDK 3.10.0** (`com.datadoghq:dd-sdk-android-*`) and sends RUM data to a
**custom Motadata endpoint over HTTP**. Your job: **swap in the rebranded Motadata fork of
that exact SDK and verify it behaves identically** — same custom endpoint, same HTTP, but now
with all `datadog`/`dd`/`DD-` naming replaced by `motadata`/`md`/`MD-`.

The fork is **behavior-identical** to Datadog 3.10.0. The ONLY differences are names
(package, classes, event keys, headers, query params, Maven coordinates). So if your app
worked before, it must work the same after — just with rebranded wire output.

---

## 0. What changed (mental model)

| Aspect | Before (Datadog 3.10.0) | After (Motadata fork 1.0.0) |
|---|---|---|
| Maven group | `com.datadoghq` | `com.motadata` |
| RUM artifact | `dd-sdk-android-rum` | `motadata-rum-android` |
| OkHttp artifact | `dd-sdk-android-okhttp` | `motadata-rum-android-okhttp` |
| Package | `com.datadog.android.*` | `com.motadata.android.*` |
| Entry object | `Datadog` | `Motadata` |
| OkHttp interceptor | `DatadogInterceptor` | `MotadataInterceptor` |
| Event listener | `DatadogEventListener` | `MotadataEventListener` |
| Site enum | `DatadogSite` | `MotadataSite` |
| Query params | `?ddsource=…&ddtags=…` | `?mdsource=…&mdtags=…` |
| HTTP headers | `DD-API-KEY`, `DD-EVP-ORIGIN`, … | `MD-API-KEY`, `MD-EVP-ORIGIN`, … |
| Event body envelope | `"_dd": { … }` | `"_md": { … }` |
| Logcat tag | `Datadog` | `Motadata` |

Everything else (the `Configuration`, `RumConfiguration`, `Rum`, `RumMonitor`,
`useCustomEndpoint(...)`, sampling, batching, the HTTP transport) is **unchanged** in
name and behavior — only the package moved to `com.motadata.android`.

---

## 1. Get the SDK as a Gradle dependency (GitHub Packages)

The fork is published to **GitHub Packages** of `motadata2025/md-sdk-android` as
`com.motadata:…:1.0.0`. Consume it like any Maven dependency.

### 1a. Credentials
You need a **GitHub Personal Access Token (classic)** with the **`read:packages`** scope,
belonging to a user who is a member of the `motadata2025` org (or has access to the repo).

Put credentials in the **global** Gradle properties file (NOT in the project, so they aren't
committed): `C:\Users\<you>\.gradle\gradle.properties`
```properties
gpr.user=<your-github-username>
gpr.key=<your-PAT-with-read:packages>
```

### 1b. Add the repository
In **`settings.gradle.kts`** under `dependencyResolutionManagement { repositories { … } }`
(or in the app module's `repositories { }` if the project uses that style). Keep
`google()` and `mavenCentral()`; **add**:
```kotlin
maven {
    name = "MotadataGitHubPackages"
    url = uri("https://maven.pkg.github.com/motadata2025/md-sdk-android")
    credentials {
        username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
        password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
    }
}
```

### 1c. Swap the dependencies
In the **app module `build.gradle(.kts)`**, find the existing Datadog deps and replace them.
**Remove** every `com.datadoghq:dd-sdk-android-*` line. **Add** the Motadata equivalents:

```kotlin
implementation("com.motadata:motadata-rum-android:1.0.0")          // RUM (core + internal pulled in transitively)
implementation("com.motadata:motadata-rum-android-okhttp:1.0.0")   // only if you use the OkHttp interceptor
```

Full coordinate list (you normally only need the two above — the rest resolve transitively):

| Module | Coordinate |
|---|---|
| RUM | `com.motadata:motadata-rum-android:1.0.0` |
| OkHttp | `com.motadata:motadata-rum-android-okhttp:1.0.0` |
| Core | `com.motadata:motadata-rum-android-core:1.0.0` |
| Internal | `com.motadata:motadata-rum-android-internal:1.0.0` |
| Trace | `com.motadata:motadata-rum-android-trace:1.0.0` |
| Trace-api | `com.motadata:motadata-rum-android-trace-api:1.0.0` |
| Trace-internal | `com.motadata:motadata-rum-android-trace-internal:1.0.0` |

> Do a global search for `com.datadoghq` and `dd-sdk-android` in all Gradle files and make
> sure **none remain** — a leftover Datadog dependency will cause duplicate classes / conflicts.

---

## 2. Migrate the app's Kotlin/Java code

The classes moved package and a few were renamed. Do this:

1. **Replace all imports** `com.datadog.android.` → `com.motadata.android.` (global find/replace
   across `src/`).
2. **Rename the few renamed public types** (only these four families):
   - `Datadog` → `Motadata`  (e.g. `Datadog.initialize(...)` → `Motadata.initialize(...)`,
     `Datadog.setUserInfo(...)` → `Motadata.setUserInfo(...)`, `Datadog.setTrackingConsent(...)` → `Motadata.…`)
   - `DatadogInterceptor` → `MotadataInterceptor`
   - `DatadogEventListener` → `MotadataEventListener`
   - `DatadogSite` → `MotadataSite`
3. **Everything else keeps its name** — `Configuration`, `RumConfiguration`,
   `Rum`, `RumMonitor`, `GlobalRumMonitor`, `TrackingConsent`, `TracingInterceptor`, etc. Only
   the import package changed.

After this, let the IDE/compiler surface any remaining **unresolved references**. For each one,
apply the rule: same name but `com.motadata.android.…` package; if it literally started with
`Datadog`, try `Motadata`. If something still won't resolve, search this repo's published API
(or ask), but for a standard RUM + OkHttp setup the four renames above are all you need.

**Do NOT change** your `useCustomEndpoint("http://…")` calls, your API key, your RUM
application id, sampling rates, or any config values — keep them exactly as they are.

---

## 3. Keep the custom endpoint + HTTP working

You are sending to a **custom Motadata URL over plain HTTP**. Nothing about that needs to change:

- Your existing `useCustomEndpoint("http://<your-motadata-host>/…")` calls work as-is (the
  method is unchanged). With a custom endpoint set, the `MotadataSite` value is irrelevant.
- **Cleartext HTTP**: your app must already allow cleartext (since it works today with 3.10.0) —
  i.e. `android:usesCleartextTraffic="true"` in `AndroidManifest.xml`, or a
  `network-security-config` permitting your host. **Leave that exactly as-is.** The Motadata
  fork does not add or require any new flag for this in Branch 1 (it behaves like 3.10.0).

If cleartext was working before, it will work now.

---

## 4. Build & run

```
# from the app project root (Windows)
gradlew.bat :app:assembleDebug          # or your app module name
gradlew.bat installDebug                # install on a connected device/emulator
```
Then launch the app and exercise a few screens/actions so RUM emits view/action/resource events.

---

## 5. Verify the rebrand on the wire (the actual test)

You must confirm (a) the app runs, and (b) the data leaving the device is fully rebranded and
still reaches Motadata. Check as many of these as you can:

### 5a. Logcat (tag is now `Motadata`)
```
adb logcat -s Motadata
```
Expect Motadata SDK log lines (init, upload). **There should be no `Datadog` tag anymore.**
No crashes / no `UnsupportedOperationException` / no SDK errors.

### 5b. Inspect the outgoing request
Use whatever you already have (the app may register a `CurlInterceptor`/logging interceptor on
its OkHttp client), or a proxy (mitmproxy / Charles / Fiddler pointed at the device), or your
**Motadata endpoint's server-side logs**. Confirm the request to your custom endpoint shows:

- **URL query:** `...?mdsource=android&mdtags=...` (NOT `ddsource`/`ddtags`)
- **Headers:** `MD-API-KEY: <key>`, `MD-EVP-ORIGIN`, `MD-EVP-ORIGIN-VERSION`, `MD-REQUEST-ID`
  (NOT `DD-*`)
- **Body** (RUM batch, may be gzipped — decompress): each event JSON contains
  `"_md": { ... }` (NOT `"_dd"`), `"service": "motadata-rum-android"`, and view URLs/ids use
  `com/motadata/...` (NOT `com/datadog/...`).
- **Scheme/host:** still plain `http://` to your custom Motadata host.

### 5c. Confirm receipt
Confirm the events actually land in Motadata (RUM dashboard / your custom endpoint receiver /
DB) — same as they did with 3.10.0.

---

## 6. What to report back

Report concisely:
1. ✅/❌ Dependency resolved from GitHub Packages (`com.motadata:motadata-rum-android:1.0.0`).
2. ✅/❌ Code compiled after the rename (list any unresolved references you had to map).
3. ✅/❌ App built, installed, ran without SDK errors (paste any logcat errors).
4. ✅/❌ Outgoing request shows `mdsource` / `MD-*` headers / `_md` body / custom `http://` host
   (paste a sample request line + a snippet of the decoded body).
5. ✅/❌ Events received in Motadata.
6. Anything that behaved differently from the old 3.10.0 build.

---

## 7. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Could not GET .../maven-metadata.xml` **401/403** | PAT missing `read:packages`, wrong username, or user not in `motadata2025` org. Fix `gpr.user`/`gpr.key`. |
| `Could not find com.motadata:motadata-rum-android:1.0.0` | Repo URL/creds wrong, or the publish hasn't finished. Confirm the package exists at github.com/orgs/motadata2025 → Packages. |
| `Unresolved reference: Datadog` (or any `com.datadog.android.*`) | Apply §2 rename rule — `com.datadog.android`→`com.motadata.android`, `Datadog*`→`Motadata*`. |
| Duplicate class / version conflict with `com.datadoghq…` | A leftover Datadog dependency remains. Remove all `com.datadoghq:dd-sdk-android-*`. |
| `CLEARTEXT communication ... not permitted` | Manifest `usesCleartextTraffic`/network-security-config got lost. Restore it (§3). Not an SDK change. |
| Data not arriving but no error | Verify `useCustomEndpoint(...)` URL/API key unchanged; check the request actually fired (§5b). |

---

## 8. Important notes

- The Motadata fork is **frozen-equivalent to Datadog 3.10.0** — no feature changes in this
  version (`1.0.0`). It is purely a rebrand. Any behavior difference is a bug worth reporting.
- Module/folder names inside the SDK repo are still `dd-sdk-android-*` internally — **that is
  invisible to you as a consumer**; you only ever reference the `com.motadata:motadata-rum-android*`
  coordinates above.
- If a re-publish of `1.0.0` is needed later (e.g. a fix on the SDK side), GitHub Packages may
  refuse to overwrite an existing release version — the SDK team will bump the version or delete
  the old package; just update the version number in your dependency line when told.
