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
| 9 | API regen + unit tests green — plan Part 6 | ✅ | `c536447b0`(api),`3eb9662ff`(jdk17+test fixes) | 9A ✅ · 9B 26934870396 ✅ (5469 tests, 0 fail) |
| 9A | Regenerate api surface (`generateApiSurface`+`apiDump`+`generateCompilerMetadata` ×7) via new `motadata-apidump.yml` dispatch wf; bot pushed regenerated files | ✅ | `c536447b0` | 26932653185 ✅ |
| 9B | Unit tests (`testDebugUnitTest --continue` ×7) via `motadata-test.yml` on **JDK 17**; fixed 2 stale assertions | ✅ | `c4bd305c7`,`3eb9662ff` | 26932981053 ❌→ 26934870396 ✅ |

**Then:** test Branch 1 end-to-end (deferred until step 9 done) — Path A (CI sample APK → `adb logcat`, CurlInterceptor dumps request) and/or Path B (CI → GitHub Packages → Windows app). No Maven Central needed for testing.

**Branch 1 = SIGNED OFF** (v`1.0.0`, published to GitHub Packages, verified on-device incl. the `MD-EVP-ORIGIN-VERSION` header fix).

---

## Branch 2 — `motadata-dev-with-functional-changes` (functional additions only — NO renames)

Cut from `motadata-dev` @ `a9da12805` (Branch-1 signed-off checkpoint). **Ships as `1.0.1`** (DECIDED — a version bump avoids the GitHub-Packages delete-then-republish dance; the `AndroidConfig.VERSION` bump is itself one of the publish-time steps). Same loop as Branch 1: edit → push → `Motadata SDK Build` green → (api regen / test dispatch as needed) → pause for user "go" before next step.

| # | Step | Plan § | Status | Commit(s) | CI run |
|---|------|--------|--------|-----------|--------|
| 10 | **`md-api-key` query param** (auth) — `RumRequestFactory.buildUrl` adds `md-api-key=<clientToken>` (new `RequestFactory.QUERY_PARAM_API_KEY`) | 2.1 | ✅ | `7617e01d5` (code), `6b75c4d16` (ci) | build 27008283960 ✅ · tests 27010278286 ✅ (JDK17, 0 fail) |
| 11 | Make `allowClearTextHttp()` **public** (drop `internal`) — replaces the `_InternalProxy` hack | 2.2 | ✅ | `b5baaeee4` | build 27012894816 ✅ |
| 12 | **`view.is_view_completed`** — serialize existing `viewComplete` in `RumViewScope.sendViewUpdate` | 2.3 | ✅ | `d15b204cd` | build 27118443006 ✅ · tests 27119584355 ✅ (JDK17, 0 fail) |
| 13 | **`session.created` + `context._timing`** — capture session-start epoch-ms in `RumSessionScope`/`RumContext`, emit on every event | 2.5 | ✅ | `405b361d8` (impl), `209a2f00e` (tests) | build 27123075809 ✅ · tests 27124204130 ✅ (JDK17, 0 fail) |
| 14 | Keep-tracking delay tune (5min → ~1min) | 2.4 | ⬜ | | |
| 15 | Add `"Motadata SDK initialized"` log at end of `Motadata.initialize()` | 3.2 | ⬜ | | |

**Then:** api-surface regen (step 11 changes public API; step 10 adds a public core const) → unit tests green (JDK 17) → bump `AndroidConfig.VERSION` to `1.0.1` → publish 7 modules to GitHub Packages → re-capture on device, confirm `?md-api-key=…`, `is_view_completed`, `session.created`, `context._timing`.

### Step-13 detail (for the record)
- **Fields added to every event** (5 backend-consumed types: view, action, resource, error, long_task):
  - `session.created` = **epoch ms** when the session started (inside the `session` object).
  - `context._timing` = `{ navigationStart: <session-start ms>, relativeTime: <NANOSECONDS since session start> }` (inside `context`).
- **⚠️ Units (backend-driven, corrects the plan):** `RUMEventProcessor.java` reads `session.created` via `getLong` → stored `.ms`; reads `_timing.relativeTime` via `convertTime(NANOSECONDS, MICROSECONDS)` → stored `.us`. So `relativeTime` is **nanoseconds**, NOT the plan's `event.date − navigationStart` ms. `navigationStart` is **not read** by the backend (parity filler) → Option A: `navigationStart == session.created`.
- **Capture/propagation:** `RumSessionScope.renewSession` stores server-corrected `sessionStartTimestampMs = time.timestamp + serverTimeOffsetMs`; exposed via new `RumContext.sessionStartTimestampMs` (toMap/fromFeatureContext) + helper `RumContext.buildTimingContext(eventMs, sessionStartMs)` (relativeTime = `(eventMs − sessionStartMs) × 1_000_000`).
- **`created` field:** added optional `created` (integer) to the **shared** `_common-schema.json` session → all 5 `*EventSession` models get it via `allOf` merge (build confirmed). `_timing` needs **no schema** (context is `additionalProperties: true`) — injected into each event's context map; nested map serializes via `JsonSerializer.toJsonElement` (handles `Map`).
- **7 injection sites:** ViewEvent/ErrorEvent/LongTaskEvent (RumViewScope), ActionEvent (RumActionScope), ResourceEvent + ErrorEvent (RumResourceScope), late-crash ErrorEvent (`MotadataLateCrashReporter` — carries `created` from persisted view, recomputes `_timing` for the error's date). `updateViewEvent` `.copy()` preserves them. Android-specific Vital* events intentionally left (no browser analog, backend doesn't consume).
- **Tests:** the new `_timing` key broke `containsExactlyContextAttributes` (exact map match) in 5 assert helpers → 144 failures. Fix: exclude the `_timing` key there (asserts only user context). Serializer/deserializer round-trip passed unchanged (created + nested _timing serialize fine). Actual values to be verified on-device at finalize. NOTE: 3 transient runner Gradle-distribution download failures (504 / read-timeout on `gradle-9.4.0-all.zip`) along the way — not code; re-runs cleared them.

### Step-12 detail (for the record)
- **Placement = schema field (option a), for exact browser parity.** Browser SDK (`viewCollection.ts:122` + `trackViews.ts:61,332,359`) emits `view.is_view_completed` as a **string** `"no"`→`"yes"` (typed `isViewCompleted: string`, a custom Pratham/Ashish add marked `// NEW: Mark as final`). So Android adds the same field to the **view object**, not the event top level.
- **Schema:** added optional string `is_view_completed` to `_view-properties-schema.json` under `properties.view.properties` (right after `is_active`) → json2kotlin regenerates `ViewEvent.ViewEventView.isViewCompleted: String?` at build (generated model is build-time only, never committed — per CLAUDE.md).
- **Serialization:** `RumViewScope.sendViewUpdate` sets `isViewCompleted = if (viewComplete) "yes" else "no"` next to the existing `isActive = !viewComplete` (line ~1261). Reuses the already-computed `viewComplete = isViewComplete()` (true only when view stopped AND all pending resources/actions/long-tasks resolved) — no lifecycle rebuild.
- **No test changes needed:** optional model field round-trips fine; the builder-style `ViewEventAssert` DSL only checks explicitly-asserted fields. Build (codegen+main) green, then JDK-17 tests 0-fail. (First test run hit a transient Gradle-distribution download timeout on the runner — re-dispatched, green.)
- Result on the wire: every view event now carries `view.is_view_completed` = `"no"` until the final authoritative update, then `"yes"` — byte-for-byte the browser shape; backend keeps one row per view.

### Step-11 detail (for the record)
- `Configuration.Builder.allowClearTextHttp()` (`Configuration.kt:288`): dropped `internal` → public, added customer-facing KDoc (on-prem `http://` endpoint; app must also permit cleartext via manifest/network-security-config). Resolves the in-code `TODO RUM-368 Expose it as public API?`.
- `_InternalProxy.allowClearTextHttp(builder)` bridge **kept** (updated comment) — flutter (`DatadogSdkPlugin.kt`), react-native (`DdSdkNativeInitialization.kt`), and integration tests (`reliability/core-it`, `instrumented/integration`) call through it. Now just delegates to the public method.
- No test change: existing `ConfigurationBuilderTest:453` already calls `.allowClearTextHttp()` (was same-module-accessible as internal; still compiles as public). Visibility-widening can't break callers, so build-green is a sufficient gate (tests batched at finalize).
- **Last public-API change in Branch 2** → the finalize api-surface regen captures this method + step-10's `QUERY_PARAM_API_KEY` const. After Branch 2, the client SOP's HTTP step becomes a plain `.allowClearTextHttp()` (no `@SuppressLint`/`_InternalProxy`).

### Step-10 detail (for the record)
- **Why query, not header:** backend `origin/dev` `RUMEventListener.java:218` authenticates on `params.get("md-api-key")` — Vert.x `request().params()` = **query string**, not headers. Missing/invalid → `401 LOGIN_TOKEN_COMPROMISED`. The `MD-API-KEY` *header* (renamed in Branch-1 step 5) is **never read for auth**.
- **Browser parity confirmed:** upstream DataDog `browser-sdk` `endpointBuilder.ts:96` already puts `dd-api-key=${clientToken}` in the **query** by default; the motadata fork (`motadata-dev` :86) just renamed it `md-api-key`. So browser always did query; Android natively only did the header → this is a genuine **ADD** on Android (hence Branch 2, not a Branch-1 rename).
- **Change:** `RequestFactory.QUERY_PARAM_API_KEY = "md-api-key"` const (core, next to `QUERY_PARAM_SOURCE`/`_TAGS`) + `put(QUERY_PARAM_API_KEY, context.clientToken)` in `RumRequestFactory.buildUrl` (insertion order: source, api-key, tags). Header still sent (harmless; backend ignores). Test `RumRequestFactoryTest.expectedUrl` updated to match the new query order.
- **API surface:** new public const → core `apiSurface`/`.api` go stale → regen via `motadata-apidump.yml` dispatch (build wf only does `assembleDebug`, doesn't catch it). **Deferred to Branch-2 finalize** (batched with step 11's public-method change; no CI gate fails meanwhile).
- **Workflow fix (this step):** `motadata-test.yml` + `motadata-apidump.yml` hardcoded `ref: motadata-dev` → made branch-agnostic via `${{ github.ref_name }}` (commit `6b75c4d16`). Confirmed `workflow_dispatch` runs the **`--ref` branch's** copy (Branch-1 9B ran JDK-17 tests though develop's copy is JDK-21), so no develop-side change needed. Tests dispatched with `--ref motadata-dev-with-functional-changes` correctly checked out this branch.

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

### Phase 9A — DONE (for the record)
- Authored `.github/workflows/motadata-apidump.yml` (`workflow_dispatch`, `permissions: contents: write`): checks out `motadata-dev`, runs `generateApiSurface`+`apiDump`+`generateCompilerMetadata` for the 7 modules, commits + pushes regenerated api files back. Run 26932653185 ✅ (6m19s) → bot commit `c536447b0` (899↔899, pure renames).
- **Gotcha:** `gh workflow run <file>` resolves the workflow by name **only on the repo default branch** (`develop`). Our wf lived only on `motadata-dev` → HTTP 404. Fix: also committed the (dispatch-only, never auto-runs) wf file to `develop` so it registers; dispatch with `--ref motadata-dev` then uses the motadata-dev copy + explicit `ref: motadata-dev` checkout. Same pattern used for `motadata-test.yml`.
- **Result:** product namespace clean — zero `com.motadata.android` `Dd*`/`_dd`/`datadog` left in api files; rum models now `MdSession`/`MdAction`/`MdActionTarget`/`MdCls`; `DdRumContentProvider`→`MdRumContentProvider`. **Remaining `datadog` in api = deferred separate roots only:** `com/datadog/trace` (1422, vendored OTel tracer internals, in trace-module public surface), `com/datadog/exec` (8), `com/datadog/tools` (2, `@NoOpImplementation` annotation). All agreed-deferred.

### Phase 9B — DONE (for the record)
- Added `.github/workflows/motadata-test.yml` (`workflow_dispatch`, registered on `develop` like the apidump wf) running `testDebugUnitTest --continue` for the 7 modules + uploads JUnit XML.
- **Run 1 (26932981053, JDK 21) ❌:** 279 failures, but **274 were a single infra wall** — `com.datadog.tools.unit.RemoveFinalModifier` strips a field's `final` via a `VarHandle` on `Field.modifiers`, which throws `UnsupportedOperationException: set` on JDK 21. **Fix: run the test job on JDK 17** (upstream targets `JvmTarget.JVM_17`; build/apidump stay on 21 — compile-only). This also cleared 2 JDK-map-ordering failures in the deferred `com.datadog.trace` `W3CHttpCodecTest`/`DatadogHttpCodecTest` (baggage tag order; not a rebrand issue).
- **2 genuine stale-test fixes (the only rebrand-coupled failures):**
  - `RumRequestFactoryTest.kt:169` expected URL `?ddsource=` → `?mdsource=` (missed in step 5; sibling `&mdtags=` was already correct).
  - `DeserializedViewEventAssert.kt:24` recursive-compare `ignoringFields(... "dd.configuration")` → `"md.configuration"` (missed in step 7; the field renamed but the ignore-path string didn't, which un-ignored it and exposed a pre-existing Float-vs-Double quirk that `assertConfigurationEquals` already handles).
- **Run 2 (26934870396, JDK 17) ✅:** **5469 tests, 0 failures, 0 errors, 0 skipped** across 311 classes.

### Phase 9C — Done criteria
All 7 modules: `testDebugUnitTest` green + `assembleDebug` green (7 AARs) + `checkApiSurfaceChangesAll` clean (api matches code). Then mark step 9 ✅ → **Branch 1 COMPLETE** → proceed to end-to-end device test (Path A/B), then cut `motadata-dev-with-functional-changes` for Branch 2.

**✅ MET — BRANCH 1 COMPLETE (2026-06-04):**
- `testDebugUnitTest` ✅ — run 26934870396, **5469 tests, 0 failures/errors/skipped** (JDK 17).
- `assembleDebug` ✅ — run **26935349788** on final HEAD `3eb9662ff`, **all 7 AARs** produced.
- api surface ✅ — regenerated from code in 9A (run 26932653185 → bot commit `c536447b0`); self-consistent so `checkApiSurfaceChangesAll` is clean by construction.
- **Final state:** branch `motadata-dev` @ `3eb9662ff`, clean, everything committed/pushed. All `datadog`/`dd`/`DD-` scrubbed from product namespace (`com.motadata.android`), event payloads (`_md`/`mdtags`/`mdsource`), wire headers (`MD-*`), Maven coords (`com.motadata:motadata-rum-android:1.0.0`). Only agreed-deferred separate roots remain (`com.datadog.trace`/`tools`/`exec`, the legal Apache header, dormant `.datadoghq.com` site hosts).

*CI note:* unit tests are a separate `workflow_dispatch` (`motadata-test.yml`, JDK 17). **Decision for later:** fold the test job into `motadata-build.yml` (note the JDK split: build=21, test=17) so every push runs tests — or keep separate. Not required for Branch 1.

### NEXT (new session): end-to-end device test, then Branch 2
1. **Device test** Branch 1 — Path A (CI builds sample APK → install on device → `adb logcat -s Motadata`, CurlInterceptor dumps the request; confirm `mdsource`/`MD-*`/`_md` on the wire, hitting Motadata's custom endpoint) and/or Path B (publish to GitHub Packages → Windows app pulls `com.motadata:motadata-rum-android:1.0.0`).
2. **Cut Branch 2** `motadata-dev-with-functional-changes` from `motadata-dev` → steps 10–15 (functional additions only).

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
