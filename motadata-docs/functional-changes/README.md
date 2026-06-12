# Functional Changes

Behavioural / wire-format changes added on top of the upstream SDK. These are the things the
Motadata backend actually depends on (distinct from the cosmetic rebrand/rename work).

Each doc is self-contained: what changed, where it lives in the code, the wire output, and why.

| # | Change | Doc |
|---|--------|-----|
| 1 | Authentication & request wire contract (`md-api-key` + `MD-*` headers) | [01-authentication-and-wire-contract.md](01-authentication-and-wire-contract.md) |
| 2 | `session.created` timestamp on every event | [02-session-created-timestamp.md](02-session-created-timestamp.md) |
| 3 | `context._timing` object on every event | [03-timing-context.md](03-timing-context.md) |
| 4 | `view.is_view_completed` flag | [04-view-completion-flag.md](04-view-completion-flag.md) |
