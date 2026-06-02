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

## State checkpoints

- Baseline (unchanged 3.10.0) on `motadata-dev`: **CI green** (run 26818283373, 6m5s) ✅
