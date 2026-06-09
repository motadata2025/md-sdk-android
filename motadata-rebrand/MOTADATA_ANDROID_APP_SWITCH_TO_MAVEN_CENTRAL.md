# Motadata Android RUM SDK — App Switch: GitHub Packages → Maven Central

**Goal:** make the app pull the Motadata SDK from **Maven Central** instead of **GitHub Packages**.
`com.motadata:motadata-rum-android*:1.0.1` is now published on Maven Central (public, GPG-signed). Maven
Central is a **default Gradle repository**, so the app no longer needs the GitHub Packages repo block or a
GitHub Personal Access Token (PAT).

> **This is a repository-source change only.** The **coordinates and version are identical**
> (`com.motadata:…:1.0.1`), and the Maven Central artifact is built from the **same source** as the
> GitHub Packages `1.0.1` you already tested — so the app's behaviour and the RUM events it emits are
> **unchanged**. Do **not** change any `implementation(...)` line or any SDK init/instrumentation code.

> **Roles:**
> **Part A** — the AI agent edits the Gradle config and builds.
> **Part B** — the human runs the app, captures RUM events, and confirms it still works.

---

## 0. What changes (and what doesn't)

| | Before (GitHub Packages) | After (Maven Central) |
|---|---|---|
| Repository | `maven { url = "https://maven.pkg.github.com/motadata2025/md-sdk-android" … }` + PAT credentials | `mavenCentral()` (no credentials) |
| GitHub PAT | required (`gpr.user` / `gpr.key` or env) | **not needed** — remove |
| Dependency coordinates | `com.motadata:motadata-rum-android*:1.0.1` | **identical — unchanged** |
| SDK init / instrumentation code | — | **unchanged** |
| App behaviour / RUM events | — | **unchanged** |

---

## Part A — Agent: swap the repository, then build

### A-1. Find where the GitHub Packages repo is declared
It is in **one** of these (search the whole project for `maven.pkg.github.com`):
- `settings.gradle.kts` / `settings.gradle` → inside `dependencyResolutionManagement { repositories { … } }`
  *(most common in modern projects)*, **or**
- root `build.gradle(.kts)` → `allprojects { repositories { … } }`, **or**
- app-module `build.gradle(.kts)` → `repositories { … }`.

```bash
grep -rn "maven.pkg.github.com" .          # locate the repo block
grep -rn "gpr.user\|gpr.key\|GITHUB_TOKEN\|maven.pkg.github" . settings.gradle* gradle.properties ~/.gradle/gradle.properties 2>/dev/null
```

### A-2. Remove the GitHub Packages repo block
Delete the entire `maven { … maven.pkg.github.com … credentials { … } }` block. Example:

**Before:**
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {                                                   // ← remove this whole block
            url = uri("https://maven.pkg.github.com/motadata2025/md-sdk-android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

**After:**
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()        // ← Maven Central serves com.motadata:* now; nothing else needed
    }
}
```

> Ensure **`mavenCentral()` is present** in that repositories block (it almost always already is). If it
> isn't, add it. `google()` must stay — it serves the AndroidX/Android Gradle dependencies.

### A-3. Remove the now-unused GitHub PAT
- In `gradle.properties` (project and/or `~/.gradle/gradle.properties`): delete the `gpr.user` / `gpr.key`
  (or similarly named) lines used **only** for GitHub Packages.
- Remove any `GITHUB_ACTOR` / `GITHUB_TOKEN` env wiring that existed only for this repo.
> Leaving an unused PAT behind is harmless (nothing reads it anymore), but remove it for cleanliness.
> Do **not** remove credentials used for anything else.

### A-4. Do NOT touch the dependencies
Every `implementation("com.motadata:…:1.0.1")` line stays **exactly as is**. Same group, artifact, version.

### A-5. Force a clean re-resolve and build
Because the source repository changed, make Gradle re-fetch from Maven Central:
```
gradlew.bat assembleDebug --refresh-dependencies
```
*(or delete `…\.gradle\caches\modules-2\files-2.1\com.motadata\` then build).*

Confirm the build succeeds and that resolution **no longer requires any GitHub token**. Optional sanity check:
```
gradlew.bat :app:dependencyInsight --configuration debugRuntimeClasspath --dependency com.motadata:motadata-rum-android
```
It should resolve `1.0.1` from Maven Central / `repo1.maven.org` (not `maven.pkg.github.com`).

---

## Part B — Human: run, capture, verify

### B-1. Run + smoke-test
Install and launch the app. In Logcat filtered to the Motadata tag:
```
adb logcat -s Motadata
```
You should still see **`Motadata SDK initialized`** at startup — same as the GitHub Packages `1.0.1` build.

### B-2. Exercise the app + capture RUM batches
Hit the same buttons/flows and capture the outgoing RUM batches the same way you did for the `1.0.1` GitHub
Packages run, so the two are directly comparable.

### B-3. Verify nothing changed
Because Maven Central `1.0.1` is the same code as GitHub Packages `1.0.1`, the captures must look **identical**:
- `md-api-key` query param present on requests
- `view.is_view_completed` (`"no"`→`"yes"`) on view events
- `session.created` (epoch-ms) + `context._timing { navigationStart, relativeTime }` on every event
- all 7 event types, no `_dd`/datadog leakage

This run is a **confirmation that sourcing from Maven Central behaves identically**, not a search for new behaviour.

### B-4. Share the capture location
Hand back the new capture batch location so the Maven-Central-sourced build can be confirmed against the
GitHub Packages `1.0.1` capture, and the final SOP can be locked.

---

## Quick checklist
- [ ] A-1: located the `maven.pkg.github.com` repo block
- [ ] A-2: removed the GitHub Packages repo block; `mavenCentral()` present (`google()` kept)
- [ ] A-3: removed the unused GitHub PAT (`gpr.user`/`gpr.key` etc.)
- [ ] A-4: dependency coordinates left unchanged (`com.motadata:…:1.0.1`)
- [ ] A-5: `assembleDebug --refresh-dependencies` builds green, resolves from Maven Central, no token needed
- [ ] B-1: Logcat shows `Motadata SDK initialized`
- [ ] B-2: captured RUM batches
- [ ] B-3: 4 Branch-2 fields present, identical to the GitHub Packages `1.0.1` capture
- [ ] B-4: shared the capture location

That's the whole switch — remove the GitHub repo + token, keep everything else, and it pulls from Maven Central by default.
