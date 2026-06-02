# Motadata Android RUM SDK — Complete Rebrand Plan

> **Goal:** fork `dd-sdk-android` 3.10.0 → a fully rebranded **Motadata** Android RUM SDK that works
> exactly as `MOTADATA_ANDROID_RUM_ONBOARDING.md` describes, with the **custom use-cases** the browser
> fork (`motadata2025/browser-sdk@motadata-dev`) already ships.
>
> **The guarantee:** after this plan, the word *datadog* / *dd-sdk* / *DD-* appears **nowhere** a
> customer or your server can see it — not in **imports**, not in **class names**, not in **event
> payloads**, not in **HTTP headers/query params**, not in **logs**, not in **Maven coordinates**.
>
> **The one legal exception (must stay):** the Apache-2.0 attribution line in each source file header
> (`This product includes software developed at Datadog…` + `Copyright … Datadog, Inc.`). The license
> *requires* it. It ships only inside source/`.jar` headers — never in events or on the wire. Your CI
> "no-datadog" gate (§6) must whitelist exactly this header.
>
> Every file:line below is verified against the local `dd-sdk-android` clone (3.10.0). Scope = **SDK only**.

---

# EXECUTION — two-branch strategy

Two branches, both off the **3.10.0** base. **Branch 1 first (rename only), build green, then cut Branch 2.**

## Branch 1 — `motadata-dev` — REBRAND / RENAME ONLY (no functional changes)
Only changes a **name / string / coordinate** — no new fields, no new params, no behavior change. After
this branch the SDK is fully debranded ("zero datadog") yet behaves byte-for-byte like upstream 3.10.0.

| In scope on `motadata-dev` | Plan section |
|---|---|
| Package + class renames | 1A |
| In-event string renames (launch view url/id, `telemetry.service`, flags/meter name, **thread names**) | 1B.1–1B.6, 1B.8 |
| `_dd`→`_md`, `ddtags`→`mdtags` **body** envelope (edit in-repo JSON schemas + regen) — debrand of a field name | 1B.7, 2.6 |
| Wire **name** renames: headers `DD-*`→`MD-*`, query `ddsource`→`mdsource` / `ddtags`→`mdtags`, logcat tags | 1C |
| Runtime debrand: NTP host, storage dir, User-Agent, Logcat **messages**, telemetry op names | 1E |
| Maven group / version `1.0.0` / POM / artifact ids | 1D |
| Logcat **dev tag** rename `Datadog`→`Motadata` | 3.1 |
| Consequent: `apiDumpAll` + `generateApiSurfaceAll`, ProGuard rules, fix rename-affected tests, build green | Part 6 (1–4) |
| Telemetry: **stays ON, no change** | 3.3 |

> Note: renaming the `DD-API-KEY` *header* → `MD-API-KEY` is here (a name change). **Adding** the
> `md-api-key` *query param* is functional → Branch 2. So on Branch 1 the SDK is fully renamed but does
> not yet send the query-param the server authenticates on — that's expected; Branch 2 completes it.
>
> **`_dd`→`_md` is in Branch 1** (decision): `_dd` literally contains "dd" and appears in **every event
> body**, so it MUST be scrubbed for Branch 1 to be truly "zero datadog". It's debranding (a field-name
> rename) — just heavier than the others (needs editing the **in-repo** JSON schemas + codegen regen).
> Rule: **Branch 1 = ALL debranding; Branch 2 = ONLY new fields/params/behavior (no renames).**

## Branch 2 — `motadata-dev-with-functional-changes` — branched FROM `motadata-dev` — FUNCTIONAL ADDITIONS
Everything that **adds a field, a param, or a behavior** — layered on the finished, fully-debranded rename.

| In scope on `motadata-dev-with-functional-changes` | Plan section |
|---|---|
| **ADD** `md-api-key` **query param** (auth) | 2.1 |
| Make `allowClearTextHttp()` **public** (visibility change) | 2.2 |
| **ADD** `view.is_view_completed` | 2.3 |
| Keep-tracking delay tune (5→1 min) | 2.4 |
| **ADD** `session.created` + `context._timing` | 2.5 |
| **ADD** `"Motadata SDK initialized"` log line | 3.2 |

**Workflow:** complete Branch 1 → `apiDumpAll` + build + tests green → commit → cut Branch 2 from it →
layer the functional changes → build + test → publish from whichever branch you ship.

---

# PART 1 — The "Zero-Datadog" scrub matrix

Four places `datadog` hides. **1B is the one most people miss** — strings that land *inside event JSON*.

## 1A — Code identity (imports & class names)

| # | Change | Where |
|---|---|---|
| 1A.1 | Package `com.datadog.android.*` → `com.motadata.android.*` (every dir + import) | whole repo — IDE "Rename Package" |
| 1A.2 | `Datadog` → `Motadata` | `dd-sdk-android-core/.../Datadog.kt` → onboarding's `Motadata.initialize(...)` |
| 1A.3 | `DatadogInterceptor` → `MotadataInterceptor` | `dd-sdk-android-okhttp` → resource snippet |
| 1A.4 | `DatadogEventListener` → `MotadataEventListener` | `dd-sdk-android-okhttp` (per-phase timings) |
| 1A.5 | `DatadogSite` → `MotadataSite` | core (enum; its `*.datadoghq.com` hosts are dormant when `useCustomEndpoint` is set, but rename for cleanliness) |
| 1A.6 | `DatadogEventListener` → `MotadataEventListener` (`.Factory()` for per-phase timings) | `integrations/dd-sdk-android-okhttp/.../DatadogEventListener.kt:47` |
| 1A.7 | Other public `Datadog*` types in **core/rum** a consumer can touch: `DatadogContext`, `DatadogDatabaseErrorHandler` (sqlite), `DatadogDataConstraints` | core, rum |

**Exact customer-facing API after rename (the onboarding snippet — verified):**

| Onboarding uses | Today (Datadog) | Module | Verified |
|---|---|---|---|
| `Motadata.initialize(ctx, config, consent)` | `object Datadog` | core `Datadog.kt:34` | ✅ |
| `Configuration.Builder(token, env, variant).allowClearTextHttp().build()` | `Configuration` (pkg only) | core | ✅ (allowClearTextHttp → public, §2.2) |
| `Rum.enable(rumConfig)` | `Rum` (pkg only) | rum | ✅ |
| `RumConfiguration.Builder(appId).useCustomEndpoint(..).trackUserInteractions().trackLongTasks(100L).useViewTrackingStrategy(ActivityViewTrackingStrategy(false))` | pkg only | rum | ✅ |
| `TrackingConsent.GRANTED` | pkg only | core | ✅ |
| `MotadataInterceptor.Builder(emptyMap()).build()` | `DatadogInterceptor.Builder(Map<String,Set<TracingHeaderType>>).build()` | okhttp `DatadogInterceptor.kt:370,392` | ✅ (`emptyMap()` fits the Map param) |
| `MotadataEventListener.Factory()` | `DatadogEventListener.Factory()` | okhttp `DatadogEventListener.kt:238` | ✅ |
| `GlobalRumMonitor.get().startView(...)` (troubleshooting) | pkg only | rum | ✅ |

> Classes that **keep their names** (only the package moves): `Configuration`, `Rum`, `RumConfiguration`,
> `TrackingConsent`, `ActivityViewTrackingStrategy`, `GlobalRumMonitor`. All resolve from
> `com.motadata.android.*` after 1A.1.
>
> **Scope note:** this plan covers the **3 modules your onboarding needs** — `core`, `rum`, `okhttp`.
> The other integration modules (`rx`, `glide`, `coil`/`coil3`, `timber`, `sqldelight`, `trace`, `logs`,
> `ndk`, `session-replay`) each carry their own `Datadog*` classes (e.g. `DatadogTree`, `DatadogTracer`,
> `DatadogCoilRequestListener`). You only rebrand those **if** you ship them. For the RUM + resource-events
> use-case, they are not required.

## 1B — Strings that appear INSIDE event payloads ⚠️ (the critical ones)

These are hardcoded strings that get serialized into the JSON your server receives. If you skip these,
your dashboards will literally show "datadog".

| # | String in event | Constant (file:line) | Appears as | Change to |
|---|---|---|---|---|
| 1B.1 | `com/datadog/application-launch/view` | `RumViewManagerScope.kt:461` `RUM_APP_LAUNCH_VIEW_URL` | **`view.url`** of the app-launch view | `com/motadata/application-launch/view` |
| 1B.2 | `com.datadog.application-launch.view` | `RumViewManagerScope.kt:460` `RUM_APP_LAUNCH_VIEW_ID` | the launch **`view.id`** seed | `com.motadata.application-launch.view` |
| 1B.3 | `dd-sdk-android` | `TelemetryEventHandler.kt:601` `TELEMETRY_SERVICE_NAME` | **`telemetry.service`** | `motadata-rum-android` |
| 1B.4 | `dd-sdk-android` | `PrecomputedAssignmentsRequestFactory.kt:147` `SDK_NAME` (flags) | flags service id | `motadata-rum-android` |
| 1B.5 | `dd-sdk-android` | `SdkFeature.kt:471`, `BenchmarkUploads.kt:76` `METER_NAME` | internal metrics meter | `motadata-rum-android` |
| 1B.6 | `com.datadog.android.rum.internal.anr.ANRException` | `ANRException.kt` (package) | **`error.type`** on every ANR | auto-fixed by 1A.1 → `com.motadata.…ANRException` |
| 1B.7 | `_dd` envelope + `_dd.sdk_name` | in-repo `src/main/json/` schema → codegen | **`_dd.*`** block on every event | ✅ `_md` (edit in-repo JSON + regen — §2.6) |
| 1B.8 | Thread names `datadog-*-thread-*`, `datadog_shutdown`, `DatadogUploadWorker`, `DatadogBackgroundUpload` | `DatadogThreadFactory.kt:20`, `DatadogCore.kt:728`, `WorkManagerUtils.kt:23-24` | **`error.threads[].name`** on crash/ANR | `motadata-*` (also see 1E.2-1E.4) |

> 1B.1–1B.3 are the must-do trio: the launch view URL and the telemetry service name are guaranteed to
> show up in your captures. 1B.6 is free (the package rename fixes the ANR class FQN automatically).

## 1C — On-the-wire (HTTP headers, query params, log tag)

| # | String | Constant (file:line) | Change to |
|---|---|---|---|
| 1C.1 | `DD-API-KEY` (header) | `RequestFactory.kt:48` `HEADER_API_KEY` | `MD-API-KEY` (and see §2.1 — also send as query) |
| 1C.2 | `DD-EVP-ORIGIN` | `RequestFactory.kt:53` `HEADER_EVP_ORIGIN` | `MD-EVP-ORIGIN` |
| 1C.3 | `DD-REQUEST-ID` | `RequestFactory.kt:63` `HEADER_REQUEST_ID` | `MD-REQUEST-ID` |
| 1C.4 | `DD-IDEMPOTENCY-KEY` | `RequestFactory.kt:78` `DD_IDEMPOTENCY_KEY` | `MD-IDEMPOTENCY-KEY` |
| 1C.5 | `ddsource` (query) | `RequestFactory.kt:68` `QUERY_PARAM_SOURCE` | `mdsource` |
| 1C.6 | `ddtags` (query) | `RequestFactory.kt:73` `QUERY_PARAM_TAGS` | `mdtags` |
| 1C.7 | `DD_LOG` (logcat) | `SdkInternalLogger.kt:286` `SDK_LOG_TAG` | `MD_LOG` |
| 1C.8 | `Datadog` (logcat dev tag) | `SdkInternalLogger.kt:287` `DEV_LOG_TAG` | `Motadata` |

## 1D — Maven coordinates & POM

| # | Change | File:line |
|---|---|---|
| 1D.1 | `GROUP_ID = "com.datadoghq"` → `"com.motadata"` | `buildSrc/.../config/MavenConfig.kt:18` |
| 1D.2 | Version `Version(3,10,0,Release)` → `Version(1,0,0,Release)` | `buildSrc/.../config/AndroidConfig.kt:22` |
| 1D.3 | POM `url`/`organization`/`developers`/`email`/`scm` (all Datadog) → Motadata | `MavenConfig.kt:58-89` |
| 1D.4 | Artifact ids → `motadata-rum-android`, `-core`, `-okhttp` | each module `build.gradle.kts` via `publishingConfig(customArtifactId=…)` |

## 1E — Runtime & network leaks ⚠️ (a type-rename MISSES these — all customer/server-facing)

These are not imports or class names, so an IDE "rename package" never touches them. Several land **in
event payloads** or are **live network calls to Datadog infrastructure**. Verified in the 3.10.0 source.

| # | Leak | File:line | Where the customer/server sees it | Change to |
|---|---|---|---|---|
| 1E.1 | NTP hosts `0..3.datadog.pool.ntp.org` | `DatadogNtpEndpoint.kt:17-32` (used in `CoreFeature.kt:472-476`) | **live UDP/DNS to Datadog infra** for clock sync — fails on air-gapped on-prem, and leaks `datadog` DNS lookups on the customer network | ✅ **DECIDED: rebrand to `0..3.pool.ntp.org`** (not disable — keeps clock-sync; air-gapped just falls back to device clock automatically) |
| 1E.2 | Worker thread names `datadog-<ctx>-thread-<n>` | `DatadogThreadFactory.kt:20` | **`error.threads[].name`** in every crash/ANR event (e.g. `datadog-rum-thread-1`, `datadog-upload-thread-1`) | `motadata-<ctx>-thread-<n>` |
| 1E.3 | Shutdown thread `datadog_shutdown` | `DatadogCore.kt:728` | crash/ANR thread dumps | `motadata_shutdown` |
| 1E.4 | WorkManager `DatadogUploadWorker` / `DatadogBackgroundUpload` | `WorkManagerUtils.kt:23-24` | `adb dumpsys jobscheduler`, thread dumps | `Motadata*` |
| 1E.5 | Upload `User-Agent` = `Datadog/$sdkVersion …` | `DataOkHttpUploader.kt:119` | **HTTP `User-Agent` header** your server receives (verify — Dalvik UA may override) | `Motadata/$sdkVersion …` |
| 1E.6 | On-disk storage dir `datadog-%s` | `CoreFeature.kt:780` `DATADOG_STORAGE_DIR_NAME` | app's internal files dir folder name on the device | `motadata-%s` |
| 1E.7 | Logcat message strings — "Datadog has not been initialized.", "Make sure you initialized the Datadog SDK…", "Datadog network instrumentation configuration is incorrect:", etc. (dozens) | core/rum (many) | **the customer's Logcat** | bulk replace "Datadog"→"Motadata" in message literals |
| 1E.8 | Telemetry op names `DatadogCore.*` | core | telemetry events sent to your server | `MotadataCore.*` |

> **1E.1 (NTP) and 1E.2 (thread names) are the two most important** beyond the obvious renames:
> NTP is a *functional* dependency on Datadog servers (breaks air-gapped on-prem); thread names appear
> *inside crash/ANR event JSON*. Both are easy to miss and both are customer/server-facing.

---

# PART 2 — Custom use-cases (functional, browser-fork parity)

The browser fork's functional changes (Pratham & Ashish), mapped to Android with exact targets.

### Browser changes that are NOT needed on Android (verified from the diffs)

| Browser change | Commit | Why N/A on Android |
|---|---|---|
| Browser detection (`ua-parser-js`) + `X-Browser-Name` header | `d7d0c2b`, `ade174a`, `4d3e323` | mobile sends authoritative `device.*`/`os.*` in the payload |
| URL grouping — `transformPathName` (numeric path segments → `?`) | `f338229` | Android `view.url` is the Activity/Fragment **class path**, not a numeric URL — nothing to group (relevant only to URL-style views: browser/RN) |
| Force `sessionReplaySampleRate = 0` (block Session Replay) | `f338229` | Android SR is a **separate module** (`dd-sdk-android-session-replay`); just **don't ship it** → SR is off by default, nothing to block |

## 2.1 `md-api-key` query param — REQUIRED (auth)  · browser `f338229`

Your Vert.x listener authenticates on the **`md-api-key` query param**; the SDK today sends the token only
as the `DD-API-KEY` header. In `RumRequestFactory.kt:57-69` (`buildUrl`), add to `queryParams`:
```kotlin
put("md-api-key", context.clientToken)
```
(Combine with 1C.1/1C.5 to also rename the header + `ddsource`→`mdsource`.)

> **Endpoint format already correct:** `RumRequestFactory.kt:67`
> `customEndpointUrl ?: (site + "/api/v2/rum")` — `useCustomEndpoint` uses your URL **as-is**, so onboarding's
> `{{endpoint.url}} = protocol://host/api/v2/rum` is right (no double path).

## 2.2 http/https flexibility — REQUIRED for on-prem  · browser `2b6316e`

Make `allowClearTextHttp()` **public** (drop `internal`): `Configuration.kt:288`. The onboarding HTTP
snippet calls it; today it won't compile for customers. `useCustomEndpoint` already accepts `http`/`https`.

## 2.3 `is_view_completed` — last-view / final-update flag — REQUIRED  · browser `dafd3b5`

**Why:** mobile emits many view events per view (`_dd.document_version` 1,2,3…). Mark the final, authoritative
one so the backend keeps **one row per view**.

**Browser** had to build the whole lifecycle (`isViewCompleted` field + `triggerFinalViewUpdate()` +
`setTimeout(... KEEP_TRACKING_AFTER_VIEW_DELAY)`), emitting `view.is_view_completed: "no"` then `"yes"`.

**Android is simpler — the completion state already exists:**
- `RumViewScope.kt:1117` `internal fun sendViewUpdate(...)` — emits every view event.
- `RumViewScope.kt:1124` **`val viewComplete = isViewComplete()`** — already computed (logic at `:1594`;
  true only when the view is stopped **and** all pending resources/actions/long-tasks resolved).

So **don't rebuild any lifecycle** — just serialize the existing flag. In `sendViewUpdate`, write:
```kotlin
view.is_view_completed = if (viewComplete) "yes" else "no"
```
Placement — pick one:
- **(a)** add `is_view_completed` to `view-schema.json` in your `rum-events-format` fork + regenerate (matches browser's `view.is_view_completed` exactly), or
- **(b)** emit via `additionalProperties` like Android already does for flat keys (`action.target.classname`) — no schema regen.

## 2.4 Keep-tracking delay (recommended)  · browser `dafd3b5`

Browser cut `KEEP_TRACKING_AFTER_VIEW_DELAY` 5min→1min so the terminal emit lands sooner. Android's analog
is the stopped-view retention governed by `isViewComplete()`; tune toward ~1 min for predictable latency.

## 2.5 `session.created` & `context._timing` — ✅ DECIDED: DO IT (full parity)  · browser `abf780f`, `7a08f3f`

**What they are:** RUM events carry `date` (when the *event* happened) and `session.id`, but **not when the
*session* began**. These two inject that missing "start" information onto every event:
- **`session.created`** = epoch-ms when the current session started. Lets the backend compute session
  duration / order sessions / bucket "time since start" without reconstructing it from whichever event
  arrives first (events are batched, sampled, out-of-order).
- **`context._timing`** = `{ navigationStart, relativeTime }` where `navigationStart` = session/app-start
  time and `relativeTime = event.date - navigationStart` = ms into the session this event occurred. Gives
  a clock-skew-free relative timeline for deterministic ordering.

**Android mapping (the start time already exists internally):**
- `RumSessionScope.kt:71` `sessionStartNs = AtomicLong(timeProvider.getDeviceElapsedTimeNanos())` already
  tracks session start (monotonic). For `session.created` capture the **wall-clock** value too at session
  start/renew: `timeProvider.getDeviceTimestamp()` (epoch ms).
- Propagate it via `RumContext` (`RumContext.kt` — already carries `sessionId`, `sessionStartReason`; add a
  `sessionStartTimestampMs` field next to them).
- In event serialization write `session.created = sessionStartTimestampMs`, and
  `context._timing = { navigationStart: sessionStartTimestampMs, relativeTime: event.date - sessionStartTimestampMs }`.

Add these if you want the same session-start columns the browser pipeline populates.

## 2.6 `_dd`→`_md`, `ddtags`→`mdtags` envelope — ✅ DECIDED: DO IT (consistency with browser)  · browser `f338229`

The whole SDK envelope (`_dd.*`) and the `ddtags` field get renamed to `_md.*` / `mdtags`, matching the
browser fork so one backend envelope rule works across **browser + mobile**. (Also clears 1B.7 — no `_dd`
in events.)

**On Android these come from the schema + codegen** (not plain object keys like browser), so:
1. **Edit the in-repo JSON schemas** at `features/dd-sdk-android-rum/src/main/json/` (25 committed files —
   **no external repo / submodule**, unlike iOS): rename the `_dd` envelope key → `_md` and `ddtags` → `mdtags`.
2. **Regenerate** the Android models (the `json2kotlin` codegen runs over `src/main/json/` at build) so the
   generated `_dd`/`ddtags` properties become `_md`/`mdtags`.
3. Update any internal Kotlin referencing those model fields + the unit tests asserting `_dd`/`ddtags`.

**Effort:** moderate (schema edit + regen + test fixups), but it's the right call for one consistent envelope.
Nested fields (`_dd.format_version`, `_dd.session.session_precondition`, `_dd.document_version`,
`_dd.configuration.*`, `_dd.replay_stats.*`) all move under `_md` automatically when the top-level key is
renamed. Note `ddtags` here is the **body** field; the retry-metadata **query** param `ddtags`
(`RequestFactory.kt:73`) is a separate rename in 1C.6.

---

# PART 3 — Make onboarding compile & verify

| # | Change | File:line | Note |
|---|---|---|---|
| 3.1 | Logcat dev tag → `Motadata` | `SdkInternalLogger.kt:287` (= 1C.8) | doc filters Logcat by tag `Motadata` |
| 3.2 | Add `"Motadata SDK initialized"` log at end of `initialize()` | `Datadog.kt` | ⚠️ no such success log exists by default — add it or soften doc step 3 |
| 3.3 | **Telemetry: KEEP ON (default 20%) — ✅ DECIDED. Do NOT change `DEFAULT_TELEMETRY_SAMPLE_RATE`** | `RumFeature.kt:788` (leave as `20f`) | Matches the browser fork (it keeps telemetry on too). Telemetry goes only to **your** endpoint under `useCustomEndpoint` (no Datadog leak). Server **ignores** telemetry events. ⚠️ Because it's on, the telemetry-path scrubs are **required**: `telemetry.service` (1B.3), `_dd`→`_md` (2.6), telemetry op names (1E.8). Benign noise: recurring `"Unexpected status code 200"` telemetry — harmless artifact of your intake returning 200 vs the SDK's expected 202; ignore it server-side. |

---

# PART 4 — Publishing

The repo already has the machinery (Sonatype Central via `nexus-publish`, GPG signing, sources/javadoc).

- **You can't use `com.datadoghq`.** Get your namespace: **`com.motadata`** (verify `motadata.com` via DNS TXT) or **`io.github.motadata2025`** (free, instant).
- **Channels (parallel):**
  - **Pilot now:** JitPack / GitHub Packages (zero setup) → end-to-end test immediately.
  - **On-prem GA:** **self-hosted Maven repo bundled in the appliance** — customers pull behind their firewall.
  - **SaaS GA:** **Maven Central** under `com.motadata` (`mavenCentral()` default, no extra config).
- **Publish the full module closure** (`-core` transitive, `-rum`, `-okhttp`, + any internal `project(...)` deps) — else "could not find …-core" at the customer.
- **Central steps:** namespace → GPG (`GPG_PRIVATE_KEY`/`GPG_PASSWORD`, `MavenConfig.kt:94-95`) → token (`CENTRAL_PUBLISHER_USERNAME/_PASSWORD`, root `build.gradle.kts:62-63`) → apply Part 1D → `./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository`.

> Browser shipped to npm `@motadata365` (`b2572c5`); Maven Central + self-hosted repo is the Android analog.

---

# PART 5 — Master quick-reference (file:line)

| Goal | File:line | Change |
|---|---|---|
| Package | whole repo | `com.datadog.android` → `com.motadata.android` |
| Class `Datadog` | `Datadog.kt` | → `Motadata` |
| Class interceptor/listener/site | okhttp, core | → `Motadata*` |
| **Launch view url** | `RumViewManagerScope.kt:461` | → `com/motadata/application-launch/view` |
| **Launch view id** | `RumViewManagerScope.kt:460` | → `com.motadata.application-launch.view` |
| **Telemetry service** | `TelemetryEventHandler.kt:601` | → `motadata-rum-android` |
| Flags/meter SDK name | `PrecomputedAssignmentsRequestFactory.kt:147`, `SdkFeature.kt:471`, `BenchmarkUploads.kt:76` | → `motadata-rum-android` |
| API-key header | `RequestFactory.kt:48` | → `MD-API-KEY` |
| EVP/request/idempotency headers | `RequestFactory.kt:53,63,78` | → `MD-*` |
| `ddsource`/`ddtags` query | `RequestFactory.kt:68,73` | → `mdsource`/`mdtags` |
| Logcat tags | `SdkInternalLogger.kt:286,287` | → `MD_LOG` / `Motadata` |
| **`md-api-key` query (auth)** | `RumRequestFactory.kt:57-69` | add `put("md-api-key", context.clientToken)` |
| cleartext http public | `Configuration.kt:288` | drop `internal` |
| **`is_view_completed`** | `RumViewScope.kt:1117-1124` | serialize existing `viewComplete` as `view.is_view_completed` |
| **`session.created`** | `RumSessionScope.kt:71` + `RumContext.kt` | capture session-start epoch-ms, emit on every event |
| **`context._timing`** | event assembly | emit `{ navigationStart, relativeTime: date - navigationStart }` |
| **`_dd`→`_md`, `ddtags`→`mdtags`** (body) | `rum-events-format` fork + regen | envelope rename |
| **Thread names** (`error.threads[]`) | `DatadogThreadFactory.kt:20`, `DatadogCore.kt:728`, `WorkManagerUtils.kt:23-24` | `datadog-*` → `motadata-*` |
| **NTP hosts** | `DatadogNtpEndpoint.kt:17-32` | `*.datadog.pool.ntp.org` → `pool.ntp.org` |
| User-Agent | `DataOkHttpUploader.kt:119` | `Datadog/$ver` → `Motadata/$ver` |
| Storage dir | `CoreFeature.kt:780` | `datadog-%s` → `motadata-%s` |
| Logcat messages | core/rum (many) | "Datadog…" → "Motadata…" |
| Init log | `Datadog.kt` | add `"Motadata SDK initialized"` |
| Telemetry | `RumFeature.kt:788` | **keep ON (20f) — no change**; scrub telemetry path (1B.3 + 2.6 + 1E.8) |
| Group id | `MavenConfig.kt:18` | → `com.motadata` |
| Version | `AndroidConfig.kt:22` | → `1.0.0` |
| POM | `MavenConfig.kt:58-89` | → Motadata |

---

# PART 6 — Build, verify & CI gate

1. Fork `dd-sdk-android`, freeze at 3.10.0. (Android keeps the RUM JSON schemas as committed files in-repo at `features/dd-sdk-android-rum/src/main/json/` — **no submodule**, so `_dd`→`_md` is edited here directly.)
2. Apply **Part 1** (1A–1E scrub, incl. in-event strings, thread names, NTP, User-Agent, storage dir, Logcat msgs) + **Part 2** (2.1 md-api-key · 2.2 cleartext · 2.3 is_view_completed · 2.4 keep-tracking · 2.5 session.created + context._timing · 2.6 `_dd`→`_md`) + **Part 3** (log tag/message).
3. `./gradlew apiDumpAll generateApiSurfaceAll` (CI fails on stale API surface after the rename).
4. `./gradlew assembleLibrariesDebug unitTestDebug` — build + tests green.
5. Publish to a pilot channel; run the onboarding snippet against your endpoint.
6. **Verify end-to-end:** event POSTs to `…/api/v2/rum?md-api-key=…` (gzip) · Logcat shows `Motadata` · launch view url is `com/motadata/application-launch/view` · `telemetry.service` is `motadata-rum-android` · one row/view via `is_view_completed=yes`.
7. **CI "no-datadog" gate:** fail the build if `datadog`, `dd-sdk`, or `DD-` appears in any shipped artifact —
   **whitelisting only** the Apache-2.0 attribution header (`This product includes software developed at Datadog` / `Copyright … Datadog, Inc.`).
8. Freeze the fork; cherry-pick upstream security patches manually (no rebase).

## What's already correct — do NOT touch
- Custom endpoint plumbing (URL used as-is, `RumRequestFactory.kt:67`) · gzip on in release (`CoreFeature.kt:641`) · NDJSON `"\n"` (`RumRequestFactory.kt:138`) · `isViewComplete()` already computes the final-emit state (`RumViewScope.kt:1594`) · `source` value is `"android"` (no datadog).

## Honest notes
- **The only "Datadog" that legitimately remains** is the Apache-2.0 file-header attribution (legal; source-only; never in events/wire). Everything customer- or server-visible is scrubbed by Part 1.
- **`telemetry.service`** (1B.3) and the **launch view url** (1B.1) are the two in-event strings most likely to slip through a "rename imports only" pass — they're plain string constants, not types.

---

# PART 7 — How to test locally (NO Maven publish until final)

You never publish to Maven Central to test. Three loops, fastest → most realistic. **Use 7.1 for Branch 1.**

## 7.1 Fast loop — in-repo sample app + Logcat (no server, no publish, no cleartext setup)
The repo ships a sample app (`:sample:kotlin`) that depends on the SDK via **project dependencies**, so it
builds against your **edited source** directly — nothing to publish. And because a source build has the
SDK's `BuildConfig.DEBUG = true`, the core adds a **`CurlInterceptor`** (`CoreFeature.kt:638`) that logs
**every upload request — URL, headers, and the uncompressed NDJSON body — to Logcat**. So you can read
exactly what the SDK would send **without any server**.

Steps:
1. After the rename, the sample's own code is renamed too (it's in the repo) → it uses `Motadata.initialize`.
2. `./gradlew :sample:kotlin:assembleUs1Debug` then run on an emulator (or `installUs1Debug`).
3. Tap around to generate views/actions/errors.
4. `adb logcat -s Motadata` (and look at the CurlInterceptor request dumps).
5. **Verify Branch 1 (rename) success** — in the logged request you should see **zero `datadog`/`dd-`**:
   - URL query: `mdsource=android` (not `ddsource`)
   - Headers: `MD-API-KEY`, `MD-EVP-ORIGIN`, … (not `DD-*`)
   - Body NDJSON: `view.url` = `com/motadata/application-launch/view`, `telemetry.service` = `motadata-rum-android`, thread names `motadata-*`, envelope is **`_md`** (not `_dd`), tags field is `mdtags`
   - Logcat tag is `Motadata`
   - → after Branch 1, **no `datadog`/`dd-` appears anywhere** in the request (URL, headers, or body)

> The **`android-sdk-event-inspection`** skill automates exactly this (validating request bodies via Logcat
> with the sample Kotlin app). Use it to drive the verification.

## 7.2 Real-upload loop — local capture proxy
Point `useCustomEndpoint(...)` in the sample at a tiny local HTTP listener that captures the body (your
existing `rum-captures` proxy), then inspect the saved NDJSON.
- Emulator → host: use `http://10.0.2.2:<port>/api/v2/rum`.
- ⚠️ **Cleartext caveat for Branch 1:** plain `http://` needs `allowClearTextHttp()`, which is still
  `internal` until **Branch 2**. So on Branch 1 either (a) use an **HTTPS** local proxy with a trusted cert,
  or (b) just use 7.1 (Logcat) which needs no network. Plain-HTTP capture works once Branch 2 lands.

## 7.3 Customer-flow loop — `publishToMavenLocal` (do this before declaring final, still no Central)
Simulates the real customer exactly — resolving via Maven coordinates — but locally:
1. `./gradlew publishToMavenLocal` → publishes `com.motadata:motadata-rum-android:1.0.0` (+ `-core`, `-okhttp`) to `~/.m2`.
2. In a **separate** test app, add `mavenLocal()` to repositories and paste the **onboarding snippet** verbatim
   (`implementation("com.motadata:motadata-rum-android:1.0.0")`).
3. Build + run → confirms the Maven coords, POM, artifact closure, and the exact onboarding snippet all work.

## 7.4 The publish path (only when full & final)
`publishToMavenLocal` (7.3) → **pilot channel** (JitPack / GitHub Packages) for a real over-the-network test →
**Maven Central** (`com.motadata`) for GA. See Part 4.

## Per-branch "done" checks
- **Branch 1 done** = sample app compiles & runs · CurlInterceptor request shows **zero `datadog`/`dd-` anywhere** (URL, headers, **and body** — envelope is `_md`) (7.1) · `apiDumpAll` + `assembleLibrariesDebug unitTestDebug` green · CI no-datadog gate passes.
- **Branch 2 done** = all Branch 1 checks · request URL now has `?md-api-key=…` · view events carry `is_view_completed` · `session.created` + `context._timing` present · auth succeeds against a validating endpoint.

— end —
