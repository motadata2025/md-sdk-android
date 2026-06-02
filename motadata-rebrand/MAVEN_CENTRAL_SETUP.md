# Maven Central Account Setup — Step-by-Step

**Who this is for:** the person setting up our Maven Central publishing account.
**Why:** we are publishing an Android SDK library (Maven coordinates like `com.motadata:motadata-rum-android`) so that other developers can install it with one Gradle line. Maven Central is the public registry Android/Java projects pull from by default. This guide gets the **account, namespace, signing key, and token** ready. You do **not** need to touch any code — this is account setup only.

**What you produce at the end:** four secrets that you hand back to the engineering team (listed in §6). Keep them safe.

**Time:** ~30–45 min of work, but **start early** — two steps (namespace verification + key propagation) involve waiting (a few hours up to ~2 days).

---

## 0. One decision to confirm with the team first: the namespace

Maven Central artifacts are grouped under a **namespace** (the `groupId`, e.g. `com.motadata`). You must **prove you own it**. Two ways:

| Namespace | How you prove ownership | When to use |
|---|---|---|
| **`com.motadata`** | Add a TXT record to **motadata.com DNS** | If we control motadata.com's DNS (preferred — most professional) |
| **`io.github.motadata2025`** | Create a GitHub repo Sonatype names | Free & fast; if we don't have DNS access |

👉 **Confirm with the team which one to register.** If unsure or you don't have DNS access, use **`io.github.motadata2025`** (instant, no domain needed). Everything below works the same for either.

---

## 1. Create a Sonatype Central account  (~5 min)

1. Go to **https://central.sonatype.com**
2. Click **Sign In** → register (you can log in with **GitHub** or **Google**, or email).
3. Verify your email if prompted.

> Note: the old "OSSRH / s01.oss.sonatype.org" system is deprecated. We use the **Central Portal** (central.sonatype.com). Ignore older guides that mention JIRA tickets.

---

## 2. Register & verify the namespace  (⏱️ has waiting time — do this first)

1. In the Central Portal, go to **View Namespaces** → **Add Namespace**.
2. Enter the namespace from §0:
   - `com.motadata`  **or**  `io.github.motadata2025`
3. Sonatype shows a **verification method**:

   **If `com.motadata` (DNS):**
   - It gives you a **TXT record** value (a verification code).
   - Add a DNS **TXT record** to `motadata.com` with that value (whoever manages our DNS does this — Cloudflare/GoDaddy/etc.).
   - Back in the Portal, click **Verify**. (DNS can take minutes to a few hours to propagate.)

   **If `io.github.motadata2025` (GitHub):**
   - It tells you to create a **public GitHub repository** with a specific name (a temporary verification code) under the `motadata2025` account.
   - Create that empty public repo. Click **Verify**. (Usually instant.)

4. Wait until the namespace status shows **Verified**. ✅

---

## 3. Generate a GPG signing key  (⏱️ key propagation takes time)

Maven Central **requires every uploaded file to be GPG-signed**. Create a key and publish its public half.

> Do this on a trusted machine. Install GnuPG first if needed (`sudo apt install gnupg` on Linux, `brew install gnupg` on Mac).

1. **Generate the key:**
   ```bash
   gpg --gen-key
   ```
   - Enter a real name + email (use a team/company email if possible).
   - Set a **passphrase** — **write it down**, you'll hand it over.

2. **Find your key ID:**
   ```bash
   gpg --list-secret-keys --keyid-format=long
   ```
   Look for a line like `sec   rsa3072/ABCDEF1234567890` — the part after the `/` is your **KEY_ID**.

3. **Publish the public key to a keyserver** (so Maven Central can verify signatures):
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```
   (Propagation can take a few hours.)

4. **Export the private key** (the engineering team needs this for automated signing in CI):
   ```bash
   gpg --armor --export-secret-keys <KEY_ID> > private-key.asc
   ```
   This `private-key.asc` file + the passphrase are two of the four secrets (see §6). **Treat them like a password.**

---

## 4. Create a publishing token  (~2 min)

1. In the Central Portal, click your **account name → View Account → Generate User Token**.
2. It gives you a **username** and a **password** (a token pair) for the publishing API.
3. **Copy both immediately** — the password is shown only once.

---

## 5. (Optional) note the namespace you registered
Write down the exact namespace string (`com.motadata` or `io.github.motadata2025`) — the engineering team needs it to set the library's `groupId`.

---

## 6. Hand these back to the engineering team (securely)

Share via a password manager / secrets vault — **never** email or commit to git:

| # | Item | From step |
|---|---|---|
| 1 | **GPG private key** (`private-key.asc` contents) | §3.4 |
| 2 | **GPG key passphrase** | §3.1 |
| 3 | **Central Portal token — username** | §4 |
| 4 | **Central Portal token — password** | §4 |
| 5 | The **namespace** string you registered (`com.motadata` / `io.github.motadata2025`) | §2 |

The engineering team plugs these into the CI pipeline (as `GPG_PRIVATE_KEY`, `GPG_PASSWORD`, `CENTRAL_PUBLISHER_USERNAME`, `CENTRAL_PUBLISHER_PASSWORD`) and the build publishes automatically.

---

## 7. Checklist

- [ ] Decided namespace with the team (`com.motadata` or `io.github.motadata2025`)
- [ ] Sonatype Central account created (central.sonatype.com)
- [ ] Namespace added **and Verified** (DNS TXT or GitHub repo)
- [ ] GPG key generated; **public key sent to keyserver.ubuntu.com**
- [ ] GPG private key exported (`private-key.asc`) + passphrase noted
- [ ] Central Portal user token generated (username + password copied)
- [ ] All 5 items handed to engineering via a secure channel

---

## 8. Common pitfalls
- **"Namespace not verified" when publishing** → the DNS TXT / GitHub verify step (§2) didn't complete; re-check status in the Portal.
- **"No public key" / signature errors** → the public key wasn't sent to the keyserver, or hasn't propagated yet (wait a few hours); re-run §3.3.
- **Lost the token password** → just generate a new token (§4); the old one can be revoked.
- **Don't use** the legacy `oss.sonatype.org` / JIRA flow — that's deprecated; use **central.sonatype.com**.

---

*Reference: Sonatype Central Portal docs — https://central.sonatype.org/register/central-portal/*
