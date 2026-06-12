# 4. `view.is_view_completed` flag

## What changed
View events now carry **`is_view_completed`** — a string `"yes"`/`"no"` saying whether this is the
final, authoritative update for the view or just an in-progress update. It is paired with the
existing `is_active` boolean, which is its exact inverse.

## Where
- `features/dd-sdk-android-rum/.../internal/domain/scope/RumViewScope.kt:1272-1273`
- `features/dd-sdk-android-rum/src/main/json/rum/_view-properties-schema.json:176`

```kotlin
isActive = !viewComplete,
isViewCompleted = if (viewComplete) "yes" else "no",
```

## Wire output
```json
"view": {
  "is_active": false,
  "is_view_completed": "yes"
}
```

## Field meaning
| `view.is_active` | `is_view_completed` | Meaning |
|------------------|---------------------|---------|
| `true` (boolean) | `"no"` (string) | view still open — in-progress update |
| `false` (boolean) | `"yes"` (string) | view ended — final authoritative update |

The two are always inverses of the same `viewComplete` flag, so they can never disagree. The backend
only needs **one** of them; `is_view_completed` alone is enough to identify the final update.

> Not to be confused with **`session.is_active`**, which tracks the session lifecycle and is
> independent of view completion.

## Why
A view emits multiple updates over its lifetime. `is_view_completed` lets the backend know which
update is the last, authoritative one for a view, so it can finalise that view's metrics rather than
overwriting with a stale in-progress value.
