# Migrate the sample app to the Motadata Android RUM SDK (agent setup + build + SOP)

**You are an AI agent on a Windows machine (Android Studio).** This app currently uses the
**Datadog Android SDK 3.10.0** (`com.datadoghq:dd-sdk-android-*`) and sends RUM data to a
**custom Motadata endpoint over HTTP**. We've published a **rebranded fork** of that exact SDK
(`com.motadata:…:1.0.0`) where all `datadog`/`dd`/`DD-` naming is replaced by
`motadata`/`md`/`MD-`. The fork is **behavior-identical** to Datadog 3.10.0 — only names changed.

## ▶ Division of labor — READ THIS FIRST

**YOUR job (the agent):**
- **Part A** — wire the dependency, swap Datadog→Motadata, migrate the code until it **compiles**,
  **build the debug APK** (install if a device is connected), then **STOP and report**.
- **Part C** — **only after the human tells you "everything works as expected,"** write the
  **client-integration SOP** MD file (Kotlin + Java).

**The HUMAN's job — Part B (do NOT do these):**
- Launch/run the app, tap buttons to generate RUM events, capture the outgoing requests, and
  verify events reach Motadata.

So: build & install cleanly → report "ready to run" → wait. Do not drive the UI, capture traffic,
or check the backend yourself. Only after the human confirms success, do Part C.

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
**public**, so **any GitHub account's `read:packages` token works** — the human gives you a PAT +
username (you do NOT need access to the `motadata2025/md-sdk-android` repo).

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
5. **Keep a precise list of every SDK-integration change you made** — you will need it for Part C.
6. **"Ready for you to run and capture events"** (or the blocker preventing that).

---

# PART B — THE HUMAN'S TASKS (run + capture + verify)

> This section is for the person, not the agent. The agent points the human here, then waits.

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

If all of the above match, Branch 1 is verified working → tell the agent to proceed to **Part C**.

---

# PART C — WRITE THE CLIENT INTEGRATION SOP (only after the human confirms success)

**Trigger:** the human has said *"everything works as expected."* Until then, do NOT do this.

Create a new file **`MOTADATA_ANDROID_CLIENT_INTEGRATION_SOP.md`** that documents — as a clean,
reusable **step-by-step SOP** — exactly what a client must add to **their own** app to enable
Motadata RUM. Base it on the changes you actually made in Part A, but **generalize**: strip this
app's business logic and secrets, replace concrete values with clearly-named placeholders
(`<MOTADATA_RUM_APPLICATION_ID>`, `<MOTADATA_CLIENT_TOKEN>`, `http://<your-motadata-host>/…`).

**Hard requirement: every code step must show BOTH Kotlin AND Java** (two fenced blocks, labelled).
The SDK is Java-interop-friendly; derive the exact Java signatures from the working integration /
the IDE — do not guess. Keep snippets minimal and correct.

The SOP must contain these sections, in order:

1. **Overview & prerequisites** — minSdk, that it's a RUM SDK feeding a custom Motadata endpoint
   over HTTP(S), and the artifacts used.
2. **Step 1 — Add the repository** — the GitHub Packages `maven { … }` block + the
   `gradle.properties` credential keys (note: public package, any GitHub `read:packages` PAT).
   *(If the client will instead get it from Maven Central in future, note that as a one-line
   alternative — but document GitHub Packages as the current method.)*
3. **Step 2 — Add dependencies** — `motadata-rum-android` (+ `-okhttp` if using OkHttp).
4. **Step 3 — AndroidManifest** — `INTERNET` permission; and **for an HTTP (cleartext) endpoint**,
   `android:usesCleartextTraffic="true"` (or a `network-security-config` scoped to the host). Show
   the manifest XML.
5. **Step 4 — Initialize the SDK** — build a `Configuration`, call `Motadata.initialize(...)` with
   the client token + env + tracking consent. **Kotlin + Java.**
6. **Step 5 — Enable RUM with the custom endpoint** — build a `RumConfiguration` (application id +
   `useCustomEndpoint("http://<host>/…")` + any sampling), call `Rum.enable(...)`. **Kotlin + Java.**
7. **Step 6 — Instrument network calls (OkHttp)** — add `MotadataInterceptor` (and, if used,
   `MotadataEventListener`) to the `OkHttpClient`; mention `setTraceSampleRate` if relevant.
   **Kotlin + Java.**
8. **Step 7 — (Optional) extra instrumentation** — set user info (`Motadata.setUserInfo`), add
   global attributes, manual view/action tracking via `GlobalRumMonitor.get()`, and automatic
   tracking strategies (activity/fragment view tracking, user-action tracking) if the app used them.
   **Kotlin + Java** for any you include.
9. **Step 8 — Verify** — logcat tag `Motadata`; the request shows `mdsource` / `MD-*` / `_md` over
   the custom host; events appear in Motadata. (Reuse Part B's checklist, condensed.)
10. **Notes** — behavior-equivalent to Datadog 3.10.0; placeholders to replace; where to get the
    client token / application id (the client's Motadata org).

Keep it self-contained and copy-pasteable so a client developer (Kotlin **or** Java) can follow it
end-to-end without this migration context. After writing it, tell the human the SOP file is ready.

---

# Reference

## Troubleshooting (agent)

| Symptom | Cause / fix |
|---|---|
| `Could not GET .../maven-metadata.xml` **401/403** | PAT missing `read:packages`, or `gpr.user`/`gpr.key` mismatch (username must own the PAT). Packages are public, so any account's `read:packages` PAT works. |
| `Could not find com.motadata:motadata-rum-android:1.0.0` | Repo URL/creds wrong. Confirm the package at github.com/motadata2025/md-sdk-android → Packages. |
| `Unresolved reference: Datadog` / `com.datadog.android.*` | Apply §A2 rule — `com.datadog.android`→`com.motadata.android`, `Datadog*`→`Motadata*`. |
| Duplicate class / conflict with `com.datadoghq…` | A leftover Datadog dependency remains — remove all `com.datadoghq:dd-sdk-android-*`. |
| `CLEARTEXT communication ... not permitted` (runtime, human sees this) | Manifest `usesCleartextTraffic`/network-security-config was lost — restore it (§A3). Not an SDK change. |

## Notes

- The fork is **frozen-equivalent to Datadog 3.10.0** — no feature changes in `1.0.0`, purely a
  rebrand. Any behavior difference is a bug worth reporting.
- The SDK repo's internal module folders are still named `dd-sdk-android-*` — **invisible to you
  as a consumer**; you only reference the `com.motadata:motadata-rum-android*` coordinates above.
- If a fixed `1.0.0` is re-published later, GitHub Packages may refuse to overwrite the version;
  the SDK team will bump it — just update the version in the dependency line when told.
