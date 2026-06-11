# Datadog Android RUM SDK — Captured Event Reference

> **Scope of this document:** This describes **only** the events that were actually captured in `rum-captures/` by the local proxy. It is a field-by-field reference: name → meaning → unit → example. Anything the SDK *could* emit but didn't land in these captures (e.g. `vital-operation-step`, `telemetry-configuration`) is intentionally left for later.
>
> **SDK:** `dd-sdk-android` **3.10.0**, endpoint `/api/v2/rum/`, payloads sent gzipped as NDJSON (one JSON object per line).
>
> **Source of truth for meaning/unit:** the SDK's own JSON schemas under `features/dd-sdk-android-rum/src/main/json/`. Where descriptions are quoted, they come from there.

---

## Table of Contents

1. [Capture folder layout](#1-capture-folder-layout)
2. [Event-type inventory](#2-event-type-inventory)
3. [Common envelope (shared by every RUM event)](#3-common-envelope-shared-by-every-rum-event)
4. [`view` event](#4-view-event)
5. [`action` event](#5-action-event)
6. [`long_task` event](#6-long_task-event)
7. [`resource` event](#7-resource-event)
8. [`error` event](#8-error-event)
9. [`vital` (app_launch) event](#9-vital-app_launch-event)
10. [`telemetry` event](#10-telemetry-event)
11. [SDK-internal `_dd.*` block](#11-sdk-internal-_dd-block)
12. [What the SDK does NOT put in the payload](#12-what-the-sdk-does-not-put-in-the-payload)

---

## 1. Capture folder layout

Each subfolder under `rum-captures/` represents **one HTTP batch** posted by the SDK to `/api/v2/rum/`. The SDK groups events into batches by time/size and gzips them.

```
20260521-202142-010/
├── meta.json              # request headers + transport metadata
├── raw-batch.ndjson       # the unmodified payload as the SDK sent it (one JSON per line)
└── event-NN-<type>.json   # each event split out, pretty-printed, in order
```

`meta.json` example:
```json
{
  "captured_at": "2026-05-21T14:51:42.703502016Z",
  "sequence": 10,
  "method": "POST",
  "path": "/api/v2/rum/",
  "query": "ddsource=android",
  "client_ip": "172.17.138.226",
  "compressed_bytes": 1818,
  "decompressed_bytes": 12802,
  "event_count": 9,
  "headers": {
    "DD-API-KEY": "...",
    "DD-EVP-ORIGIN": "android",
    "DD-EVP-ORIGIN-VERSION": "3.10.0",
    "DD-REQUEST-ID": "407892ee-be64-4468-8ad7-737862afd35f",
    "DD-IDEMPOTENCY-KEY": "987a2621fa742f6d005717059a5d43c39b26d352",
    "User-Agent": "Dalvik/2.1.0 (Linux; U; Android 12; M2010J19CI Build/SKQ1.211202.001)",
    "Content-Encoding": "gzip",
    "Content-Type": "text/plain;charset=UTF-8"
  }
}
```

Notes on the request envelope:
- **`DD-EVP-ORIGIN` / `DD-EVP-ORIGIN-VERSION`** — tells the intake which SDK and version produced this batch.
- **`DD-IDEMPOTENCY-KEY`** — deterministic content hash; lets Datadog dedupe retried batches.
- **`DD-REQUEST-ID`** — per-attempt UUID.
- The `User-Agent` is the **Dalvik** UA, not a custom Datadog one — it identifies the Android runtime and the device that sent the batch.

---

## 2. Event-type inventory

The captures contain seven distinct `type` values (counts as of the most recent capture session):

| `type` value          | File suffix          | Schema (in repo)                              | What it represents |
|-----------------------|----------------------|------------------------------------------------|--------------------|
| `view`                | `*-view.json`        | `rum/view-schema.json`                         | A screen / activity / fragment session with aggregate counters and perf metrics |
| `action`              | `*-action.json`      | `rum/action-schema.json`                       | One user interaction (tap, swipe, scroll, custom) |
| `long_task`           | `*-long_task.json`   | `rum/long_task-schema.json`                    | A main-thread block > 100 ms (configurable) |
| `resource`            | `*-resource.json`    | `rum/resource-schema.json`                     | One outgoing network request (URL, status, duration, size) captured via `DatadogInterceptor` |
| `error`               | `*-error.json`       | `rum/error-schema.json`                        | A reported error / exception / crash |
| `vital` (app_launch)  | `*-vital-app_launch.json` | `rum/vital-app-launch-schema.json`        | A startup-timing metric (TTID/TTFD) |
| `telemetry`           | `*-telemetry.json`   | `rum/telemetry/*.json`                         | SDK self-diagnostics emitted by the SDK itself |

A single batch can mix any of these — they all hit the same `/api/v2/rum/` endpoint.

---

## 3. Common envelope (shared by RUM events, with caveats)

These fields are defined in `rum/_common-schema.json` and form the shared shell of a RUM event. **Several are conditional, not mandatory.** Use the presence matrix below before assuming a field is always there.

### 3.0 Presence matrix (verified against your captures)

`✓` = present in every captured event of this type · `✗` = never present · `n/m` = present in some but not all

| Field                              | view | action | long_task | resource | error | vital | telemetry |
|------------------------------------|:----:|:------:|:---------:|:--------:|:-----:|:-----:|:---------:|
| `date`, `type`, `service`, `source`, `version` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `application.id`                   | ✓    | ✓      | ✓         | ✓        | ✓     | ✓     | ✓ |
| `session.id`                       | ✓    | ✓      | ✓         | ✓        | ✓     | ✓     | ✓ |
| `view.id`                          | ✓    | ✓      | ✓         | ✓        | ✓     | ✓     | ✓ |
| `build_version`                    | ✓    | ✓      | ✓         | ✓        | ✓     | ✗     | ✗ |
| `ddtags`                           | ✓    | ✓      | ✓         | ✓        | ✓     | ✓     | ✗ |
| `application.current_locale`       | ✓    | ✓      | ✗         | ✓        | ✓     | ✓     | ✗ |
| `session.type`                     | ✓    | ✓      | ✓         | ✓        | ✓     | ✓     | ✗ |
| `session.has_replay`               | ✓    | ✓      | ✓         | ✓        | ✓     | ✗     | ✗ |
| `session.is_active`                | ✓    | ✗      | ✗         | ✗        | ✗     | ✗     | ✗ |
| `view.url`, `view.name`            | ✓    | ✓      | ✓         | ✓        | ✓     | ✓     | ✗ |
| `usr.anonymous_id`                 | ✓    | ✓      | ✓         | ✓        | ✓     | ✓     | ✗ |
| `connectivity.status`, `connectivity.interfaces` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| `os.name`, `os.version`, `os.version_major` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| `device.type`, `device.name`, `device.model`, `device.brand`, `device.architecture` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| `device.logical_cpu_count`, `device.total_ram`, `device.is_low_ram` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| `device.locales`, `device.time_zone` | ✓ | ✓ | ✗      | ✓        | ✓     | ✓     | ✗ |
| `device.battery_level`, `device.brightness_level`, `device.power_saving_mode` | ✓ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| `context`                          | ✓    | ✓      | ✓         | ✓        | ✓     | ✓     | ✗ |
| `feature_flags`                    | ✓    | ✗      | ✗         | ✗        | ✓     | ✗     | ✗ |

**Key facts to internalize:**
- **`telemetry` is its own shape** — almost no "common" fields apply (it nests a small device/os subset under `telemetry.*` instead — see section 9).
- **Hot-path events drop slow sensor reads.** `action` and `long_task` skip `battery_level`, `brightness_level`, `power_saving_mode`. `long_task` additionally skips `locales` and `time_zone`. This is intentional — those calls would slow the ANR-watchdog path.
- **`session.is_active`** is **view-only** — it represents whether the session is still alive on the view-event's write, not whether the event is recent.
- **`feature_flags`** only ships on `view` and `error` (the two event types where flag attribution is most useful for debugging).
- **`build_version` and `session.has_replay`** drop off `vital` and `telemetry`.
- **`ddtags`** is the comma-separated tag string Datadog uses for facet/dimension routing; it's on every RUM event except telemetry.

> Below: full meaning/unit/example for every field. The presence matrix above tells you *whether* the field is there — the table below tells you *what it means* when it is.

| Field | Type | Meaning | Unit / format | Example |
|---|---|---|---|---|
| `date` | integer | Wall-clock timestamp when the event happened on the device | **Unix epoch in milliseconds** | `1779375090434` (≈ 2026-05-21T14:51:30Z) |
| `type` | string | Event type discriminator | one of `view` / `action` / `long_task` / `error` / `vital` / `telemetry` | `"view"` |
| `service` | string | Service name configured in `DatadogConfig` (defaults to the application's package id) | free string | `"com.example.myapplication"` |
| `source` | string | Platform that produced the event | always `"android"` for this SDK | `"android"` |
| `version` | string | App's `versionName` from `BuildConfig` | semver-ish | `"1.0"` |
| `build_version` | string | App's `versionCode` from `BuildConfig` | numeric string | `"1"` |
| `ddtags` | string | Datadog-style tag list, comma-separated `key:value` | string of `k:v,k:v,…` | `"service:com.example.myapplication,version:1.0,sdk_version:3.10.0,env:dev,variant:debug"` |
| `application.id` | string (UUID) | RUM application ID set in the SDK at init | UUID | `"f5d5e5c9-964d-461e-b16e-c6d9d5833645"` |
| `application.current_locale` | string | Active locale at the moment the event was created (BCP-47-ish) | language-region | `"en-IN"` |
| `session.id` | string (UUID) | Current RUM session ID. Stable until session ends. | UUID | `"ba928661-71c0-4614-8d5d-ef83223f47a6"` |
| `session.type` | enum | Origin of the session | `user` / `synthetics` / `ci_test` | `"user"` |
| `session.has_replay` | boolean | Whether Session Replay is recording this session | true/false | `false` |
| `session.is_active` | boolean | Whether the session is still alive when this event was written (view events only) | true/false | `true` |
| `view.id` | string (UUID) | Identifier of the view the event belongs to | UUID | `"41e4c00e-9e70-48b9-b997-7197a1d94372"` |
| `view.url` | string | A path-like identifier for the view (the SDK uses the class path) | string | `"com/example/myapplication/MainActivity"` |
| `view.name` | string | Human-readable view name (defaults to fully-qualified activity/fragment class) | string | `"com.example.myapplication.MainActivity"` |
| `usr.anonymous_id` | string (UUID) | Stable per-install identifier auto-generated by the SDK when no real user is set | UUID | `"4bcb7b80-f3ed-4a6a-a765-fa59788f4dfa"` |
| `context` | object | Free-form attributes attached to RUM via `addAttribute(...)` | open dict | `{}` |
| `feature_flags` | object | Active feature flags reported via `addFeatureFlagEvaluation(...)` | open dict | `{}` |

### Connectivity sub-object (`connectivity.*`)

| Field | Type | Meaning | Allowed values | Example |
|---|---|---|---|---|
| `connectivity.status` | enum | Coarse network reachability | `connected` / `not_connected` / `maybe` | `"connected"` |
| `connectivity.interfaces[]` | array of enum | Active interface(s) | `bluetooth`, `cellular`, `ethernet`, `wifi`, `wimax`, `mixed`, `other`, `unknown`, `none` | `["wifi"]` |

### OS sub-object (`os.*`)

| Field | Type | Meaning | Unit | Example |
|---|---|---|---|---|
| `os.name` | string | OS family | usually `"Android"` | `"Android"` |
| `os.version` | string | Full OS version | string | `"12"` |
| `os.version_major` | string | Major version only | string | `"12"` |

### Device sub-object (`device.*`)

| Field | Type | Meaning | Unit | Example |
|---|---|---|---|---|
| `device.type` | enum | Form factor | `mobile` / `desktop` / `tablet` / `tv` / `gaming_console` / `bot` / `other` | `"mobile"` |
| `device.name` | string | Marketing name | string | `"POCO M2010J19CI"` |
| `device.model` | string | `Build.MODEL` | string | `"M2010J19CI"` |
| `device.brand` | string | `Build.BRAND` | string | `"POCO"` |
| `device.architecture` | string | CPU architecture | string | `"aarch64"` |
| `device.locales[]` | array of string | All system locales | language-region | `["en-IN"]` |
| `device.time_zone` | string | IANA tz of the device | tz id | `"Asia/Kolkata"` |
| `device.battery_level` | number | Current battery charge | **0.0–1.0** (fraction, not percent) | `0.32` |
| `device.power_saving_mode` | boolean | Whether power-saver is on | true/false | `false` |
| `device.brightness_level` | number | Screen brightness | **0.0–1.0** (fraction); occasionally values >1.0 appear on emulators with auto-brightness — known SDK quirk, treat as anomalous | `0.4` |
| `device.logical_cpu_count` | integer | `Runtime.availableProcessors()` | count | `8` |
| `device.total_ram` | integer | Total RAM | **MB** (note: divided by 1024² in the SDK) | `5706` |
| `device.is_low_ram` | boolean | `ActivityManager.isLowRamDevice()` | true/false | `false` |

> ⚠️ Some events drop a subset of these (e.g. `long_task` doesn't include `battery_level`, `brightness_level`, `power_saving_mode`, `locales`, `time_zone`). That's because the long-task path runs on a hot ANR-watchdog code path and skips slow lookups.

---

## 3.1 Motadata functional additions (Branch 2, `v1.0.1`+)

These are **Motadata-specific** fields/params added on top of the rebrand. They appear automatically —
no app code. Present on the **5 backend-consumed event types** (`view`, `action`, `resource`, `error`,
`long_task`); **not** emitted on `vital` (app_launch) or `telemetry` events.

| Field / param | Where | Type | Unit / format | Meaning | Example |
|---|---|---|---|---|---|
| `md-api-key` | request **query** (+ `MD-API-KEY` header) | string | client token | the param the Motadata intake authenticates on | `…?mdsource=android&md-api-key=pub…` |
| `session.created` | inside `session` object | integer | **epoch milliseconds** (absolute) | wall-clock time the session started | `1780919007516` |
| `context._timing.navigationStart` | inside `context._timing` | integer | **epoch milliseconds** (absolute) | session start (Option A: equals `session.created`) | `1780919007516` |
| `context._timing.relativeTime` | inside `context._timing` | integer | **nanoseconds** (duration) | time **after session start** this event occurred = `(event.date − session.created) × 1e6` | `32568000000` (= 32.57 s) |
| `view.is_view_completed` | inside `view` object (view events only) | string | `"yes"` / `"no"` | `"yes"` on the final/authoritative update for a view, else `"no"` | `"yes"` |

> **`relativeTime` can be negative for the `ApplicationLaunch` view** (one per session): its `date` is
> backdated to actual app-start, which precedes `session.created`, so `date − created < 0`. This is
> expected and affects only that single synthetic launch view — every other event is positive.
>
> **Backend reads:** `session.created` → stored as `*.created.time.ms`; `relativeTime` → converted
> `NANOSECONDS → MICROSECONDS` and stored as `*.relative.time.us`. (So the SDK must send `relativeTime`
> in nanoseconds — it does.)

---

## 4. `view` event

A `view` event is **updated and re-sent** every time something happens on that view — new action, long task, slow frame, etc. The same `view.id` will be sent dozens of times with monotonically increasing `_dd.document_version`. The **last** one for a given `view.id` is the authoritative summary.

This is the richest event type. The captures have view events with up to **84 distinct field paths**.

### `view.*` block (the data unique to this event)

| Field | Type | Meaning | Unit | Example |
|---|---|---|---|---|
| `view.id` | string (UUID) | Identifier for this view session | UUID | `"2ec6c2ca-fe04-4ed5-92c4-06882ac56e54"` |
| `view.url` | string | Path-style id (class path on Android) | string | `"com/example/myapplication/MainActivity2"` |
| `view.name` | string | Display name | string | `"com.example.myapplication.MainActivity2"` |
| `view.time_spent` | integer | Total time the view was on screen | **nanoseconds** | `17420963000` (≈ 17.42 s) |
| `view.is_active` | boolean | View still on screen when the event was written | true/false | `true` (active update) or `false` (final update) |
| `view.is_slow_rendered` | boolean | "Whether the View had a low average refresh rate" | true/false | `false` |
| `view.interaction_to_next_view_time` | integer | "Duration from the last interaction on the previous view to the moment the current view was displayed" | **nanoseconds** | `2548826500` (≈ 2.55 s) |
| `view.action.count` | integer | Total user actions on this view so far | count | `10` |
| `view.error.count` | integer | Total errors reported on this view | count | `0` |
| `view.crash.count` | integer | Total crashes attributed to this view | count | `0` |
| `view.long_task.count` | integer | Total long tasks on this view | count | `11` |
| `view.frozen_frame.count` | integer | Frames that took ≥ 700 ms (frozen) | count | `2` |
| `view.resource.count` | integer | Total network resources on this view (will be 0 until you wire `DatadogInterceptor`) | count | `0` |
| `view.frustration.count` | integer | Total frustration signals attributed to this view (sum of `error_tap`s the SDK generated) | count | `0` |
| `view.memory_average` | number | "Average memory used during the view lifetime" | **bytes** | `2.6914611764705884E8` (≈ 269 MB) |
| `view.memory_max` | number | "Peak memory used during the view lifetime" | **bytes** | `2.76716E8` (≈ 277 MB) |
| `view.cpu_ticks_count` | number | "Total number of cpu ticks during the view's lifetime" | ticks | `530.0` |
| `view.cpu_ticks_per_second` | number | "Average number of cpu ticks per second during the view's lifetime" | ticks / s | `30.42` |
| `view.refresh_rate_average` | number | Average frame rate the UI thread achieved | **frames per second** | `58.56` |
| `view.refresh_rate_min` | number | Worst observed frame rate | **frames per second** | `6.52` (severe stutter spike) |
| `view.freeze_rate` | number | (when present) "Rate of freezes during the view's lifetime" | **seconds per hour** | `0.0` |
| `view.slow_frames_rate` | number | (when present) "Rate of slow frames during the view's lifetime" | **milliseconds per second** | `0.0` |
| `view.slow_frames[]` | array | Per-slow-frame breakdown | see below | (see below) |

### `view.slow_frames[]` (when present)

A slow frame is a render that exceeded the device refresh budget but isn't frozen. Each entry:

| Field | Type | Meaning | Unit | Example |
|---|---|---|---|---|
| `start` | integer | Offset from view start to when this slow frame began | **nanoseconds** | `25244094` (≈ 25.2 ms after view started) |
| `duration` | integer | How long the frame took | **nanoseconds** | `336731306` (≈ 337 ms — practically a frozen-frame edge) |

### Concrete example (truncated)

```json
{
  "date": 1779372290451,
  "type": "view",
  "view": {
    "id": "2ec6c2ca-fe04-4ed5-92c4-06882ac56e54",
    "url": "com/example/myapplication/MainActivity2",
    "name": "com.example.myapplication.MainActivity2",
    "interaction_to_next_view_time": 2548826500,
    "time_spent": 17420963000,
    "is_active": true,
    "is_slow_rendered": false,
    "action":      { "count": 10 },
    "error":       { "count": 0 },
    "crash":       { "count": 0 },
    "long_task":   { "count": 11 },
    "frozen_frame":{ "count": 2 },
    "resource":    { "count": 0 },
    "frustration": { "count": 0 },
    "slow_frames": [
      { "start": 25244094,   "duration": 336731306 },
      { "start": 3091910638, "duration": 37261662 }
    ],
    "memory_average": 269146117.65,
    "memory_max":     276716000,
    "cpu_ticks_count": 530.0,
    "cpu_ticks_per_second": 30.42,
    "refresh_rate_average": 58.56,
    "refresh_rate_min":     6.52
  },
  "_dd": { "document_version": 23, "replay_stats": { "records_count": 0, "segments_count": 0, "segments_total_raw_size": 0 } }
}
```

> 🔑 **Mental model:** each `view` event is a snapshot of the running aggregate. `_dd.document_version` increments with each emission, so multiple `view` events with the same `view.id` are *not* duplicates — they're versioned updates of the same record.

---

## 5. `action` event

One event per user interaction. The SDK emits this **after** the action's window closes (100 ms inactivity, or 5 s max — see `RumActionScope.kt`).

### `action.*` block

| Field | Type | Meaning | Allowed values / unit | Example |
|---|---|---|---|---|
| `action.id` | string (UUID) | Identifier for this specific interaction | UUID | `"125dcb5b-7e36-485c-ae25-432ec761e65e"` |
| `action.type` | enum | What kind of interaction | `custom`, `click`, `tap`, `scroll`, `swipe`, `application_start`, `back` | `"tap"` |
| `action.loading_time` | integer | Time from the start of the action to when the next view/resource finished loading | **nanoseconds**; can be `1` ns if nothing followed | `1` |
| `action.target.name` | string | Human-readable target label (the SDK derives it from `contentDescription`, resource id, etc.). **Often empty if you haven't labeled the button.** | string | `""` |
| `action.error.count` | integer | Errors attributed to this action's window | count | `0` |
| `action.crash.count` | integer | Crashes attributed to this action | count | `0` |
| `action.long_task.count` | integer | Long tasks attributed to this action | count | `0` |
| `action.resource.count` | integer | Network resources attributed to this action | count | `0` |
| `action.frustration.type[]` | array of enum (when present) | Frustration tags the SDK attached — see note below | `rage_click`, `dead_click`, `error_click`, `rage_tap`, `error_tap` | not present in any captured event |

**Frustration on actions**: this field is **absent** unless the SDK generated a frustration. The Android SDK *only* generates `error_tap` (when a tap had `eventErrorCount > 0`). All other frustration types are computed server-side by Datadog and will never appear in your local proxy captures.

### Android-specific flattened fields (top-level, not under `action.*`)

The Android SDK serializes some platform-specific metadata at the **root** of the JSON object (not nested), to avoid colliding with the cross-platform schema. You'll see these:

| Field | Type | Meaning | Allowed values | Example |
|---|---|---|---|---|
| `action.target.classname` | string | Java/Kotlin class of the tapped view | free string | `"com.google.android.material.button.MaterialButton"` |
| `action.target.resource_id` | string | Android resource id of the view (`R.id.xxx` name) | free string | `"buttonLoadJson"` |
| `action.gesture.direction` | enum (when present) | Direction for scroll/swipe actions | `up`, `down`, `left`, `right` | `"up"` |

> Yes — they really are written with the dots in the key name. This is how the schema's `additionalProperties` is leveraged on Android.
>
> **Captured directions:** 22 swipe events in your captures have `action.gesture.direction` populated — 17 × `up`, 5 × `down`. No `left`/`right` were produced. Pure taps (the dominant action type in your captures) don't carry this field.

### Concrete example

```json
{
  "date": 1779368806817,
  "type": "action",
  "action": {
    "type": "tap",
    "id": "125dcb5b-7e36-485c-ae25-432ec761e65e",
    "loading_time": 1,
    "target":     { "name": "" },
    "error":      { "count": 0 },
    "crash":      { "count": 0 },
    "long_task":  { "count": 0 },
    "resource":   { "count": 0 }
  },
  "action.target.classname":   "com.google.android.material.button.MaterialButton",
  "action.target.resource_id": "buttonLoadJson",
  "view": {
    "id":   "b24bf0c9-3b6b-4d66-9214-c210c9720738",
    "url":  "com/example/myapplication/MainActivity2",
    "name": "com.example.myapplication.MainActivity2"
  }
}
```

---

## 6. `long_task` event

Emitted whenever the SDK detects the main thread was blocked beyond a threshold (default 100 ms; > 700 ms → also flagged as a frozen frame).

### `long_task.*` block

| Field | Type | Meaning | Unit | Example |
|---|---|---|---|---|
| `long_task.id` | string (UUID) | Unique id of this long-task instance | UUID | `"6dca0545-ca3f-47a8-b093-cf34136c350e"` |
| `long_task.duration` | integer | How long the main thread was blocked | **nanoseconds** | `377278800` (≈ 377 ms) |
| `long_task.is_frozen_frame` | boolean | Whether this exceeded the frozen-frame threshold (≥ 700 ms) | true/false | `false` |

### Top-level `context` enrichment

Long tasks include an extra string under `context` describing what the looper was running when stuck:

| Field | Type | Meaning | Example |
|---|---|---|---|
| `context.long_task.target` | string | A stringified description of the offending Handler / message | `"Handler (android.app.ActivityThread$H) {76b3301} null: 159"` |

That string is the Looper message that was being processed — useful to localize the source of the jank.

### Concrete example

```json
{
  "date": 1779369479989,
  "type": "long_task",
  "long_task": {
    "id": "6dca0545-ca3f-47a8-b093-cf34136c350e",
    "duration": 377278800,
    "is_frozen_frame": false
  },
  "context": {
    "long_task.target": "Handler (android.app.ActivityThread$H) {76b3301} null: 159"
  },
  "view": { "id": "543a4bfb-…", "url": "com/example/myapplication/MainActivity", "name": "…MainActivity" }
}
```

> ⚠️ `long_task` events run on the ANR-watchdog path and **omit** `battery_level`, `brightness_level`, `power_saving_mode`, `device.locales`, and `device.time_zone` to stay cheap.

---

## 7. `resource` event

One event per outgoing HTTP request captured by `DatadogInterceptor` (attached to the customer's `OkHttpClient`). The SDK emits this **after** the request completes (success, failure, or error). Resource events are correlated with the view that was active when the call started.

> Captured on 2026-05-27 from the sample app using OkHttp + Picsum (images) + jsonplaceholder.typicode.com (JSON).

### `resource.*` block

| Field | Type | Meaning | Allowed values / unit | Example |
|---|---|---|---|---|
| `resource.id` | string (UUID) | Identifier for this specific request | UUID | `"2378213c-4e74-4955-8f6f-e26a1aac0155"` |
| `resource.type` | enum | Kind of resource — derived from the response's `Content-Type` MIME by the SDK | `image`, `native`, `document`, `beacon`, `fetch`, `xhr`, `js`, `font`, `css`, `media`, `other` | `"image"` (response was `image/jpeg`) · `"native"` (any other MIME, the OkHttp default) |
| `resource.method` | enum | HTTP method | `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, `CONNECT`, `OPTIONS`, `TRACE` | `"GET"` |
| `resource.url` | string | Full URL the request was sent to | absolute URL | `"https://picsum.photos/900/500"` |
| `resource.status_code` | integer | HTTP response status code | standard HTTP codes | `200` · `404` |
| `resource.duration` | integer | Wall-clock time from request start to response complete | **nanoseconds** | `43894300` (≈ 44 ms) · `4949023400` (≈ 4.9 s) |
| `resource.size` | integer | Response body size | **bytes** | `21269` (a JPEG) · `83` (a small JSON) · `2` (a 404 body) |

**`resource.type` detection rule** (from the OkHttp integration code):
```
null Content-Type  →  "native"
image/*            →  "image"
text/* / application/javascript →  "js" or "document" depending on context
otherwise          →  matched against MIME → falls back to "native"
```
In the current captures only `image` and `native` appeared because the sample app calls only Picsum (image/jpeg) and a JSON API (application/json — which maps to `native` for OkHttp Android).

### What's NOT in the captured resource events

These fields exist in the resource schema but were **not produced** by the SDK for the 2026-05-27 captures:

| Missing field | Why |
|---|---|
| `resource.provider` | Auto-populated by Datadog intake (CDN/SaaS classifier) — server-side enrichment |
| `resource.dns`, `resource.connect`, `resource.ssl`, `resource.first_byte`, `resource.download` | Detailed timing phases — require the OkHttp `EventListener` factory (`DatadogEventListener.Factory`), not just the interceptor. The current sample only attaches the interceptor |
| `resource.redirect` | Same — requires `DatadogEventListener` |
| `resource.graphql.*` | Only populated when using the Apollo GraphQL integration |
| `resource.worker.*` | Only relevant for browser/web RUM, not Android |
| `resource.trace_id`, `resource.span_id` | Only populated when distributed tracing is configured (`setFirstPartyHosts` with a tracing header type) — your snippet uses `emptyMap()` so no tracing |

**If you want per-phase timings** (DNS / connect / TLS / first-byte / download), the customer must also attach `DatadogEventListener.Factory`:
```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(MotadataInterceptor.Builder(emptyMap()).build())
    .eventListenerFactory(MotadataEventListener.Factory())   // ← add this
    .build()
```

### Top-level `context` enrichment

In all captured resource events, `context` was an empty object `{}`. The SDK does not auto-fill `context` for resources — it's only filled if the customer calls `addAttribute(...)` before the request.

### Concrete example — successful image fetch

```json
{
  "date": 1779877872518,
  "type": "resource",
  "application": { "id": "f5d5e5c9-…", "current_locale": "en-US" },
  "service": "com.example.myapplication",
  "version": "1.0",
  "build_version": "1",
  "ddtags": "service:com.example.myapplication,version:1.0,sdk_version:3.10.0,env:dev,variant:debug",
  "session": { "id": "995b9ff4-…", "type": "user", "has_replay": false },
  "source": "android",
  "view": {
    "id": "9885c1a3-…",
    "url": "com/example/myapplication/MainActivity2",
    "name": "com.example.myapplication.MainActivity2"
  },
  "usr": { "anonymous_id": "09be6200-…" },
  "connectivity": { "status": "connected", "interfaces": ["wifi"] },
  "os": { "name": "Android", "version": "17", "version_major": "17" },
  "device": { "type": "mobile", "name": "Google sdk_gphone16k_x86_64", "model": "sdk_gphone16k_x86_64", "brand": "Google", "architecture": "x86_64", "locales": ["en-US"], "time_zone": "Asia/Calcutta", "logical_cpu_count": 4, "total_ram": 2971, "is_low_ram": false },
  "_dd": { "format_version": 2, "session": { "session_precondition": "user_app_launch" }, "configuration": { "session_sample_rate": 100 } },
  "context": {},
  "resource": {
    "id": "2378213c-4e74-4955-8f6f-e26a1aac0155",
    "type": "image",
    "method": "GET",
    "url": "https://picsum.photos/900/500",
    "status_code": 200,
    "duration": 4949023400,
    "size": 21269
  }
}
```

### Concrete example — 404 to a JSON API

```json
{
  "date": 1779878003124,
  "type": "resource",
  "view": { "id": "9885c1a3-…", "url": "com/example/myapplication/MainActivity2", "name": "…MainActivity2" },
  "resource": {
    "id": "24a3fe3a-5786-435e-9a42-061b1b26ce15",
    "type": "native",
    "method": "GET",
    "url": "https://jsonplaceholder.typicode.com/this-route-does-not-exist",
    "status_code": 404,
    "duration": 2151385200,
    "size": 2
  }
}
```

Note: a non-2xx status code (e.g. 404, 500) is recorded as a successful **resource** event with the right `status_code` — it is **not** automatically promoted to an `error` event. To convert HTTP failures into RUM errors, the customer must call `stopResourceWithError(...)` manually or use the `DatadogInterceptor`'s `traceOrigin`-based error handling.

### Variety captured (2026-05-27 sessions)

| `resource.type` | `resource.method` | `status_code` | Count |
|---|---|---|---|
| `image` | `GET` | `200` | majority — Picsum image fetches |
| `native` | `GET` | `200` | jsonplaceholder JSON gets |
| `native` | `GET` | `404` | 2× (intentional 404 testing) |

No POST/PUT/DELETE were captured because the sample app doesn't issue them. The SDK supports all standard HTTP methods — `resource.method` just reports whatever the OkHttp request had.

---

## 8. `error` event

Reported via:
- automatic crash interceptor (uncaught `Throwable`)
- `GlobalRumMonitor.get().addError(...)` / `addErrorWithStacktrace(...)`
- the OkHttp interceptor for HTTP errors (not seen in captures because no `resource` events were emitted)

### `error.*` block

| Field | Type | Meaning | Allowed values / unit | Example |
|---|---|---|---|---|
| `error.id` | string (UUID) | Unique id of this error | UUID | `"2b38a45f-3f25-4bfe-a33b-6afc08f4e7eb"` |
| `error.message` | string | Human-readable error message | free string | `"Intentional crash from Event Lab"` |
| `error.source` | enum | Where the error originated (semantic) | `network`, `source`, `console`, `logger`, `agent`, `webview`, `custom`, `report` | `"source"` (uncaught code) |
| `error.source_type` | enum | Stacktrace dialect | `android`, `browser`, `ios`, `react-native`, `flutter`, `ndk`, … | `"android"` |
| `error.type` | string | Exception type (fully-qualified Java class) | free string | `"java.lang.RuntimeException"` |
| `error.category` | enum | High-level category | `ANR`, `App Hang`, `Exception`, `Watchdog Termination`, `Memory Warning`, `Network` | `"Exception"` |
| `error.is_crash` | boolean | Did this error crash the app? | true/false | `true` |
| `error.stack` | string | Full stack trace (Java newline-separated) | text | `"java.lang.RuntimeException: …\n\tat com.example.…(MainActivity2.java:222)\n\t…"` |
| `error.time_since_app_start` | integer | "Time since application start when error happened" | **milliseconds** | `39585` (≈ 39.6 s after process start) |
| `error.threads[]` | array | Snapshot of every thread at the moment of the error | see below | (see below) |

### `error.threads[]` items

| Field | Type | Meaning | Example |
|---|---|---|---|
| `name` | string | Thread name | `"main"`, `"datadog-upload-thread-1"`, `"OkHttp ConnectionPool"`, … |
| `crashed` | boolean | Whether this thread is the one that crashed | `true` for the main crashing thread, `false` otherwise |
| `state` | string | JVM thread state | `runnable`, `waiting`, `timed_waiting`, `blocked` |
| `stack` | string | Per-thread stack trace | text |

> The thread dump is wide — captured errors typically contain 30–40 thread entries (main app threads, AndroidX work, OkHttp, Datadog internal threads `datadog-rum-*` / `datadog-upload-thread-*` / `datadog-storage-thread-*`, Android Studio inspector threads, etc.).

### Concrete example (truncated)

```json
{
  "date": 1779369518887,
  "type": "error",
  "error": {
    "id": "2b38a45f-3f25-4bfe-a33b-6afc08f4e7eb",
    "message": "Intentional crash from Event Lab",
    "source": "source",
    "source_type": "android",
    "type": "java.lang.RuntimeException",
    "category": "Exception",
    "is_crash": true,
    "stack": "java.lang.RuntimeException: Intentional crash from Event Lab\n\tat com.example.myapplication.MainActivity2.lambda$confirmCrash$20$com-example-myapplication-MainActivity2(MainActivity2.java:222)\n\t…",
    "time_since_app_start": 39585,
    "threads": [
      { "name": "main", "crashed": true,  "state": "runnable", "stack": "java.lang.RuntimeException: …" },
      { "name": "OkHttp ConnectionPool", "crashed": false, "state": "timed_waiting", "stack": "…" }
    ]
  },
  "view": { "id": "2e16fad1-…", "name": "…MainActivity2" }
}
```

### 7.1 ANR (Application Not Responding) — special-case `error` event

ANRs are **not a separate event type** — they ride on the `error` event, with a distinctive set of field values. An ANR is raised by Android when the **main thread is unresponsive for ≥ 5 s** (input dispatch), ≥ 10 s (broadcast receiver), or similar thresholds for other components.

The SDK detects ANRs via two paths and emits them as `error` events:

| Path | Source code | Triggers | `is_crash` | When event is written |
|---|---|---|---|---|
| **Watchdog (non-fatal)** | `ANRDetectorRunnable.kt` | Background thread posts a no-op to the main `Handler` and waits ≤ 5 s on a `CountDownLatch`. Misses → ANR. Polls every 500 ms. | `false` | Same session, in-process, immediately on detection |
| **Late reporter (fatal)** | `DatadogLateCrashReporter.kt` | Reads `ActivityManager.getHistoricalProcessExitReasons()` for `REASON_ANR` (Android 11+ / API 29+) | `true` | **Next** app launch, tied to the **previous** session's last view |

Both can fire for the same incident (the SDK docs explicitly warn about possible duplication on Android 11+ — see `RumConfiguration.kt:160`).

#### Distinctive field values for any ANR

| Field | Value |
|---|---|
| `error.category` | `"ANR"` |
| `error.message` | `"Application Not Responding"` (constant from `ANRDetectorRunnable.ANR_MESSAGE`) |
| `error.type` | `"com.datadog.android.rum.internal.anr.ANRException"` |
| `error.source` | `"source"` |
| `error.source_type` | `"android"` |
| `error.threads[]` | Full thread dump; the entry with `crashed: true` is the **main** thread (always `main` for an ANR) — its `state` (`sleeping`, `waiting`, `blocked`, `runnable`) explains *how* it was stuck, and its `stack` pinpoints **where** |

#### How to tell which path fired (fatal vs non-fatal)

| Signal | Watchdog (non-fatal) | Late reporter (fatal) |
|---|---|---|
| `error.is_crash` | `false` | `true` |
| `error.time_since_app_start` | populated (ms) | **`null`** (known TODO RUM-3780) |
| Companion view's `view.crash.count` | unchanged | incremented (`+1`) |
| Companion view's `view.is_active` | usually `true` (app recovered) | `false` (session was killed) |
| Surrounding session | event is in the **same** session as the action that caused it | event is in the **previous** session; a **new** session starts immediately before, with `_dd.session.session_precondition = "user_app_launch"` |

The "session split" is the most reliable tell. If you see a fatal ANR, you'll always see two sessions: the doomed one ending with the ANR, and a fresh `user_app_launch` session right after.

#### Worked example (from `20260525-161715-008/event-01-error.json` in these captures)

This is a **fatal** ANR caught by the late reporter:

```json
{
  "date": 1779705839840,
  "type": "error",
  "session": { "id": "594f5149-9326-4ff3-a796-7ad66611ce58" },
  "view":    { "id": "e5a2f255-50a5-4e03-9197-58e9918ccf5b",
               "name": "com.example.myapplication.MainActivity2" },
  "error": {
    "category":    "ANR",
    "message":     "Application Not Responding",
    "type":        "com.datadog.android.rum.internal.anr.ANRException",
    "source":      "source",
    "source_type": "android",
    "is_crash":    true,
    "time_since_app_start": null,
    "threads": [
      {
        "name":    "main",
        "crashed": true,
        "state":   "sleeping",
        "stack":   "at java.lang.Thread.sleep(Native method)\n  at java.lang.Thread.sleep0(Thread.java:689)\n  …\n  at com.example.myapplication.MainActivity2.triggerAnrMimic(MainActivity2.java:219)\n  at com.example.myapplication.MainActivity2.lambda$onCreate$13$… (MainActivity2.java:97)\n  …\n  at android.os.Looper.loop(Looper.java:397)\n  at android.app.ActivityThread.main(ActivityThread.java:9523)"
      }
      // … 44 more thread entries
    ]
  }
}
```

Reconstructed timeline from the captures (verifies it was the late-reporter path):

```
t=…35576 ms   action: tap on MainActivity2's "trigger ANR" button (session 594f5149)
t=…35577 ms   main thread enters triggerAnrMimic() → Thread.sleep(…)
t=…39840 ms   OS records EXIT_REASON_ANR; process killed
              (this becomes the error event's `date`)
t=…39996 ms   user relaunches the app → NEW session ef3f1b6b begins,
              session_precondition = "user_app_launch"
              SDK init runs DatadogLateCrashReporter.handleAnrCrash():
                 - reads getHistoricalProcessExitReasons()
                 - sees EXIT_REASON_ANR at …39840
                 - emits the ANR error event back-attributed to session 594f5149's
                   last view (e5a2f255 / MainActivity2)
                 - re-writes view e5a2f255 with crash.count = 1, is_active = false
```

The view event series for `e5a2f255` confirms the late-reporter view rewrite:

| `_dd.document_version` | `view.crash.count` | `view.is_active` |
|:-:|:-:|:-:|
| 2 (before ANR) | 0 | true |
| 3 (before ANR) | 0 | true |
| **4 (after ANR, written by late reporter)** | **1** | **false** |

#### Reading workflow when you see an ANR event

1. **Fatal or non-fatal?** Check `error.is_crash` (and the `time_since_app_start` null tell).
2. **Which view/session?** `session.id` + `view.id` + `view.name`. For fatal ANRs, expect a fresh `user_app_launch` session right after.
3. **What blocked main?** Inside `error.threads[]`, find the entry with `crashed: true` (always `main`). Read its `state` and `stack`.
4. **What was the user doing right before?** The last `action` in the same session is almost always the trigger.
5. **What does the "ANR rate" metric in Datadog count?** It's `sessions_with_ANR / total_sessions` over the selected window — this ANR contributes 1 to the numerator of *session 594f5149's* day.

#### How to generate ANRs in the sample app

| Want to generate | Do this in a click handler |
|---|---|
| **Fatal ANR** (`is_crash: true`, late-reporter path) | `Thread.sleep(15000)` on the main thread, then close the app from the "App not responding" dialog → relaunch |
| **Non-fatal ANR** (`is_crash: false`, watchdog path) | `Thread.sleep(6000)` on the main thread, but **don't** close the dialog — wait for the app to recover |
| **Lock-induced ANR** (state will be `blocked`/`waiting` instead of `sleeping`) | Acquire a lock on a background thread, then `synchronized(lock) {}` on the main thread |

---

## 9. `vital` (app_launch) event

Single events that report startup-timing metrics. In the captures, only the **app_launch** flavor appears (the schema also defines an "operation-step" flavor that the SDK didn't emit).

Note: the file is named `*-vital-app_launch.json` but the actual `type` field on the JSON is just `"vital"` — the sub-type is in `vital.type` / `vital.app_launch_metric`.

### `vital.*` block

| Field | Type | Meaning | Allowed values / unit | Example |
|---|---|---|---|---|
| `vital.id` | string (UUID) | Unique id of this measurement | UUID | `"e7268709-b8b4-463c-bce0-0ea217c5cde7"` |
| `vital.type` | enum | Vital family | `app_launch` (in these captures) | `"app_launch"` |
| `vital.name` | string | Human-readable metric name | string | `"time_to_initial_display"` |
| `vital.app_launch_metric` | enum | Which startup metric | `ttid` (Time To Initial Display) or `ttfd` (Time To Full Display) | `"ttid"` |
| `vital.duration` | number | How long the metric measured | **nanoseconds** | `1349714900` (≈ 1.35 s) |
| `vital.startup_type` | enum | Kind of launch | `cold_start` or `warm_start` | `"cold_start"` |
| `vital.has_saved_instance_state_bundle` | boolean | Whether the activity restored from a saved bundle | true/false | `false` |

### Concrete example

```json
{
  "date": 1779369479301,
  "type": "vital",
  "vital": {
    "id": "e7268709-b8b4-463c-bce0-0ea217c5cde7",
    "name": "time_to_initial_display",
    "type": "app_launch",
    "app_launch_metric": "ttid",
    "duration": 1349714900,
    "startup_type": "cold_start",
    "has_saved_instance_state_bundle": false
  },
  "_dd": { "profiling": {} }
}
```

### 9.1 TTID and TTFD — the two startup metrics

App-launch vital events come in two flavors, distinguished by `vital.app_launch_metric`:

| Metric | `app_launch_metric` | `vital.name` | Means | Who decides "ready"? |
|---|---|---|---|---|
| **TTID** (Time To Initial Display) | `ttid` | `time_to_initial_display` | Time from launch until the **first frame** is rendered | Android system (FrameMetrics) — auto-detected by the SDK |
| **TTFD** (Time To Full Display) | `ttfd` | `time_to_full_display` | Time from launch until the **content is actually usable** (data loaded, lists populated) | **Your code** — you call `GlobalRumMonitor.get().reportAppFullyDisplayed()` |

One-liner intuition: **TTID = "I see pixels"; TTFD = "I can use the app."**

#### Why both exist

A splash screen can paint a logo in 200 ms (great TTID) while the real content is still loading for 4 s (bad TTFD). TTID alone over-states perceived speed. TTFD captures what the user actually waits for.

#### How the SDK obtains each

| | TTID | TTFD |
|---|---|---|
| Source code | `RumFeature.kt:719` `onTTIDComputed` | `RumMonitor.kt:436-442` `reportAppFullyDisplayed()` |
| Trigger | Automatic — the OS reports first frame | **Manual** — you must call the API |
| Enabled by default? | Yes | No (no event is emitted unless you call the API) |
| Per-session | Once per cold/warm start | First call only (subsequent calls ignored) |

#### Behavioral guarantee from the SDK

If you call `reportAppFullyDisplayed()` **before** TTID has been computed, the SDK clamps TTFD to the TTID value (`RumSessionScopeStartupManager.kt:142-145`). TTFD ≥ TTID always.

#### `startup_type` matters more than you'd think

| Value | Meaning | Typical duration on mobile |
|---|---|---|
| `cold_start` | Process started fresh (`Application.onCreate` ran) | 500 ms – several seconds |
| `warm_start` | Process alive, Activity recreated | usually < 500 ms |

**Never mix cold and warm in the same average** — they're different populations. Filter by `startup_type` before computing p50/p95.

#### How to generate TTFD in your sample app

Add to your sample app at the moment the screen is genuinely usable (e.g. after API call resolves and the UI is populated):

```kotlin
import com.datadog.android.rum.GlobalRumMonitor

// in onCreate or after data load completes
GlobalRumMonitor.get().reportAppFullyDisplayed()
```

Place it:
- After initial data has loaded (in your network `onSuccess` callback)
- From the main thread
- Once per session — subsequent calls are ignored

Once added, every cold start produces **two** vital events (one TTID, one TTFD). TTFD − TTID = how much non-startup work happens after the first frame, usually the most actionable startup number.

#### What's in *these* captures

15 `vital-app_launch` events total. All TTID, all cold_start. **Zero TTFD** — confirms the sample app doesn't call `reportAppFullyDisplayed()` yet.

| Statistic | Value |
|---|---|
| Min TTID | 1,125 ms |
| Median TTID | ~1,500 ms |
| Mean TTID | ~2,113 ms |
| **Max TTID** | **9,052 ms (outlier in `20260521-194858-005`)** |
| Warm starts | 0 |
| TTFD events | 0 |

The 9 s outlier is the only suspicious data point — likely caused by a heavy `Application.onCreate`, a blocking main-thread I/O before first frame, or first-install JIT warmup. Cross-reference the long_task events in that session to localize it.

#### Analysis workflow when reading TTID/TTFD

1. **Filter by `startup_type` first.** Always.
2. **Use p50 and p95**, not mean. Distributions are heavily right-skewed.
3. **Compute TTFD − TTID per session.** Big gap = lots of work after first frame.
4. **Slice by `has_saved_instance_state_bundle`.** Restoring state costs measurable time.
5. **Cross-reference with `long_task` events** in the same session and within the launch window. If TTID is 3 s and you see a 2 s long_task right after process start, the long_task is your suspect.

---

## 10. `telemetry` event

The SDK reports its own health. The schema folder defines four telemetry sub-types (`debug`, `error`, `configuration`, `usage`) but the captures contain only `error`-status entries.

The telemetry envelope is **leaner** than RUM events — it drops most device/OS/connectivity fields and nests a small device/os subset under `telemetry.*`.

### Top-level fields on telemetry

| Field | Type | Meaning | Example |
|---|---|---|---|
| `date` | integer | Timestamp (epoch ms) | `1779369481150` |
| `type` | string | Always `"telemetry"` | `"telemetry"` |
| `service` | string | Always `"dd-sdk-android"` (the SDK reports itself as the service) | `"dd-sdk-android"` |
| `source` | string | `"android"` | `"android"` |
| `version` | string | **SDK version**, not app version | `"3.10.0"` |
| `application.id` | UUID | The app's RUM application id | `"f5d5e5c9-…"` |
| `session.id` | UUID | Current RUM session id (helps tie telemetry to a RUM session) | `"187b1111-…"` |
| `view.id` | UUID | View active at the moment | `"543a4bfb-…"` |
| `action.id` | UUID | (when present) action active at the moment | `"…"` |
| `effective_sample_rate` | number | Effective post-sampling rate that produced this telemetry record | `20.0` (i.e. 20%) |

### `telemetry.*` block

| Field | Type | Meaning | Allowed values | Example |
|---|---|---|---|---|
| `telemetry.type` | enum | Telemetry kind | `log` (captured), also possible: `configuration`, `usage` | `"log"` |
| `telemetry.status` | enum | Severity for `log` telemetry | `debug`, `error` | `"error"` |
| `telemetry.message` | string | Free-form message from the SDK | string | `"Unexpected status code 200 on upload request: RUM Request"` |
| `telemetry.process_uptime` | integer | Time since SDK started in this process | **milliseconds** | `1855` |
| `telemetry.device.architecture` | string | CPU arch (mirrored for telemetry) | | `"x86_64"` |
| `telemetry.device.brand` | string | Device brand | | `"Google"` |
| `telemetry.device.model` | string | Device model | | `"sdk_gphone16k_x86_64"` |
| `telemetry.device.logical_cpu_count` | integer | CPU count | count | `4` |
| `telemetry.device.total_ram` | integer | Total RAM | **MB** | `2971` |
| `telemetry.device.is_low_ram` | boolean | Low-RAM device | true/false | `false` |
| `telemetry.os.name` | string | OS name | | `"Android"` |
| `telemetry.os.version` | string | OS version | | `"17"` |
| `telemetry.os.build` | string | Android build fingerprint | | `"CP21.260330.005"` |

### Concrete example

```json
{
  "_dd":    { "format_version": 2 },
  "type":   "telemetry",
  "date":   1779369481150,
  "service":"dd-sdk-android",
  "source": "android",
  "version":"3.10.0",
  "application": { "id": "f5d5e5c9-…" },
  "session":     { "id": "187b1111-…" },
  "view":        { "id": "543a4bfb-…" },
  "effective_sample_rate": 20.0,
  "telemetry": {
    "device": { "architecture": "x86_64", "brand": "Google", "model": "sdk_gphone16k_x86_64", "logical_cpu_count": 4, "total_ram": 2971, "is_low_ram": false },
    "os":     { "build": "CP21.260330.005", "name": "Android", "version": "17" },
    "type":   "log",
    "status": "error",
    "message":"Unexpected status code 200 on upload request: RUM Request",
    "process_uptime": 1855
  }
}
```

> Telemetry is **sampled** independently of RUM (default 20% — that's why `effective_sample_rate: 20.0` is showing here).

---

## 11. SDK-internal `_dd.*` block

Every event has a `_dd` envelope of SDK-internal metadata.

| Field | Type | Meaning | Example |
|---|---|---|---|
| `_dd.format_version` | integer | Wire format version of the payload | `2` |
| `_dd.session.session_precondition` | enum | What triggered this session | `user_app_launch`, `inactivity_timeout`, `max_duration`, `background_launch`, `prewarm`, `from_non_interactive_session`, `explicit_stop` |
| `_dd.configuration.session_sample_rate` | number | The sample rate that **was in effect** for this session | `100.0` (percent) |
| `_dd.document_version` | integer | (view events only) Monotonic counter of how many times this exact `view.id` has been emitted | `23` |
| `_dd.replay_stats.records_count` | integer | (view events only) Session Replay records captured for this view | `0` (you don't have SR enabled) |
| `_dd.replay_stats.segments_count` | integer | (view events only) Replay segments uploaded | `0` |
| `_dd.replay_stats.segments_total_raw_size` | integer | (view events only) Total uncompressed replay bytes | `0` |
| `_dd.profiling` | object | (vital events only) Profiling correlation block — empty `{}` because profiling SDK isn't integrated | `{}` |

---

## 12. What the SDK does NOT put in the payload

Everything in this section is **absent from the captures** because either (a) Datadog's intake/processing pipeline adds it server-side, or (b) it belongs to a different Datadog SDK/intake. This is a categorized list of *well-known* enrichments — **not exhaustive**, since intake-side behavior is at Datadog's discretion and evolves over time. The SDK side (above) is contract-bound by the JSON schemas in the repo; the server side is product behavior you observe in the Datadog UI.

### 12.1 Server-side enrichments added by Datadog intake

| Category | Fields you'll see in Datadog (typical) | How it's derived |
|---|---|---|
| **GeoIP** | `@geo.country`, `@geo.country_iso_code`, `@geo.continent`, `@geo.continent_code`, `@geo.subdivision`, `@geo.subdivision_iso_code`, `@geo.city`, `@geo.timezone` | MaxMind / equivalent lookup against the request's source IP |
| **Network / client IP** | `@network.client.ip`, `@network.client.geoip.*`, sometimes `@network.client.carrier_name`, `@network.client.connection_type` (best-effort) | TCP socket / `X-Forwarded-For` / IP-to-ASN lookup |
| **User-agent parsing** | UA-derived facets (mostly relevant for Browser RUM; minimal on Android since the UA is the Dalvik runtime UA) | Parsing the `User-Agent` header |
| **`@type` / facet expansion** | Every JSON field promoted to a `@`-prefixed attribute in the UI for filtering | Automatic indexing at ingest |
| **Trace correlation** | `@trace_id`, `@span_id` linking RUM events to APM spans | Looked up via the `dd.trace_id` your app passes (if APM + RUM correlation is wired) |
| **Session attribution** | Session-level rollups: total view count, action count, error count, duration, "session quality" flags | Computed from the stream of events sharing a `session.id` |
| **View attribution** | View-level rollups computed across the SDK's multiple `view` snapshots (`_dd.document_version` series) — the UI shows the latest authoritative version | Reducer over view documents |
| **Service / env correlation** | Links between RUM service and APM service, RUM env and APM env, deploy/version tagging | Matching on `service` + `env` + `version` tags |
| **Sampling / quality flags** | `_dd.session.sampled`, "session was downsampled", "event was truncated", replay sampling decisions | Pipeline metadata |
| **Replay cross-references** | Pointers from RUM events to the corresponding session-replay segments when SR is enabled | Generated when the replay intake records segments for the same `session.id` |

### 12.2 Server-side **frustration signals** (not done by SDK)

The Android SDK only ever generates `error_tap` (when a tap had at least one error in its window). The other entries in the `action.frustration.type` enum are produced by **server-side pattern analysis** over many action events:

| Frustration type | What it means (computed at intake) |
|---|---|
| `rage_click` / `rage_tap` | Many rapid clicks/taps on the same target within a short window |
| `dead_click` | A click that produced no observable change (no view transition, no resource, no DOM mutation) |
| `error_click` | A click on the same view as a subsequent error (broader rule than `error_tap`) |

You will **never** see these in `rum-captures/` regardless of how you tap — they only appear after the events reach Datadog and the rules run.

### 12.3 Things that go to a *different* intake (different SDK)

These won't appear under `/api/v2/rum/` even if you enable them — they have their own endpoints and their own SDKs.

| Intake path | Belongs to | Add to capture by integrating |
|---|---|---|
| `/api/v2/logs` | `dd-sdk-android-logs` | Logger API |
| `/api/v2/spans` | `dd-sdk-android-trace` | `AndroidTracer` / OpenTracing |
| `/api/v2/replay` | `dd-sdk-android-session-replay` | Session Replay feature |
| profiling intake | `dd-sdk-android-profiling` | Profiling feature |
| flags intake | `dd-sdk-android-flags` | Feature flags |

### 12.4 RUM event types the SDK *can* emit but didn't in your captures

These are valid `/api/v2/rum/` payloads with their own schemas in `features/dd-sdk-android-rum/src/main/json/rum/`. Add the relevant instrumentation in your sample app to capture them:

| Type | Schema | How to trigger |
|---|---|---|
| `vital` (operation-step flavor) | `vital-operation-step-schema.json` | Use the operation-step API on `RumMonitor` |
| `telemetry` (debug) | `telemetry/debug-schema.json` | Internal — raise telemetry sample rate to capture |
| `telemetry` (configuration) | `telemetry/configuration-schema.json` | Emitted once at SDK init, sampled — raise `telemetryConfigurationSampleRate` to 100% |
| `telemetry` (usage) | `telemetry/usage-schema.json` | Emitted when certain APIs are called, sampled — raise `telemetrySampleRate` to 100% |

> `resource` events were added in the 2026-05-27 capture session and are now documented in Section 7.

### 12.5 Honest disclaimer

The SDK-side fields in sections 3–10 are **contract-bound** by the JSON schemas in this repo — what's there is what the SDK emits, what's not isn't.

The server-side list above (11.1, 11.2) is **best-effort**: it covers the well-known categories Datadog adds to mobile RUM events at the time of writing. Datadog can and does add new enrichments over time, and some of the above are conditional on org settings, plan, or other features being enabled (APM correlation, Session Replay, etc.). If you see an `@`-prefixed field in the Datadog UI that isn't covered here, treat it as **likely** server-side — the rule of thumb is: if it's in a capture, it's SDK-emitted; if it only appears in the Datadog UI, it's intake-enriched.

---

## Quick unit cheat-sheet

| Unit | Used by |
|---|---|
| Unix epoch **milliseconds** | every event's `date` |
| **Nanoseconds** | `view.time_spent`, `view.interaction_to_next_view_time`, `view.slow_frames[].start`, `view.slow_frames[].duration`, `long_task.duration`, `vital.duration`, `action.loading_time`, `resource.duration` |
| **Milliseconds** | `error.time_since_app_start`, `telemetry.process_uptime` |
| **Bytes** | `view.memory_average`, `view.memory_max`, `resource.size` |
| **Megabytes** | `device.total_ram`, `telemetry.device.total_ram` |
| **Fraction 0.0–1.0** | `device.battery_level`, `device.brightness_level` |
| **Frames per second** | `view.refresh_rate_average`, `view.refresh_rate_min` |
| **Seconds per hour** | `view.freeze_rate` |
| **Milliseconds per second** | `view.slow_frames_rate` |
| **Percent 0–100** | `_dd.configuration.session_sample_rate`, `effective_sample_rate` |

— end of Step 1 reference —
