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
| 5 | Wire names `DD-*`→`MD-*`, `ddsource`/`ddtags`→`mdsource`/`mdtags`, logcat tags — plan §1C | ⬜ | — | — |
| 6 | Runtime leaks: NTP→`pool.ntp.org`, User-Agent, storage dir, logcat messages — plan §1E | ⬜ | — | — |
| 7 | Body envelope `_dd`→`_md`, `ddtags`→`mdtags` via JSON-schema edit + codegen regen — plan §2.6 | ⬜ | — | — |
| 8 | Maven coords + POM: group `com.motadata`, version `1.0.0`, artifact ids — plan §1D | ⬜ | — | — |
| 9 | `apiDumpAll` + `generateApiSurfaceAll` + fix broken tests → full green — plan Part 6 | ⬜ | — | — |

**Then:** test Branch 1 end-to-end (deferred until step 9 done) — Path A (CI sample APK → `adb logcat`, CurlInterceptor dumps request) and/or Path B (CI → GitHub Packages → Windows app). No Maven Central needed for testing.

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
