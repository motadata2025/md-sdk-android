# Motadata Rebrand — Progress Tracker

**Living checklist of the rebrand.** Updated after every step. Source of truth for *what* each
step does = `MOTADATA_ANDROID_SDK_REBRAND_PLAN.md`. This file tracks *status only*.

- **Repo / branch:** `motadata2025/md-sdk-android`, branch `motadata-dev` (renames) → later cut
  `motadata-dev-with-functional-changes` (functional additions).
- **Build = CI only.** Each step = one (or few) commit → push → **`Motadata SDK Build` workflow must be green** (7 AARs) before the next step. Never build locally.
- **Legend:** ✅ done & CI-green · 🔄 in progress · ⬜ not started

---

## Branch 1 — `motadata-dev` (debranding / renames only)

| # | Step | Status | Commit(s) | CI run |
|---|------|--------|-----------|--------|
| 1 | Package rename `com.datadog.android` → `com.motadata.android` (dotted + slash + JNI-underscore; 88 dirs) | ✅ | `7feb71787`, `615aa7835` | 26868076588 ✅ |
| 2 | Class renames `Datadog`→`Motadata` (`DatadogInterceptor`, `DatadogEventListener`, `DatadogSite`, …) — plan §1A | ✅ | `a30d2fa17` | 26869349974 ✅ |
| 3 | In-event strings — plan §1B (launch view url/id, `telemetry.service`, flags/meter → `motadata-rum-android`) | ✅ | `f66b053e2` | 26870147865 ✅ |
| 4 | Thread names `datadog-*` → `motadata-*` — plan §1B.8 / §1E.2-4 | ✅ | `f94c6fd55` | 26874612559 ✅ |
| 5 | Wire names `DD-*`→`MD-*`, `ddsource`/`ddtags`→`mdsource`/`mdtags`, logcat tags — plan §1C | ✅ | `df3e9c0d9` | 26876753887 ✅ |
| 6 | Runtime leaks: NTP→`pool.ntp.org`, User-Agent, storage dir, logcat messages — plan §1E | ✅ | `df269823f` | 26877583518 ✅ |
| 6.5 | Internal class-name debrand (remaining `Datadog*`/`Dd*` internal classes → `Motadata*`/`Md*`) — pure rename, no behavior/API-shape change | ✅ | `61de63f52`,`236f36aeb` | 26879305308 ✅ |
| 7 | Body envelope `_dd`→`_md`, `ddtags`→`mdtags` via JSON-schema edit + codegen regen — plan §2.6 | ✅ | `4900580cb`,`95f8f68f4` | 26884001463 ✅ |
| 8 | Maven coords + POM: group `com.motadata`, version `1.0.0`, artifact ids — plan §1D | ✅ | `873882f8a` | 26885382569 ✅ |
| 9 | `apiDumpAll` + `generateApiSurfaceAll` + fix broken tests → full green — plan Part 6 | ⬜ | — | — |

**Then:** test Branch 1 end-to-end (deferred until step 9 done) — Path A (CI sample APK → `adb logcat`, CurlInterceptor dumps request) and/or Path B (CI → GitHub Packages → Windows app). No Maven Central needed for testing.

---

## ▶ STEP 9 — EXECUTION PLAN (start here next session)

Step 9 = **verify/finalize**, not new renames. Two phases (9A quick & predictable, 9B iterative & CI-bound). Everything runs on CI (never build locally). All commands target `-R motadata2025/md-sdk-android`, branch `motadata-dev`.

**Pre-flight:** `git -C <repo> pull` first (CI may have pushed regenerated files). Confirm steps 1–8 still green via this table.

### Phase 9A — Regenerate API surface (~20–30 min, 1 CI run)
*Why:* `api/apiSurface`, `api/<module>.api`, `api/compiler-meta.txt` for the 7 shipped modules are **stale** — still list old `Dd*`/`Datadog*`/`_dd` names from steps 6.5 & 7. Regenerate from the renamed code. (`motadata-build.yml` only does `assembleDebug`, so it never caught this — not a failure, just not checked.)
*Tasks (per module, no aggregate "generate" task exists):* `:m:generateApiSurface` (→`api/apiSurface`) + `:m:apiDump` (→`api/<module>.api`) + `:m:generateCompilerMetadata` (→`api/compiler-meta.txt`). The 7 modules: `:dd-sdk-android-core :dd-sdk-android-internal :features:dd-sdk-android-rum :features:dd-sdk-android-trace :features:dd-sdk-android-trace-api :features:dd-sdk-android-trace-internal :integrations:dd-sdk-android-okhttp`.
*Mechanism (can't build locally → CI must produce AND return the files):* add **`.github/workflows/motadata-apidump.yml`** (`on: workflow_dispatch`, **`permissions: contents: write`**) that: checkout (persist-credentials) → JDK21 + Android SDK + Gradle → run the generate/apiDump/compilerMetadata tasks for the 7 modules → `git add **/api/apiSurface **/api/*.api **/api/compiler-meta.txt` → commit `"chore: regenerate api surface [skip ci]"` → `git push`. Trigger: `gh workflow run motadata-apidump.yml -R motadata2025/md-sdk-android --ref motadata-dev`; poll; then `git pull`.
*Fallback if CI push is blocked (token/branch-protection):* same job but `actions/upload-artifact` the `**/api/**` files → `gh run download <id>` → copy into repo → commit + push locally.
*Note:* api **filenames** stay `dd-sdk-android-*.api` (derived from gradle module names, which we did NOT rename — only artifact ids changed). That's correct; they're internal repo files, never published.

### Phase 9B — Run unit tests + fix to green (iterative, ~1–3 h, CI-bound)
*Mechanism:* add a **`unit-test` job** (new `.github/workflows/motadata-test.yml`, `on: workflow_dispatch`) running:
`./gradlew :dd-sdk-android-core:testDebugUnitTest :dd-sdk-android-internal:testDebugUnitTest :features:dd-sdk-android-rum:testDebugUnitTest :features:dd-sdk-android-trace:testDebugUnitTest :features:dd-sdk-android-trace-api:testDebugUnitTest :features:dd-sdk-android-trace-internal:testDebugUnitTest :integrations:dd-sdk-android-okhttp:testDebugUnitTest --continue`
with `if: always()` → `actions/upload-artifact` of `**/build/test-results/**/*.xml` + `**/build/reports/tests/**`. `--continue` surfaces ALL failures per run (minimizes iterations).
*Loop:* `gh workflow run motadata-test.yml` → poll → `gh run download <id>` → `grep -rl '<failure' **/test-results` / read report → fix → commit → re-run. Repeat to green.
*Likely failure buckets to expect (most are mechanical "update expected value"):*
  1. **Serializer/forge tests** asserting `_dd`/`ddtags` → `_md`/`mdtags` (partially done in step 7; resource/test-fixture JSON under `src/test/resources/*.json` NOT yet touched — likely the biggest bucket).
  2. **Version** assertions `3.10.0` → `1.0.0` (step 8 changed `AndroidConfig.VERSION`).
  3. **String constants** tests assert: telemetry.service/meter `motadata-rum-android`, launch/background view url/id `com/motadata/...`, `MD-*` headers, `mdsource`/`mdtags`, `MD_LOG`, NTP `pool.ntp.org`, storage `motadata-%s`, thread names `motadata-*`, op names `MotadataCore.*`.
  4. **Class/Forge references** to renamed types (should compile, but fixtures may hardcode names).
  5. **Binary fixture** `dd-sdk-android-core/src/test/resources/logs-batch-2.2.0-and-earlier` (backward-compat deserialization) — left untouched in step 1; if a test reads it expecting `com.datadog.android`, decide: keep (it tests OLD-format compat) vs regenerate.

### Phase 9C — Done criteria
All 7 modules: `testDebugUnitTest` green + `assembleDebug` green (7 AARs) + `checkApiSurfaceChangesAll` clean (api matches code). Then mark step 9 ✅ → **Branch 1 COMPLETE** → proceed to end-to-end device test (Path A/B), then cut `motadata-dev-with-functional-changes` for Branch 2.

*Decision for next session:* whether to fold the test job permanently into `motadata-build.yml` (every push runs tests) or keep it a separate `workflow_dispatch`. Recommend: separate dispatch during step 9 (faster iteration), fold in once green.

## Branch 2 — `motadata-dev-with-functional-changes` (functional additions only, cut after Branch 1)

| # | Step | Status |
|---|------|--------|
| 10 | `md-api-key` query param — plan §2.x | ⬜ |
| 11 | Public `allowClearTextHttp()` — `Configuration.kt:288` | ⬜ |
| 12 | `is_view_completed` — `RumViewScope.kt` | ⬜ |
| 13 | `session.created` + `context._timing` — `RumSessionScope.kt` / `RumContext.kt` | ⬜ |
| 14 | Keep-tracking delay tune | ⬜ |
| 15 | "Motadata SDK initialized" init log | ⬜ |

---

## Notes & decisions (running log)

- **Scope rule:** rename **only** the `com.datadog.android` namespace. Leave `com.datadog.trace` / `.tools` / `.gradle` / `.benchmark` (and `.sample`/`.opentelemetry`/etc.) — those are separate roots, not product-facing in the same way.
- **`motadata-rebrand/` docs keep the old `com.datadog.*` names on purpose** (they describe the before→after migration). The rename pass reverts any accidental edits to them.
- **Deferred on purpose (do NOT treat as missed):**
  - `group = "datadog"` Gradle task-group labels in `buildSrc` (~10×) — cosmetic, internal-only, not shipped.
  - `MavenConfig.kt` `GROUP_ID = "com.datadoghq"` and AAR/artifact filenames `dd-sdk-android-*` → **step 8**.
- **Telemetry stays ON** (server ignores it). Ship core + rum + okhttp closure (7 AARs); not session-replay.

### Step-8 detail (for the record)
- `MavenConfig.kt`: GROUP_ID `com.datadoghq`→`com.motadata`; POM `name`→artifactId (was module dir name `dd-sdk-android-*`); url/scm→`github.com/motadata2025/md-sdk-android`; org/dev url→`motadata.com`; email→`info@motadata.com`. `AndroidConfig.kt` VERSION 3.10.0→1.0.0.
- Artifact ids (Option A, matches onboarding): rum=`motadata-rum-android`, okhttp=`motadata-rum-android-okhttp`, core/internal/trace/trace-api/trace-internal=`motadata-rum-android-<suffix>` (set via `publishingConfig(customArtifactId=…)`).
- **Left:** internal build flag `dd-skip-signing` (not published, not customer-facing). Email/urls are reasonable placeholders — tweak before publish if needed. Non-shipped modules keep default artifact id (= module name) under com.motadata group, but they're not published.

### Step-7 detail (for the record)
- Edited 9 shipped JSON schemas (rum/* 7 + telemetry/_common + trace/span) `"_dd"`→`"_md"`, `"ddtags"`→`"mdtags"`. json2kotlin regenerates property `dd`→`md`, class `.Dd`→`.Md`, `DdSession`→`MdSession`, `@SerializedName("_md")` on the wire. Updated 38 rum/trace .kt: `.Dd`→`.Md`, `DdSession`→`MdSession`, `.dd`→`.md`, `dd =`→`md =`, `ddtags`→`mdtags`, `buildDDTagsString`→`buildMdTagsString`, serializer-test `"_dd"`→`"_md"`. Trace mapper `dd = dd`→`md = md`.
- **2nd commit fix:** implicit-receiver `dd.configuration` (no leading dot) in `MotadataLateCrashReporter.sampleRate` → `md.configuration` (dot-anchored regex missed it).
- **Anchored regexes correctly LEFT:** `dd-sdk`/`dd-trace` in comments + GitHub URLs; `dd=p:%s;s:0` W3C tracestate string (`MotadataPropagationHelper` — that's the future trace-correlation `dd=` item); trace meta keys `_dd.p.id`/`_dd.agent_psr`/`_dd.span_links`/`_dd.datadog_initial_context` (trace-internal APM keys, separate scope).
- **Step-9 TODO:** regenerate `api/apiSurface` + `api/*.api` (still list `DdSession`/`DdAction`/`DdActionTarget`/`DdCls` — these generated model classes only live in api files). **Deferred (unshipped modules):** logs/session-replay/webview/benchmark schemas still have `_dd`/`ddtags`.

### Step-6.5 detail (for the record)
- Renamed all 106 `Datadog*`/`Dd*` classes DEFINED in `com.motadata.*` → `Motadata*`/`Md*` (+ 4 extension files + manifest `MdRumContentProvider`). 110 files moved. No cross-namespace collisions.
- **Gotcha (2nd commit):** word-boundary rename skipped identifiers where Datadog/Dd is embedded after a prefix. Build broke on `@NoOpImplementation` codegen: generator emits `NoOpMotadata*` from renamed interfaces, but hand-written refs still said `NoOpDatadog*`. Fixed: `NoOpDatadogPropagation`(hand-written)/`NoOpDatadogTracer`/`NoOpDatadogTracerBuilder` → `NoOpMotadata*`, `GlobalDatadogTracer`→`GlobalMotadataTracer`, `UnsupportedDatadogSpanContextImplementation`→`UnsupportedMotadata…`, test `NonDdTracer`→`NonMdTracer`.
- **LEFT (deliberate, user-agreed — not customer/server-facing, outside scrub scope):** camelCase var/method names — `fakeDatadogContext`/`mockDatadogContext` (test-only, never shipped) and `getDatadogContext()` on `InternalSdkCore` (internal cross-module API, 544 refs, only visible in decompiled bytecode). Renaming = unnecessary churn.
- **Still out of scope (separate namespaces):** `com.datadog.trace`/`tools`/`benchmark` vendored packages + their classes (`DatadogHttpCodec`, `DatadogBaseMeter`…); session-replay `androidx` accessor. Generated `Dd*` `_dd` models (`DdSession`/`DdAction`/`DdDevice`) → renamed by **step 7**.

### Step-5 detail (for the record)
- RequestFactory.kt: `DD-API-KEY`/`DD-EVP-ORIGIN`/`DD-REQUEST-ID`/`DD-IDEMPOTENCY-KEY`→`MD-*`; query `ddsource`→`mdsource`, `ddtags`→`mdtags`. SdkInternalLogger `SDK_LOG_TAG` `DD_LOG`→`MD_LOG` (`DEV_LOG_TAG` already `Motadata` from step-2 bare-word pass). SKILL.md logcat tag updated to match.
- **Decision (agreed w/ user): backend reads `mdsource`/`MD-*`** (mirrors browser-sdk fork) → safe to rename.
- **Left untouched by design:** `x-datadog-*` trace-propagation headers (TracingInterceptor/DatadogHttpCodec) — user uses **W3C/OTel `traceparent`** for any future RUM-trace correlation, so Datadog-format headers stay dormant; benchmark uploader `DD-API-KEY` (`com.datadog.benchmark` → Datadog's own benchmark intake, not shipped); event-**body** `ddtags` in `log-schema.json`/RUM `_common-schema.json`/webview = **step 7** (`_dd`/`_md` + body `ddtags`→`mdtags`). Internal const *identifier* `DD_IDEMPOTENCY_KEY` left (value is `MD-…`; name not serialized). Future: tiny `dd=` entry in W3C `tracestate` to scrub when trace correlation is turned on.

### Step-4 detail (for the record)
- Thread names: `datadog-*-thread-*`→`motadata-*` (DatadogThreadFactory, guarded on `-thread-` so storage `datadog-%s` untouched); `datadog_shutdown`→`motadata_shutdown` (DatadogCore). WorkManager: `DatadogUploadWorker`→`MotadataUploadWorker`, `DatadogBackgroundUpload`→`MotadataBackgroundUpload`. Coupled `DatadogThreadFactoryTest` assertions updated.
- **Folded in (was a step-3/§1B miss):** background-view in-event strings `com.datadog.background.view`→`com.motadata…` + view.url `com/datadog/background/view`→`com/motadata…` (RumViewManagerScope:456-457). All §1B in-event view seeds now scrubbed (launch + background).
- **Deferred (logged):** trace `dd-agent-startup-datadog-tracer/-profiler` thread names (AgentThreadFactory, `com.datadog.trace` namespace = trace internals); storage dir `datadog-%s` + reliability regex `datadog-(.*)` → **step 6**; internal const *identifiers* (`TAG_DATADOG_UPLOAD`, `DATADOG_STORAGE_DIR_NAME`, `DATADOG_*_HEADER`) — not serialized, left.

### Step-2 detail (for the record)
- Renamed the 7 §1A public types **and their whole families** (tests/factories/extensions/providers) repo-wide: `Datadog`→`Motadata` (object), `DatadogInterceptor`, `DatadogEventListener`, `DatadogSite`, `DatadogContext`(+`Provider`/`Storage`/`Wrapper`), `DatadogDatabaseErrorHandler`, `DatadogDataConstraints`. 26 files renamed.
- Bare `Datadog` rename **skips the legal Apache-header lines** (`developed at Datadog`, `Datadog, Inc.`) — those stay (5618 mentions). Verified 0 non-legal bare `Datadog` left.
- **Deferred `Datadog*` families** (NOT §1A — later/optional, only if shipped): `DatadogCore`, `DatadogExceptionHandler`, `DatadogConfig`, `DatadogContentProvider`, `DatadogFeaturesInitializer`, `DatadogAccountInfoProvider` (core internals); `DatadogSpan*`/`DatadogTracer*`/`DatadogScope`/`DatadogPropagation*`/`DatadogHttpCodec`/`DatadogTraceId*` (trace); `DatadogNdkCrashHandler`/`DatadogLateCrashReporter`/`DatadogLogGenerator`/`DatadogLogHandler`/`DatadogRumMonitor`/`DatadogGesturesTracker` (rum/core internals); integration classes (`DatadogGlideModule`, `DatadogCronetEngine`, `DatadogCoilRequestListener`, `DatadogApolloInterceptor`, `DatadogFrescoCacheListener`, `DatadogTree`, `DatadogFlagsClient`, `DatadogEventBridge`, …). These don't appear in customer-typed API or event payloads; revisit when scrubbing internal-class leakage / when shipping a given integration.

### Step-1 detail (for the record)
- Renamed all 3 textual forms; `git mv` of 88 package directories.
- Build-breaker found: `buildSrc/.../apisurface/GenerateCompilerMetaTask.kt:45` located a sample class via `path.contains("datadog")` → classes now under `com/motadata/`, so it matched nothing. Fixed `"datadog"` → `"motadata"`.
- Result: zero `com.datadog.android` in product; other roots intact; all 7 AARs built.
