---
name: sling-release
description: Automates releasing an Apache Sling project end-to-end using Maven and the sling-cli Docker tool. Use when the user wants to release a Sling project, start a release, perform any step of the Apache Sling release process, or asks about releasing/voting/promoting a Sling module.
tools: Bash, Read, Edit, Write
---

This skill releases an Apache Sling module end-to-end by combining **Maven** steps (run in the
project being released) with the **`apache/sling-cli`** Docker tool (this repo) for the
Nexus/vote/finalize steps.

## Overview

Three phases:
1. **Stage** — Maven prepares the release and stages signed artifacts to Nexus.
2. **Vote** — close the staging repo, verify, send the `[VOTE]` email, wait 72h, tally.
3. **Finalize** — after a passing vote: promote to Maven Central, update JIRA + Reporter, and (for
   PMC members) publish to `dist.apache.org`.

## Prerequisites (verify before starting)

1. **Docker** is available, and the `apache/sling-cli` image is built/pulled.
2. **`~/.apache-committer`** (or any env file) with ASF credentials, passed to the container:
   ```
   ASF_USERNAME=your-apache-id
   ASF_PASSWORD=your-apache-password
   ```
3. **GPG signing key** published to `keys.openpgp.org` and added to the Sling `KEYS` file
   (<https://dist.apache.org/repos/dist/release/sling/KEYS>).
4. **`~/.m2/settings.xml`** configured for maven-gpg-plugin **3.x** (used by recent parent POMs). The
   passphrase is read from a **server** whose id is `gpg.passphrase` (the `gpg.passphraseServerId`
   default) and decrypted via `settings-security.xml` — **not** from a `<gpg.passphrase>` profile
   property (that older approach is passed to gpg literally and fails):
   ```xml
   <servers>
     <server><id>apache.snapshots.https</id><username>ID</username><password>{ENC}</password></server>
     <server><id>apache.releases.https</id><username>ID</username><password>{ENC}</password></server>
     <server><id>gpg.passphrase</id><passphrase>{ENC}</passphrase></server>
   </servers>
   <profiles><profile><id>apache-release</id><properties>
     <gpg.pinentryMode>loopback</gpg.pinentryMode>
     <gpg.keyname>YOUR_KEY_ID</gpg.keyname>
     <smtp.host>smtp.gmail.com</smtp.host>
   </properties></profile></profiles>
   ```
5. **`~/.m2/settings-security.xml`** with the `<master>` password used to decrypt the above.

If any prerequisite is missing, stop and tell the user how to set it up before proceeding.

## Running the CLI

Use the Docker image directly. A `sling-cli` shell alias may exist but usually includes `-it` (TTY),
which **fails in non-interactive contexts** like Claude Code — so do not rely on it:

```bash
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release <command> [options]
```

- Default mode is `DRY_RUN` (shows what would happen). Add `-x AUTO` to execute, `-x INTERACTIVE` to confirm each step.
- Always run a **DRY_RUN first** for any command that sends mail or mutates Nexus/dist, show the user the output, then re-run with `-x AUTO`.
- `~` does not expand inside quoted docker args — use `$HOME`.

## Determining the Java version (Maven steps)

Build with the Java version required by the project's Sling parent POM (recent parents require Java
17). If `mvnXX` wrappers are absent, set `JAVA_HOME` explicitly to an installed JDK
(`/usr/libexec/java_home -V` to list).

## Phase 1: Stage (Maven, in the project being released)

1. **Parent POM current?** Confirm it's up to date and `mvn clean verify` is green.
2. **Dry-run prepare**, then clean. Verify only `<version>`/`<scm>` differ in `pom.xml.tag`:
   ```bash
   mvn release:prepare -DdryRun=true && mvn release:clean
   ```
3. **Deploy snapshot**; confirm `META-INF/LICENSE` + `META-INF/NOTICE` are inside the jar:
   ```bash
   mvn deploy
   ```
4. **Prepare + perform** (creates the tag, bumps to next SNAPSHOT, signs + stages to Nexus):
   ```bash
   mvn release:prepare
   mvn release:perform
   ```
   Capture the staging repo id from a `…/orgapachesling-NNNN` line. If it scrolled off, find it with
   `release list` (below).

## Phase 2: Vote

### 2.0 — Find the staging repo id (if not captured)
```bash
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release list
```
Lists every staging repo with its `[open]`/`[closed]` state. A freshly staged repo is `[open]`; the
numeric suffix of `orgapachesling-NNNN` is the `REPO_ID`.

### 2.1 — Close the staging repository
```bash
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release close-staging -r <REPO_ID> -x AUTO
```
The description is auto-derived from the staged POM's `<name> <version>` (so it is correct even for an
open repo not yet in the Lucene index). After closing, the repo shows as `[closed]`.

### 2.2 — Verify artifacts
```bash
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release verify -r <REPO_ID>
```
Confirm PGP signatures, SHA-1, MD5, and CI status all pass before voting.

### 2.3 — Send the vote email (dry-run first!)
```bash
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release prepare-email -r <REPO_ID>          # review
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release prepare-email -r <REPO_ID> -x AUTO  # send
```

### 2.4 — Wait 72h, then tally
At least 72 hours on dev@sling.apache.org and ≥3 binding (PMC) +1 votes are required.
```bash
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release tally-votes -r <REPO_ID>            # review
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release tally-votes -r <REPO_ID> -x AUTO    # send [RESULT]
```
**PMC membership is auto-detected from your ASF id** — no flag needed. If you are a PMC member the
result email says you will copy the release to the dist directory yourself; otherwise it asks a PMC
member to do the dist upload.

### If the vote FAILS
```bash
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release drop -r <REPO_ID> -x AUTO
git push --delete origin <TAG_NAME>   # e.g. org.apache.sling.feature.launcher-1.3.4
git tag -d <TAG_NAME>
# restart Phase 1 with a new version
```

## Phase 3: Finalize (after a passing vote)

```bash
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release finalize -r <REPO_ID> -x AUTO
```
Runs in order: promote to Maven Central → (if you are a PMC member) update `dist.apache.org` →
create next JIRA version & move unresolved issues → mark JIRA version released → update Apache
Reporter.

- **PMC detection is automatic.** A PMC member's run also publishes to `dist.apache.org`; the previous
  version to remove is **deduced from the dist/release directory**, so no `--previous-version` is
  needed (override with `--previous-version X.Y.Z` only if necessary). A non-PMC run skips the dist
  step (a PMC member handles it, as requested in the tally-votes email).

### Update the website (optional)
```bash
docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli release update-local-site -r <REPO_ID>
```
Review the diff, then commit & push the sling-site changes separately.

## Quick reference

`<cli>` = `docker run --rm --env-file=$HOME/.apache-committer apache/sling-cli`

| Command | When |
|---------|------|
| `<cli> release list` | List staging repos with `[open]`/`[closed]` state + description |
| `<cli> release close-staging -r <ID> -x AUTO` | After `mvn release:perform` |
| `<cli> release verify -r <ID>` | Before sending the vote email |
| `<cli> release prepare-email -r <ID> -x AUTO` | Send the `[VOTE]` email |
| `<cli> release tally-votes -r <ID> -x AUTO` | After 72h with ≥3 binding votes (PMC auto-detected) |
| `<cli> release finalize -r <ID> -x AUTO` | All post-vote steps (dist included automatically for PMC) |
| `<cli> release promote -r <ID> -x AUTO` | Promote to Maven Central (individual step) |
| `<cli> release update-dist -r <ID> -x AUTO` | Update dist.apache.org (PMC only; prev version auto-deduced) |
| `<cli> release drop -r <ID> -x AUTO` | Drop staging (failed vote / cleanup) |
| `<cli> release create-new-jira-version -r <ID> -x AUTO` | Create next JIRA version |
| `<cli> release release-jira-version -r <ID> -x AUTO` | Mark JIRA version released |
| `<cli> release update-reporter -r <ID> -x AUTO` | Update Apache Reporter |
| `<cli> release update-local-site -r <ID>` | Update JBake website content |

## Execution notes

When this skill is invoked:
1. Confirm which phase the user is starting from (Stage / Vote / Finalize / Cancel) and the `REPO_ID` if past Phase 1.
2. Verify the prerequisites above before any mutating step.
3. For mail/Nexus/dist commands, run DRY_RUN first, show output, then `-x AUTO`.
4. On failure, diagnose (often a missing prereq or wrong `REPO_ID`) and suggest a fix before retrying.
5. Do not touch git state beyond what the documented steps require.
