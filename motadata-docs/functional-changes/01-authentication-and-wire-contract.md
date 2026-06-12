# 1. Authentication & request wire contract

## What changed
Every RUM upload now carries the client token as a **`md-api-key` query parameter**, and the SDK
sends a Motadata-specific set of `MD-*` headers and `md*` query params instead of the upstream ones.
The backend authenticates on the **query parameter** (`md-api-key`); the matching header is sent too.

## Where
- `dd-sdk-android-core/.../api/net/RequestFactory.kt` — wire constants
- `features/dd-sdk-android-rum/.../internal/net/RumRequestFactory.kt:60,79` — applies them per request

## Wire contract
**Query params** (on the intake URL):

| Param | Value |
|-------|-------|
| `md-api-key` | client token (intake authenticates on this) |
| `mdsource` | source, e.g. `android` |
| `mdtags` | request tags (when present) |

**Headers**:

| Header | Value |
|--------|-------|
| `MD-API-KEY` | client token |
| `MD-EVP-ORIGIN` | source, e.g. `android` |
| `MD-EVP-ORIGIN-VERSION` | SDK version |
| `MD-REQUEST-ID` | per-request id |
| `MD-IDEMPOTENCY-KEY` | retry idempotency key (when present) |

Final URL shape: `<endpoint>/api/v2/rum?mdsource=android&md-api-key=<token>&mdtags=...`

## Why
The Motadata intake reads the API key from the `md-api-key` **query parameter**. The client token
is any opaque string — there is no Datadog-style format validation. All header/param names are
Motadata-owned (`MD-*` / `md*`), so nothing on the wire reveals the upstream origin.
