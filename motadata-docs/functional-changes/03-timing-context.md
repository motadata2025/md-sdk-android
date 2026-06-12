# 3. `context._timing` object

## What changed
Every event now carries a **`_timing`** object in its context, giving the backend a browser-RUM-style
timing anchor: when the session started (`navigationStart`) and how long after that the event
occurred (`relativeTime`).

## Where
- `features/dd-sdk-android-rum/.../internal/domain/RumContext.kt:79-93` — `TIMING_CONTEXT_KEY = "_timing"`
  and `buildTimingContext(eventTimestampMs, sessionStartTimestampMs)`
- Emitted via `RumContext.buildTimingContext(...)` on events (e.g. `RumViewScope.kt:1356`)

## Wire output
```json
"_timing": {
  "navigationStart": 1749470400000,
  "relativeTime": 5300000000
}
```

## Field meaning
| Field | Unit | Value |
|-------|------|-------|
| `navigationStart` | epoch ms | session start time (same as [`session.created`](02-session-created-timestamp.md)) |
| `relativeTime` | **nanoseconds** | `(eventTimestampMs − sessionStartTimestampMs) × 1_000_000` |

## Why
This mirrors the browser SDK's timing model so the backend can process Android and browser RUM the
same way. Note `relativeTime` is in **nanoseconds** — the unit the intake expects — even though the
inputs are milliseconds. `navigationStart` is fixed to the session start (navigationStart == session
start).
