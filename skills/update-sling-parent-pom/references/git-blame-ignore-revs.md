# Keeping git blame clean after the spotless reformat

A parent POM upgrade that introduces or changes Spotless formatting rules (see the main instructions' `mvn spotless:apply` step) reformats every source file the new rules touch. Left as-is, `git blame` (and GitHub's blame view) attributes every reformatted line to the upgrade commit instead of the commit that actually last changed that line's content.

Per the [Update to Parent 60](https://cwiki.apache.org/confluence/spaces/SLING/pages/284790344/Update+to+Parent+60) migration notes:

## Preferred: commit formatting separately

If you control the commit sequence, run `mvn spotless:apply` and commit the resulting reformat **on its own**, separate from the parent-version/dependency changes. This makes the next step trivial and keeps the substantive diff reviewable on its own.

## Either way: add the commit(s) to `.git-blame-ignore-revs`

Create (or append to) a `.git-blame-ignore-revs` file at the repo root listing the full commit SHA(s) that contain reformatting, one per line, with a comment above each explaining why:

```
# SLING-NNNNN: sling-bundle-parent NN -> MM upgrade; includes the resulting
# mvn spotless:apply reformat of every source file.
<full-commit-sha>
```

GitHub's blame view picks this file up automatically at the default location — no repo configuration needed for that. For local `git blame`, either pass it explicitly or configure it once per clone:

```bash
git blame --ignore-revs-file .git-blame-ignore-revs -- path/to/file
# or, to make it the default for this clone:
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

Verify the file works before committing it — blame a file that was reformatted and confirm the reformatted lines now attribute to their real prior author/commit, not the upgrade commit.

If the reformat ended up mixed into the same commit as substantive changes (rather than committed separately) and that commit is already pushed/shared (e.g. an open PR), do not rewrite it to split things apart — rewriting shared history requires a force-push, which needs explicit user approval and risks losing work for anyone else who fetched the branch. Just add that commit's SHA to `.git-blame-ignore-revs` as a follow-up commit; it still fully addresses the blame-noise problem even though the commit itself remains mixed.
