# Motadata Rebrand — working docs

Docs needed to rebrand this `dd-sdk-android` fork into the **Motadata Android RUM SDK**.
Copied here so we don't go back and forth to the Motadata repo. **Start with the plan.**

> ## ▶ RESUME POINT (read this first if you're a new session)
> **Goal:** fork `dd-sdk-android` 3.10.0 → `com.motadata:motadata-rum-android` (+ closure), zero `datadog`/`dd`/`DD-` anywhere customer/server-facing (only the legal Apache source header stays). Feeds Motadata's custom RUM endpoint. Mirrors the `motadata2025/browser-sdk` fork.
> **Plan (single source of truth):** `MOTADATA_ANDROID_SDK_REBRAND_PLAN.md` here. **Loop:** `WORKFLOW.md` here.
> **Branch model:** `motadata-dev` = ALL debranding/renames only (incl. `_dd`→`_md` via in-repo JSON schemas). `motadata-dev-with-functional-changes` (cut from it) = functional ADDITIONS only (`md-api-key` query, `is_view_completed`, `session.created`, `context._timing`, public `allowClearTextHttp()`, init log).
> **Builds:** GitHub Actions only (`gh` authed as motadata2025; `gh ... -R motadata2025/md-sdk-android`). NEVER build locally (laptop hangs). Edit → push → CI builds 7-module closure → read logs.
> **CURRENT STATUS / NEXT STEP:** see **`PROGRESS.md`** (the living step-by-step tracker — updated after every step). As of now: step 1/9 (package rename) ✅ CI-green; next is step 2 (class renames, plan §1A).

| File | What it is | Use it for |
|---|---|---|
| **`PROGRESS.md`** | 📍 Living progress tracker — status of every step (✅/🔄/⬜), commits, CI runs, decisions | **Where we are right now.** Check/update this first. |
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
