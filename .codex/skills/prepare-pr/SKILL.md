---
name: prepare-pr
description: Prepare a branch for pull request creation by ensuring it is based on the latest remote target branch, rebasing when needed, resolving clear non-version conflicts, asking on version or ambiguous conflicts, and running Maven package verification with tests skipped. Use when the user asks to prepare, create, open, or check a PR, mark a branch PR-ready, or verify a branch before PR creation.
---

# Prepare PR

Use this skill before creating or declaring a pull request ready.

## Defaults

- Use `origin/main` as the PR target unless the user explicitly names another base branch.
- Use rebase, not merge, to integrate new base commits.
- Run Maven package verification with tests skipped: `.\mvnw.cmd package -DskipTests`.
- Do not create the PR unless the user explicitly asks for PR creation after the branch is ready.

## Workflow

1. Inspect branch state:
   - `git status --short --branch`
   - `git branch --show-current`
   - `git remote -v`
2. If the working tree is dirty, stop and ask before syncing, rebasing, stashing, or modifying files.
3. Fetch the latest remote state:
   - `git fetch origin`
4. Compare the branch with the target base:
   - compute `git merge-base HEAD origin/main`
   - compare it to `origin/main`
5. If the merge-base is not the latest target base, rebase the branch onto the target base:
   - `git rebase origin/main`
6. If conflicts occur:
   - ask the user which version to keep for any conflict involving project, parent, dependency, plugin, BOM, or property-backed version values
   - resolve only clear mechanical non-version conflicts directly
   - ask the user for ambiguous product or code decisions
   - never discard user work
7. After a successful rebase or conflict resolution, run:
   - `.\mvnw.cmd package -DskipTests`
8. Report PR-ready only when:
   - the working tree is clean
   - the current branch includes the latest target base
   - Maven package verification with tests skipped passes

## Conflict Handling

- If a conflict involves project, parent, dependency, plugin, BOM, or property-backed version values, stop and ask the user which version to keep even when the conflict looks mechanical.
- If a non-version conflict is obviously mechanical, resolve it and continue the rebase.
- If a conflict changes behavior, product intent, data shape, public API, migrations, or tests in a way that needs judgment, ask the user before choosing.
- If conflict resolution fails or becomes uncertain, pause with the conflicting files and current rebase state.

## Reporting

In the final response, include:
- current branch
- target base branch
- whether the branch was already up to date or rebased
- any conflicts resolved or decisions requested
- verification command and result
- PR readiness status
