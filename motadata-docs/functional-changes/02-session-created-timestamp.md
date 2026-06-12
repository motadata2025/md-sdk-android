# 2. `session.created` timestamp

## What changed
Every event now includes a **`session.created`** field: the epoch-milliseconds timestamp at which
the current session started. Upstream had no such field.

## Where
- `features/dd-sdk-android-rum/.../internal/domain/RumContext.kt` — `sessionStartTimestampMs` is
  carried on the RUM context (`SESSION_START_TIMESTAMP = "session_start_timestamp"`)
- `features/dd-sdk-android-rum/.../internal/domain/scope/RumViewScope.kt:781,1325,1520` —
  `created = rumContext.sessionStartTimestampMs` on emitted events
- `features/dd-sdk-android-rum/src/main/json/rum/_common-schema.json:79` — schema definition

## Wire output
```json
"session": {
  "id": "…",
  "type": "user",
  "has_replay": false,
  "created": 1749470400000
}
```

## Why
The backend uses the session start time as the anchor for session-level timing and ordering. It is
the same value used to compute [`_timing`](03-timing-context.md) (`navigationStart`). The value is the
server-corrected session start (epoch ms), so it stays consistent across all events in a session.
