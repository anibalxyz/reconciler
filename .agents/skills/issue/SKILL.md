---
name: issue
description: Create and triage issues for the reconciler project. Use when writing issue descriptions, creating issues, filling templates, applying labels and milestones, or deciding whether to open an issue.
---

## When to create an issue

- New features or enhancements
- Bugs that need investigation
- Refactors or maintenance tasks
- Documentation improvements that require discussion

## When to skip creating an issue

- Typo fixes
- Comment changes
- Small documentation edits
- Minor bug fixes with self-evident context

## How to create an issue

Navigate to the [Issues page](https://github.com/anibalxyz/reconciler/issues) and select **New Issue**. Three templates are available under `.github/ISSUE_TEMPLATE/`:

| Template            | Label     | When to use                                      | Captures                                                      |
| ------------------- | --------- | ------------------------------------------------ | ------------------------------------------------------------- |
| **Bug Report**      | `bug`     | Something is not working as expected             | Expected vs actual behavior, steps to reproduce, context      |
| **Feature Request** | `feature` | Propose a new feature or enhancement             | Problem statement, proposed solution, alternatives considered |
| **Task / Chore**    | `chore`   | Maintenance, refactoring, technical improvements | Task type (dropdown), description, motivation                 |

Pick the template that fits, fill it out completely, and submit.

## Labels

Issues are automatically labeled by template (`bug`, `feature`, `chore`). Add extra labels if they help triage (e.g., `performance`, `security`).

## Relationship with PRs

When a PR implements an issue, the PR body stays lean: it links the issue, captures decisions that emerged during implementation, and describes testing. The issue is the spec; the PR is the delta. See the PR skill for details.
