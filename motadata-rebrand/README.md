# Motadata Rebrand — working docs

Docs needed to rebrand this `dd-sdk-android` fork into the **Motadata Android RUM SDK**.
Copied here so we don't go back and forth to the Motadata repo. **Start with the plan.**

| File | What it is | Use it for |
|---|---|---|
| **`MOTADATA_ANDROID_SDK_REBRAND_PLAN.md`** | ⭐ The rebrand plan — every change with exact `file:line` | **The checklist we execute.** Part 1 = scrub all `datadog` (imports, in-event strings, wire, Maven); Part 2 = custom use-cases (`md-api-key`, `is_view_completed`, http/https); Parts 3-6 = verify/publish/CI. |
| `MOTADATA_ANDROID_RUM_ONBOARDING.md` | How a customer installs & uses the rebranded SDK | The **target behavior** — what must work after rebrand (init snippet, `MotadataInterceptor` for resource events, HTTP/HTTPS, verification steps). |
| `EVENT_TYPES.md` | Field-by-field reference of every RUM event the SDK emits | Verifying the **in-event scrub** (Part 1B) — confirm no `datadog` string lands in `view.url`, `telemetry.service`, etc. |

## The goal in one line
Fork frozen at **3.10.0** → publish `com.motadata:motadata-rum-android:1.0.0` (+ `-okhttp`) with **zero `datadog`/`dd-sdk`/`DD-`** in imports, class names, event payloads, headers/query, or logs — only the Apache-2.0 source-header attribution stays (license-required).

## First moves when we start
1. Package rename `com.datadog.android.*` → `com.motadata.android.*` (IDE refactor).
2. In-event string constants (plan Part 1B) — the ones a type-rename misses.
3. Functional: `md-api-key` query, `is_view_completed`, public `allowClearTextHttp()`.
4. Maven coords + POM (plan Part 1D), then `./gradlew apiDumpAll` + build/test.

> Not copied (server/dashboard side, out of scope for the SDK rebrand): `METRICS.md`,
> `ANDROID_VS_REACT_NATIVE.md`, `REACT_NATIVE_INSTRUMENTATION_STEPS.md`, and the project-root
> `mobile-rum-architecture-report.md`. Ask if you want any pulled in too.
