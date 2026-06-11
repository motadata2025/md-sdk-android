# WORKFLOW.md — CI Runbook (source of truth for building & publishing the Motadata Android SDK)

**If you want to change the SDK, follow this file top to bottom.** It is the single source of truth for
*how* a change gets from your editor to a published artifact: edit → push → build green → test green →
publish. It also lists everything you need installed locally so a fresh clone can be onboarded fast.

> **Golden rule: nothing compiles on the local laptop.** Every build/test/publish runs on GitHub Actions.
> The dev laptop is memory-constrained (a local Gradle build froze it once). *Editing* files is free;
> only *compiling* is offloaded to CI. Never run `./gradlew <build task>` locally without asking first.

---

## 0. Which doc owns what (don't duplicate)

| Concern | Source of truth |
|---|---|
| **How to build / test / publish** (this pipeline) | **`WORKFLOW.md`** ← you are here |
| What was changed, step by step (history) | `PROGRESS.md` |
| Wire format / event fields & units | `EVENT_TYPES.md` |
| How a client app integrates the SDK | `MOTADATA_ANDROID_CLIENT_SOP_BRANCH-2.md` |
| How a client app upgrades 1.0.0 → 1.0.1 | `MOTADATA_ANDROID_MIGRATION_SOP_1.0.0-to-1.0.1.md` |
| Durable cross-session facts (backstop) | agent memory (`project_motadata_android_rebrand.md`) |

---

## 1. Local prerequisites (onboarding a fresh clone)

You do **not** need a working local Android build (CI does that). You need just enough to *edit*, *push*,
and *drive CI via `gh`*.

1. **Clone the repo:**
   ```bash
   git clone https://github.com/motadata2025/md-sdk-android.git
   cd md-sdk-android
   ```
   The local repo has **two remotes** — confirm with `git remote -v`:
   - `origin`   → `https://github.com/motadata2025/md-sdk-android.git`  ← **ours**
   - `upstream` → `https://github.com/DataDog/dd-sdk-android.git`        ← DataDog (read-only ancestry)

2. **Install the GitHub CLI** (`gh`) — this is how every CI run is triggered and inspected.
   - macOS: `brew install gh` · Debian/Ubuntu: `sudo apt install gh` · Windows: `winget install GitHub.cli`

3. **Authenticate `gh` as the `motadata2025` account** with the right scopes:
   ```bash
   gh auth login                                              # choose github.com, HTTPS, authenticate
   gh auth refresh -h github.com -s repo,workflow,read:packages,delete:packages
   ```
   Required token scopes (verify with `gh auth status`):
   | Scope | Needed for |
   |---|---|
   | `repo` | push, read runs |
   | `workflow` | dispatch the workflows (`gh workflow run`) |
   | `read:packages` | check published versions |
   | `delete:packages` | the delete-and-republish fallback **only** (see §6) |

   > Publishing itself uses the **Actions bot token inside CI** (`secrets.GITHUB_TOKEN`), *not* your local
   > token — so your local token does **not** need `write:packages`. It needs `delete:packages` only if you
   > ever delete a version to republish the same number (the bot token cannot delete user-owned packages).

4. **Git identity / signing:** commits are authored locally as usual. (Upstream's GPG-signing policy is a
   DataDog rule; our CI does not enforce it. Keep your existing commit author.)

5. **`git`, a text editor, and an agent** — that's the whole local toolchain. **No JDK, no Android SDK, no
   Gradle needed locally.**

---

## 2. The change → publish procedure (follow in order)

Each step says who/what runs it and what "green" means. **Do not advance to the next step until the
current one is green.** The human is asked before each push/dispatch.

### Step A — Pick the branch
| Branch | Purpose | Version |
|---|---|---|
| `motadata-dev` | rebrand / debrand only | `1.0.0` |
| `motadata-dev-with-functional-changes` | functional additions | `1.0.1` (current) |

New SDK work continues on `motadata-dev-with-functional-changes` (or a fresh branch cut from it — see
the note in §5 about registering a new branch for auto-build).

### Step B — Edit locally (no build)
Make the code edits. Nothing compiles on the laptop.

### Step C — Commit & push (auto-triggers the build)
```bash
git add <files>
git commit -m "RUM-XXXXX: <short description>"     # ticket prefix per CLAUDE.md
git push origin <branch>
```
- Pushing to `motadata-dev` or `motadata-dev-with-functional-changes` **auto-runs `motadata-build.yml`**.
- For **doc-only** commits, append `[skip ci]` to the message to avoid a pointless build.

### Step D — Watch the build go green  *(compile gate)*
```bash
gh run list  -R motadata2025/md-sdk-android --limit 5
gh run watch <run-id> -R motadata2025/md-sdk-android --exit-status      # blocks until done; non-zero on fail
gh run view  <run-id> -R motadata2025/md-sdk-android --log-failed       # only the failed steps, if red
```
Green here = the 7 shipping modules **compile** (JDK 21, `assembleDebug`, 7 AARs). If red → fix → Step C.

### Step E — Refresh the API surface  *(only if you changed public API / type names)*
```bash
gh workflow run motadata-apidump.yml -R motadata2025/md-sdk-android --ref <branch>
```
JDK 21. Regenerates `api/apiSurface`, `api/<module>.api`, `api/compiler-meta.txt` and **bot-commits them
back** to `<branch>` as `chore: regenerate api surface [skip ci]`. After it finishes, **`git pull`** so your
local branch picks up the bot commit. Skip this step for pure behavior changes that touch no public API.

### Step F — Run unit tests go green  *(behavior gate)*
```bash
gh workflow run motadata-test.yml -R motadata2025/md-sdk-android --ref <branch>
gh run list -R motadata2025/md-sdk-android --limit 5      # find the run id, then watch it
```
**JDK 17** (see §4 — tests break on 21). Runs `testDebugUnitTest --continue` across all 7 modules so one
run surfaces *all* failures; uploads JUnit XML + HTML as the `test-results` artifact. Green = behavior OK.

### Step G — Bump the version  *(only when publishing a NEW released version)*
GitHub Packages versions are **immutable**. To ship a new release, bump:
```
buildSrc/src/main/kotlin/com/datadog/gradle/config/AndroidConfig.kt   →  line 22
    val VERSION = Version(1, 0, 1, Version.Type.Release)   // bump the numbers
```
Commit + push (re-runs the build). If you are *re-publishing the same number* during testing, skip this and
use the delete-and-republish fallback in §6 instead.

### Step H — Publish to GitHub Packages
```bash
gh workflow run motadata-publish.yml -R motadata2025/md-sdk-android --ref <branch>
gh run list -R motadata2025/md-sdk-android --limit 5      # watch it to success
```
JDK 21. Publishes all 7 modules (release variant) as `com.motadata:motadata-rum-android*:<version>` with
`-Pdd-skip-signing`. Uses the in-CI bot token (`packages: write`).

### Step I — Verify the publish
```bash
gh api /users/motadata2025/packages/maven/com.motadata.motadata-rum-android/versions \
  -q '.[].name'                                          # should list the new version
```
The test app then consumes it with `--refresh-dependencies` (see the migration SOP).

---

## 3. The five workflows (reference)

All live in `.github/workflows/`. All are branch-agnostic — they check out `${{ github.ref_name }}`, i.e.
**whatever branch you dispatch with `--ref`** (publish/publish-central/test/apidump) or push to (build).

| Workflow | Trigger | JDK | What it does |
|---|---|---|---|
| `motadata-build.yml` | **push** (both branches) + dispatch | **21** | `assembleDebug` of 7 modules → 7 AARs (compile gate) |
| `motadata-test.yml` | dispatch | **17** | `testDebugUnitTest --continue` ×7, uploads reports (behavior gate) |
| `motadata-apidump.yml` | dispatch | **21** | regen api surface, **bot-commits back** (`contents: write`) |
| `motadata-publish.yml` | dispatch | **21** | publish 7 modules → **GitHub Packages** (`packages: write`, `-Pdd-skip-signing` = unsigned) |
| `motadata-publish-central.yml` | dispatch | **21** | publish 7 modules → **Maven Central** (GPG-signed, staging + close, manual release). See §7 |

**The 7 shipping modules** (the closure published & built):
`dd-sdk-android-core`, `dd-sdk-android-internal`, `features:dd-sdk-android-rum`,
`features:dd-sdk-android-trace`, `features:dd-sdk-android-trace-api`,
`features:dd-sdk-android-trace-internal`, `integrations:dd-sdk-android-okhttp`.

---

## 4. Invariants & gotchas (these decide green-vs-red — read before touching CI)

1. **JDK split is mandatory.** Tests run on **JDK 17**; build / apidump / publish on **JDK 21**.
   The test utility `com.datadog.tools.unit.RemoveFinalModifier` strips a field's `final` via a VarHandle on
   `Field.modifiers`, which throws `UnsupportedOperationException` on JDK 21. Don't "unify" the JDKs.

2. **Dispatch resolves on `develop`, but executes your `--ref`.** `gh workflow run <file>` looks the
   workflow up **only on the repo default branch (`develop`)** — so the three dispatch-only files
   (`test`, `apidump`, `publish`) are also committed to `develop` (they never auto-run there). The copy that
   *actually runs* is the one on your `--ref` branch, and it checks out that branch. **`build.yml` is
   intentionally NOT on `develop`** — it's push-triggered, so it never needs dispatch resolution.
   *If you ever edit a dispatch workflow, update the `develop` copy too, or `gh workflow run` can't find it.*

3. **Always pass `-R motadata2025/md-sdk-android` to `gh`.** The local clone also has DataDog's remote
   (`upstream`); without `-R`, `gh` may query DataDog's repo and show *their* runs.

4. **GitHub Packages versions are immutable.** Re-publishing an existing version → `409 Conflict`. Ship a
   **new** version (Step G) or use the delete-and-republish fallback (§6).

5. **Branch → version mapping:** `motadata-dev` = `1.0.0`, `motadata-dev-with-functional-changes` = `1.0.1`.
   Dispatch publish on the branch whose version you intend to ship.

6. **Read the real conclusion via `gh`,** not a piped `tail`/`grep` exit code. Use
   `gh run watch <id> --exit-status` (non-zero on failure) and `gh run view <id> --log-failed` for details.

7. **CodeQL on push is DataDog's leftover workflow** — ignore it; only **"Motadata SDK Build"** matters.

8. **Gradle wrapper download flake (infra, not your code):** runs occasionally fail with
   `Downloading …/gradle-9.x-all.zip failed: timeout` / `504`. Just `gh run rerun <id> -R motadata2025/md-sdk-android`.
   If it becomes chronic, switch the wrapper from `-all` → `-bin` in `gradle/wrapper/gradle-wrapper.properties`.

---

## 5. Adding a new long-lived branch (optional)

`motadata-build.yml` only auto-builds the two branches in its `on.push.branches` list. To get auto-build on
a new branch, add it to that list. To run **test / apidump / publish** on a new branch, no change is needed —
they're branch-agnostic; just dispatch with `--ref <new-branch>` (the workflow files already exist on
`develop`, which is all the dispatch resolver needs).

---

## 6. Republishing the SAME version (delete-and-republish fallback)

Preferred path is **bump the version** (Step G) — it's immutable-safe and keeps history. Use this fallback
only if you must keep the same number (e.g. fixing a bad `1.0.1` before anyone consumed it):

1. **One-time:** ensure your local `gh` token has delete rights (interactive, as `motadata2025`):
   ```bash
   gh auth refresh -h github.com -s delete:packages,read:packages
   ```
   (Both scopes required. The CI bot token **cannot** delete user-owned packages — must be the local
   `motadata2025` user token.)
2. **Delete all 7 packages' offending version** (deleting the package removes its only version):
   ```bash
   for p in motadata-rum-android motadata-rum-android-okhttp motadata-rum-android-core \
            motadata-rum-android-internal motadata-rum-android-trace \
            motadata-rum-android-trace-api motadata-rum-android-trace-internal; do
     gh api -X DELETE "/user/packages/maven/com.motadata.$p"
   done
   ```
   Confirm gone: `gh api /user/packages/maven/com.motadata.motadata-rum-android` → `404`.
3. **Republish:** `gh workflow run motadata-publish.yml -R motadata2025/md-sdk-android --ref <branch>` →
   wait for success → verify `…/versions`. (Maven on GH Packages allows re-publishing a *deleted* version.)
4. **Test app re-fetches** the same coordinate with `gradlew <task> --refresh-dependencies` (Gradle caches
   it otherwise).

---

## 7. Publishing to Maven Central (public releases)

**GitHub Packages (§6) = dev loop** (fast, deletable, needs a PAT to consume).
**Maven Central = public release** (signed, immutable, consumable with **no token** — `mavenCentral()` is
default in every Gradle build). Use Central only for **deliberate final releases**, never every iteration
(Central versions cannot be deleted or reused). Same coordinates + **same version** as GitHub Packages —
keep them unified: `com.motadata:motadata-rum-android*:1.0.1`.

### How it's wired (already in place — don't re-invent)
- Plugin: **`io.github.gradle-nexus.publish-plugin`** in root `build.gradle.kts` (`nexusPublishing {}`) →
  Sonatype **Central Portal** via the OSSRH Staging API (`ossrh-staging-api.central.sonatype.com`).
- **Signing required**: `MavenConfig.kt` (`useInMemoryPgpKeys`); the Central workflow does **NOT** pass
  `-Pdd-skip-signing` (Central rejects unsigned artifacts). The POM (name/license/scm/developers) is complete.
- **Root `group = MavenConfig.GROUP_ID` (`com.motadata`)** in `build.gradle.kts` — REQUIRED. The nexus plugin
  auto-resolves the staging profile by matching this group; if it's empty,
  `initializeSonatypeStagingRepository` fails with *"Failed to find staging profile for package group:"*.
  **Do not remove it.** (The old DataDog `stagingProfileId` was removed so the profile auto-resolves for us.)

### One-time account setup → 4 GitHub repo secrets (full guide: `MAVEN_CENTRAL_SETUP.md`)
Namespace `com.motadata` is verified on central.sonatype.com (DNS TXT on motadata.com). The account creator
produces 4 secrets, loaded at **Settings → Secrets and variables → Actions**:
| Repo secret | Purpose |
|---|---|
| `GPG_PRIVATE_KEY` | signing key (full `private-key.asc`, BEGIN→END) — read by `MavenConfig.kt` |
| `GPG_PASSWORD` | GPG passphrase — read by `MavenConfig.kt` |
| `CENTRAL_USERNAME` | Central Portal token user — workflow maps it to env `CENTRAL_PUBLISHER_USERNAME` |
| `CENTRAL_PASSWORD` | Central Portal token pass — workflow maps it to env `CENTRAL_PUBLISHER_PASSWORD` |
> The build reads `CENTRAL_PUBLISHER_*`; the workflow maps the repo-secret names to those env vars. The GPG
> **public** key must be on a keyserver (`gpg --send-keys`) so Central can verify signatures.

### Publish procedure
1. Confirm the version is what you intend to ship publicly (**immutable**). Same number as GitHub Packages.
2. Dispatch (its `develop` shim makes it resolvable — §4 invariant #2):
   ```bash
   gh workflow run motadata-publish-central.yml -R motadata2025/md-sdk-android --ref <branch>
   ```
   JDK 21. GPG-signs + uploads the 7 modules to a Sonatype **staging** repo and **closes** it (validates).
   It does **not** auto-release.
3. Watch it green: `gh run watch <id> -R motadata2025/md-sdk-android --exit-status`.
4. **Manual release — the only irreversible step:** central.sonatype.com → **Deployments** → find the
   `com.motadata … <version>` deployment → **Publish** (or **Drop** to cancel; reversible until Publish).
5. Syncs to Maven Central, searchable in **~15–30 min**.

### Consuming from Maven Central (app side)
Swap the GitHub Packages repo block for `mavenCentral()`; **leave the `implementation(...)` lines unchanged**
(identical coordinates, no PAT):
```kotlin
repositories { mavenCentral() }     // delete the maven.pkg.github.com block + its PAT credentials
implementation("com.motadata:motadata-rum-android:1.0.1")   // unchanged
```

### First-run gotchas (already hit & fixed — kept here so nobody re-debugs them)
- *"Failed to find staging profile for package group:"* (empty group) → set root `group = MavenConfig.GROUP_ID`. ✅ fixed.
- DataDog's hardcoded `stagingProfileId` → removed so it auto-resolves for `com.motadata`. ✅ fixed.
- *"no public key"* at validation → the GPG public key hadn't propagated; re-run `gpg --send-keys`, wait, re-dispatch.

---

## 8. `gh` cheat-sheet

```bash
# trigger a dispatch workflow on a branch
gh workflow run <file>.yml -R motadata2025/md-sdk-android --ref <branch>

# list recent runs / watch one / read failures
gh run list  -R motadata2025/md-sdk-android --limit 5
gh run watch <run-id> -R motadata2025/md-sdk-android --exit-status
gh run view  <run-id> -R motadata2025/md-sdk-android --log-failed
gh run rerun <run-id> -R motadata2025/md-sdk-android        # for the gradle-download flake

# list published versions of a package (GitHub Packages)
gh api /users/motadata2025/packages/maven/com.motadata.motadata-rum-android/versions -q '.[].name'

# verify a version is live on Maven Central (after Portal "Publish" + sync)
curl -sI https://repo1.maven.org/maven2/com/motadata/motadata-rum-android/1.0.1/motadata-rum-android-1.0.1.aar | head -1
```

---

## 9. State checkpoints

- Baseline (unchanged 3.10.0) on `motadata-dev`: CI green (run 26818283373) ✅
- Branch 1 (`motadata-dev`): rebrand complete, published `com.motadata:*:1.0.0` (GitHub Packages) ✅
- Branch 2 (`motadata-dev-with-functional-changes`): functional additions complete, published `*:1.0.1` (GitHub Packages) ✅
- Maven Central: `com.motadata:*:1.0.1` published from Branch 2 (GPG-signed, via Central Portal) ✅
