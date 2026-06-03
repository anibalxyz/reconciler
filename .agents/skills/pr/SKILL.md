---
name: pr
description: Branch naming and pull requests for the reconciler project. Use when creating branches, writing PR descriptions, opening PRs, or reviewing contributions.
---

## Title

Same convention as commits:

```
<type>(<scope>): <description>
```

> [!TIP] See `commit` skill for types, scopes, and format rules.

## Branch naming

Format: `<prefix>/<description-in-kebab-case>`

| Prefix      | Use                                    | Example                             |
| ----------- | -------------------------------------- | ----------------------------------- |
| `feat/`     | New feature                            | `feat/excel-export`                 |
| `fix/`      | Bug fix                                | `fix/token-refresh-timing`          |
| `chore/`    | Maintenance, deps, typos               | `chore/update-dependencies`         |
| `docs/`     | Documentation                          | `docs/expand-getting-started`       |
| `refactor/` | Code restructuring, no behavior change | `refactor/clarify-composition-root` |

If no listed type fits, propose a candidate: a short name and a one-line definition the user could add to the table.
Wait for confirmation before proceeding.

Rules:

- Always branch from `main`
- Description in lowercase kebab-case
- Keep it short but descriptive

## Template

The template lives at `.github/pull_request_template.md`. Read it directly.

## Rules

- **PR = unit of meaning**: one semantic change per PR. Not "add models" then "add service". Add the complete use case, including tests and docs.
- **Squash merge to `main`**: GitHub squash merge uses the PR title as the commit message. PR number is appended as `(#N)`.
- **Other merge strategies**: ask the user. Do not choose without confirmation.
- **With issue**: Summary links the issue (`Closes #N`), Key decisions captures what the issue couldn't foresee.
- **Without issue**: Summary carries the full what + why. Keep it for trivial changes only.
- **Avoid AI-slop patterns.** For example:
  Do not use em-dash, en-dash, or `--` as clause separators or to introduce explanations.
  Use `;`, `,`, `.`, parentheses, or split into two sentences instead.
