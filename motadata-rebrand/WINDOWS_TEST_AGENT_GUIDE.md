# Migrate the sample app to the Motadata Android RUM SDK (agent setup + build)

**You are an AI agent on a Windows machine (Android Studio).** This app currently uses the
**Datadog Android SDK 3.10.0** (`com.datadoghq:dd-sdk-android-*`) and sends RUM data to a
**custom Motadata endpoint over HTTP**. We've published a **rebranded fork** of that exact SDK
(`com.motadata:…:1.0.0`) where all `datadog`/`dd`/`DD-` naming is replaced by
`motadata`/`md`/`MD-`. The fork is **behavior-identical** to Datadog 3.10.0 — only names changed.

## ▶ Division of labor — READ THIS FIRST

**YOUR job (the agent) — Part A below:**
1. Wire up the GitHub Packages dependency (repo + credentials).
2. Swap the Datadog dependencies for the Motadata ones.
3. Migrate the app code (imports + a few class renames) until it **compiles**.
4. **Build the debug APK** (and install it if a device is connected). Then **STOP and hand off.**

**The HUMAN's job — Part B below (do NOT do these):**
- Launching/running the app, tapping buttons/navigating to generate RUM events,
  capturing the outgoing requests, and verifying events reach Motadata.

So: get it to **build and install cleanly**, report status, and tell the human it's ready to run.
Do not try to drive the UI, capture traffic, or check the backend yourself.

**Prerequisite the human gives you:** a **GitHub `read:packages` PAT** + the **GitHub username**
that owns it (needed to download the dependency at build time). You cannot create this — ask the
human for it if it isn't already in `gradle.properties`.

---

# PART A — YOUR TASKS (setup + code + build)

## A0. What changed (mental model)

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

Everything else (`Configuration`, `RumConfiguration`, `Rum`, `RumMonitor`,
`useCustomEndpoint(...)`, sampling, batching, HTTP transport) is **unchanged** in name and
behavior — only the package moved to `com.motadata.android`.

## A1. Get the SDK as a Gradle dependency (GitHub Packages)

The fork is published to **GitHub Packages** as `com.motadata:…:1.0.0`. The packages are
**public**, so **any GitHub account's `read:packages` token works** — the human will give you a
PAT + username (you do NOT need access to the `motadata2025/md-sdk-android` repo).

### A1a. Credentials
Put the human-supplied credentials in the **global** Gradle properties file (NOT in the project —
keep them out of source control): `C:\Users\<user>\.gradle\gradle.properties`
```properties
gpr.user=<github-username-that-owns-the-PAT>
gpr.key=<the-read:packages-PAT>
```

### A1b. Add the repository
In **`settings.gradle.kts`** under `dependencyResolutionManagement { repositories { … } }`
(or the app module's `repositories { }` if the project uses that style). Keep `google()` and
`mavenCentral()`; **add**:
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

### A1c. Swap the dependencies
In the **app module `build.gradle(.kts)`**, **remove** every `com.datadoghq:dd-sdk-android-*`
line and **add** the Motadata equivalents:
```kotlin
implementation("com.motadata:motadata-rum-android:1.0.0")          // RUM (core + internal pulled in transitively)
implementation("com.motadata:motadata-rum-android-okhttp:1.0.0")   // only if the app uses the OkHttp interceptor
```

Full coordinate list (normally only the two above are needed — the rest resolve transitively):

| Module | Coordinate |
|---|---|
| RUM | `com.motadata:motadata-rum-android:1.0.0` |
| OkHttp | `com.motadata:motadata-rum-android-okhttp:1.0.0` |
| Core | `com.motadata:motadata-rum-android-core:1.0.0` |
| Internal | `com.motadata:motadata-rum-android-internal:1.0.0` |
| Trace | `com.motadata:motadata-rum-android-trace:1.0.0` |
| Trace-api | `com.motadata:motadata-rum-android-trace-api:1.0.0` |
| Trace-internal | `com.motadata:motadata-rum-android-trace-internal:1.0.0` |

> Global-search all Gradle files for `com.datadoghq` and `dd-sdk-android` and make sure **none
> remain** — a leftover Datadog dependency causes duplicate-class / version conflicts.

## A2. Migrate the app's Kotlin/Java code

1. **Replace all imports** `com.datadog.android.` → `com.motadata.android.` (global find/replace
   across `src/`).
2. **Rename the four renamed public types:**
   - `Datadog` → `Motadata` (e.g. `Datadog.initialize(...)` → `Motadata.initialize(...)`,
     `Datadog.setUserInfo(...)` → `Motadata.setUserInfo(...)`, etc.)
   - `DatadogInterceptor` → `MotadataInterceptor`
   - `DatadogEventListener` → `MotadataEventListener`
   - `DatadogSite` → `MotadataSite`
3. **Everything else keeps its name** — `Configuration`, `RumConfiguration`, `Rum`, `RumMonitor`,
   `GlobalRumMonitor`, `TrackingConsent`, `TracingInterceptor`, etc. Only the import package changed.

Then let the compiler surface any remaining **unresolved references** and apply the rule: same
name, `com.motadata.android.…` package; if it literally started with `Datadog`, try `Motadata`.
For a standard RUM + OkHttp app, the four renames above are all you need.

**Do NOT change** `useCustomEndpoint("http://…")` calls, the API key, the RUM application id,
sampling rates, or any config values. Keep all of them exactly as they are.

## A3. Leave the custom endpoint + cleartext HTTP exactly as-is

The app sends to a **custom Motadata URL over plain HTTP** — none of that changes:
- `useCustomEndpoint("http://<host>/…")` is unchanged (with a custom endpoint set, the
  `MotadataSite` value is irrelevant).
- **Cleartext HTTP** is allowed via the app's existing `android:usesCleartextTraffic="true"` (or
  `network-security-config`) in `AndroidManifest.xml`. **Do not remove or alter it.** The fork
  needs no new flag for this in `1.0.0` (it behaves exactly like 3.10.0).

Just confirm those settings are still present after your edits; don't add anything new.

## A4. Build (and install) — then STOP

```
gradlew.bat :app:assembleDebug      # build the debug APK (use the real app module name)
gradlew.bat installDebug            # OPTIONAL: install it if a device/emulator is connected
```
Goal: a **clean build** and, if a device is attached, the app **installed**. **Do not launch it,
tap through it, or capture traffic** — that's the human's part. Hand off once it builds/installs.

## A5. Report back to the human

Report concisely:
1. ✅/❌ Dependency resolved from GitHub Packages (`com.motadata:motadata-rum-android:1.0.0`).
2. ✅/❌ Code compiled — list every unresolved reference you had to map and how.
3. ✅/❌ `assembleDebug` succeeded; ✅/❌ `installDebug` succeeded (or "no device attached").
4. The exact files you changed (Gradle files + which source files).
5. Anything ambiguous you had to guess, and any leftover `datadog`/`dd` reference you could not
   resolve.
6. **"Ready for you to run and capture events"** (or the blocker preventing that).

---

# PART B — THE HUMAN'S TASKS (run + capture + verify)

> This section is for the person, not the agent. The agent should point the human here.

Once the app is built/installed:

1. **Run the app** and **tap through it** — open screens, trigger actions/network calls so RUM
   emits view / action / resource events.
2. **Watch logcat** (tag is now `Motadata`):
   ```
   adb logcat -s Motadata
   ```
   Expect Motadata SDK lines (init, batch upload). No `Datadog` tag; no crashes / SDK errors.
3. **Capture the outgoing request** (via the app's `CurlInterceptor`/logging interceptor, a proxy
   like Charles/mitmproxy/Fiddler, or the Motadata endpoint's server logs) and confirm it is fully
   rebranded:
   - **URL query:** `...?mdsource=android&mdtags=...` (NOT `ddsource`/`ddtags`)
   - **Headers:** `MD-API-KEY`, `MD-EVP-ORIGIN`, `MD-EVP-ORIGIN-VERSION`, `MD-REQUEST-ID` (NOT `DD-*`)
   - **Body** (RUM batch, may be gzipped — decompress): each event has `"_md": { … }` (NOT `"_dd"`),
     `"service": "motadata-rum-android"`, and view urls/ids use `com/motadata/...` (NOT `com/datadog/...`)
   - **Scheme/host:** still plain `http://` to your custom Motadata host.
4. **Confirm receipt** — events land in Motadata (dashboard / custom endpoint receiver / DB), same
   as with 3.10.0.

If all of the above match, Branch 1 is verified working.

---

# Reference

## Troubleshooting (agent)

| Symptom | Cause / fix |
|---|---|
| `Could not GET .../maven-metadata.xml` **401/403** | PAT missing `read:packages`, or `gpr.user`/`gpr.key` mismatch (username must own the PAT). Packages are public, so any account's `read:packages` PAT works. |
| `Could not find com.motadata:motadata-rum-android:1.0.0` | Repo URL/creds wrong. Confirm the package at github.com/motadata2025/md-sdk-android → Packages. |
| `Unresolved reference: Datadog` / `com.datadog.android.*` | Apply §A2 rule — `com.datadog.android`→`com.motadata.android`, `Datadog*`→`Motadata*`. |
| Duplicate class / conflict with `com.datadoghq…` | A leftover Datadog dependency remains — remove all `com.datadoghq:dd-sdk-android-*`. |
| `CLEARTEXT communication ... not permitted` (at runtime, human sees this) | Manifest `usesCleartextTraffic`/network-security-config was lost — restore it (§A3). Not an SDK change. |

## Notes

- The fork is **frozen-equivalent to Datadog 3.10.0** — no feature changes in `1.0.0`, purely a
  rebrand. Any behavior difference is a bug worth reporting.
- The SDK repo's internal module folders are still named `dd-sdk-android-*` — **invisible to you
  as a consumer**; you only reference the `com.motadata:motadata-rum-android*` coordinates above.
- If a fixed `1.0.0` is re-published later, GitHub Packages may refuse to overwrite the version;
  the SDK team will bump it — just update the version in the dependency line when told.
