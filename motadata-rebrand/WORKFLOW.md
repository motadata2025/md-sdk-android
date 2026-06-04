# Dev Workflow — CI-based (no local builds)

**Golden rule: nothing compiles on the local laptop.** All builds run on GitHub Actions.
The laptop is memory-constrained (a local Gradle build froze it once). Editing files is free; only
*compiling* is offloaded to CI.

## The loop (per change)

```
1. EDIT     Claude makes code edits locally (rename / scrub / functional). No build. No laptop load.
2. COMMIT   git add … && git commit            (on the active branch)
3. PUSH     git push origin <branch>            → this auto-triggers the GitHub build
4. BUILD    GitHub Actions "Motadata SDK Build" runs on ubuntu-latest (16 GB runner):
               JDK 21 → Android SDK → ./gradlew :core:assembleDebug :rum:assembleDebug :okhttp:assembleDebug
5. CHECK    Claude reads the result with gh and reports pass/fail + logs:
               gh run list  -R motadata2025/md-sdk-android --limit 5
               gh run watch <run-id> -R motadata2025/md-sdk-android --exit-status
               gh run view  <run-id> -R motadata2025/md-sdk-android --log-failed   # only failed steps
6. FIX      If red → Claude fixes the edit → back to step 2. If green → move to next change.
```

This is automated end-to-end: **edit → push → build → check**. Claude drives all of it and reports;
the user can interject anytime.

## Important gotchas (learned)

- **Always pass `-R motadata2025/md-sdk-android` to `gh`.** The local repo has two remotes
  (`origin`=fork, `upstream`=DataDog); without `-R`, gh may query DataDog's repo and show *their* runs.
- **Don't run `./gradlew` locally** (it OOM-freezes the laptop). If a local build is ever unavoidable,
  ask the user first, then: `JAVA_TOOL_OPTIONS="" ./gradlew :module:assembleDebug --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx2g` with the IDE closed.
- **CodeQL also runs on push** — that's DataDog's leftover workflow. Ignore it; only "Motadata SDK Build" matters.
- **Real exit code:** read the run conclusion via `gh`, not a piped `tail`/`grep` exit.

## Branches (see REBRAND_PLAN EXECUTION section)

- `motadata-dev` — **rename / debrand only** (current). Build must stay green after each batch.
- `motadata-dev-with-functional-changes` — cut from it later; **functional additions only**.
- The workflow `motadata-build.yml` triggers on push to **both** branches.

## Build artifacts / publishing (later)

The CI build produces AARs (`*/build/outputs/aar/*.aar`). When ready to ship, the same workflow is
extended to `publishToMavenLocal` / publish to GitHub Packages or Maven Central on a tagged release.

## CI workflows (all `workflow_dispatch`-capable)

All live on `motadata-dev`. **Gotcha:** `gh workflow run <file>` resolves the workflow only on the
repo **default branch (`develop`)**, so each dispatch-only workflow file is also committed to
`develop` (it never auto-runs there). Dispatch with `--ref motadata-dev`; the run uses the
motadata-dev copy + checks out motadata-dev.

| Workflow | Does | JDK | Trigger |
|---|---|---|---|
| `motadata-build.yml` | `assembleDebug` of 7 modules (7 AARs) | 21 | push + dispatch |
| `motadata-test.yml` | `testDebugUnitTest --continue` of 7 modules | **17** (RemoveFinalModifier reflection breaks on 21) | dispatch |
| `motadata-apidump.yml` | regenerate api surface, commit back (`permissions: contents:write`) | 21 | dispatch |
| `motadata-publish.yml` | publish 7 modules to GitHub Packages, `-Pdd-skip-signing` (`permissions: packages:write`) | 21 | dispatch |

Run: `gh workflow run <file> -R motadata2025/md-sdk-android --ref motadata-dev`.

## Publishing to GitHub Packages (testing distribution)

Published as `com.motadata:motadata-rum-android*:1.0.0` (public packages, owner `motadata2025`).
Consumed by the test app per `WINDOWS_TEST_AGENT_GUIDE.md`.

### Republishing the SAME version (1.0.0) during testing — the SOP

We keep the version at **`1.0.0`** while iterating (no bumping). But **GitHub Packages rejects
overwriting an existing release version** (`PUT … → 409 Conflict`). So to push a fixed `1.0.0`:

1. **Grant the local `gh` token package-delete rights once** (interactive, approve as `motadata2025`):
   ```
   gh auth refresh -h github.com -s delete:packages,read:packages
   ```
   (Deletion needs BOTH scopes. The Actions bot token CANNOT delete user-owned packages, so this
   must be a `motadata2025` user token — i.e. the local keyring token, not CI.)
2. **Delete all 7 packages** (each has only the `1.0.0` version; deleting the package removes it):
   ```
   for p in motadata-rum-android motadata-rum-android-okhttp motadata-rum-android-core \
            motadata-rum-android-internal motadata-rum-android-trace \
            motadata-rum-android-trace-api motadata-rum-android-trace-internal; do
     gh api -X DELETE "/user/packages/maven/com.motadata.$p"
   done
   ```
   (Confirm gone: `gh api /user/packages/maven/com.motadata.motadata-rum-android` → 404.)
3. **Republish:** `gh workflow run motadata-publish.yml -R motadata2025/md-sdk-android --ref motadata-dev`
   → wait for success → verify `…/versions` shows `1.0.0` again. (Maven on GH Packages DOES allow
   re-publishing a version that was deleted — verified 2026-06-04.)
4. **Test app must re-fetch** (same coordinate ⇒ Gradle has it cached): build with
   `gradlew.bat <task> --refresh-dependencies` (or delete
   `…\.gradle\caches\modules-2\files-2.1\com.motadata\`).

> If iteration churn grows, a one-time bump to `1.0.1` removes steps 1–2 entirely (just republish +
> change the dep version). We stay on `1.0.0` deliberately for now.

## State checkpoints

- Baseline (unchanged 3.10.0) on `motadata-dev`: **CI green** (run 26818283373, 6m5s) ✅
