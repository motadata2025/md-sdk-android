# Maven Central Account Setup — `com.motadata`

**Goal:** set up our Maven Central publishing account so we can publish the Android SDK under the namespace **`com.motadata`** (e.g. `com.motadata:motadata-rum-android`). This is **account setup only — no code work.**

**Namespace :** `com.motadata` — verified by adding a DNS record to **motadata.com**. You will need someone with access to **motadata.com DNS** (our IT / domain admin) to add one TXT record. Arrange that before you start.

**What you deliver at the end:** the 4 secrets in §5. Hand them to engineering team.

**Time:** ~30 min of work; allow up to a day total because DNS propagation and GPG key propagation involve waiting.

---

## 1. Create the Sonatype Central account
1. Open **https://central.sonatype.com**
2. Click **Sign In → Sign Up**. Register with our **company Google/GitHub account** (or a company email).
3. Verify the email if asked.

---

## 2. Register and verify the `com.motadata` namespace
1. In the Portal, open **View Namespaces → Add Namespace**.
2. Enter exactly: `com.motadata`
3. The Portal shows a **TXT record value** (a verification code string).
4. Add a **DNS TXT record to `motadata.com`** with that value:
   - Host/Name: `motadata.com` (root domain)
   - Type: `TXT`
   - Value: the code from step 3
   - (Do this in our DNS provider's console — Cloudflare/GoDaddy/Route53/etc. Ask IT/domain admin to add it if you don't have access.)
5. Back in the Portal, click **Verify**. Wait until status = **Verified** (DNS can take minutes to a few hours). Re-click **Verify** until it turns green.

---

## 3. Generate the GPG signing key
Maven Central requires every file to be GPG-signed. Run on your machine (install GnuPG first: `sudo apt install gnupg` on Linux, `brew install gnupg` on Mac).

1. Generate the key:
   ```bash
   gpg --gen-key
   ```
   - Real name: `Motadata`
   - Email: our company email
   - **Passphrase:** set one and **write it down** — this is secret #2.

2. Get the KEY_ID:
   ```bash
   gpg --list-secret-keys --keyid-format=long
   ```
   Copy the value after `rsa3072/` (or similar) on the `sec` line — that is your **KEY_ID**.

3. Publish the public key to the keyserver:
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```

4. Export the private key (secret #1):
   ```bash
   gpg --armor --export-secret-keys <KEY_ID> > private-key.asc
   ```
   Keep `private-key.asc` secret — treat it like a password.

---

## 4. Generate the publishing token
1. In the Portal: **account name → View Account → Generate User Token**.
2. Copy the **username** and **password** shown (the password is shown only once — secrets #3 and #4).

---

## 5. Hand these to engineering team

| # | Item | Where it came from |
|---|---|---|
| 1 | GPG private key — contents of `private-key.asc` | §3.4 |
| 2 | GPG key passphrase | §3.1 |
| 3 | Central Portal token — username | §4 |
| 4 | Central Portal token — password | §4 |

Namespace is fixed (`com.motadata`), so nothing else to report.

---

## 6. Done-checklist
- [ ] Confirmed who can add a TXT record to motadata.com DNS
- [ ] Sonatype Central account created (central.sonatype.com)
- [ ] `com.motadata` namespace added and status = **Verified**
- [ ] GPG key generated; public key sent to `keyserver.ubuntu.com`
- [ ] `private-key.asc` exported + passphrase written down
- [ ] Central Portal user token generated (username + password copied)
- [ ] All 4 secrets handed to engineering securely

---

## 7. If something fails
- **Namespace won't verify** → the TXT record isn't live yet or the value is wrong. Confirm the record with `dig TXT motadata.com` (or `nslookup -type=TXT motadata.com`), wait for propagation, click **Verify** again.
- **Signature/"no public key" error later** → the public key wasn't sent or hasn't propagated; re-run §3.3 and wait a few hours.
- **Lost token password** → generate a new token (§4) and revoke the old one.
- Use **only** central.sonatype.com. The old `oss.sonatype.org` / JIRA process is deprecated — ignore guides that mention it.

*Reference: https://central.sonatype.org/register/central-portal/*
