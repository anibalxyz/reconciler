---
name: commit
description: Create and review conventional commits for the reconciler project. Use when writing commit messages, selecting type and scope, amending commits, squashing history, or reviewing git log before committing.
---

## Format

```
<type>(<scope>): <description>

[body]
```

## New type or scope

If no listed type or scope fits, propose a candidate: a short name and a one-line definition the user could add to the table.
Wait for confirmation before proceeding.

## Types

| Type       | Use                                         |
| ---------- | ------------------------------------------- |
| `feat`     | New feature or enhancement                  |
| `fix`      | Bug fix                                     |
| `chore`    | Maintenance (deps, config, bump version)    |
| `docs`     | Documentation                               |
| `refactor` | Code restructuring, no functional changes   |
| `test`     | New or updated tests                        |
| `style`    | Formatting, whitespace, no code change      |
| `ci`       | CI/CD pipelines                             |
| `build`    | Affect the build system or external tooling |

No match -> see **New type or scope** above.

## Scopes

Optional. Include when the change falls clearly within one of these areas.

| Scope         | Area                           |
| ------------- | ------------------------------ |
| `auth`        | Authentication / authorization |
| `api`         | Backend API                    |
| `cli`         | CLI tool                       |
| `db`          | Database / Flyway migrations   |
| `infra`       | Docker, Nginx, infrastructure  |
| `deps`        | Dependencies                   |
| `dashboard`   | Frontend Dashboard (React)     |
| `public-site` | Frontend Public Site (Astro)   |
| `monitoring`  | Prometheus, Grafana, Loki      |

Omit when the change spans multiple scopes, or when including it would push the header over the limit.

No match -> see **New type or scope** above.

## Rules

1. **Header**: max 72 chars. Description in imperative present ("add", not "adds" or "added").
   If over the limit: shorten the description first; drop scope as a last resort.
   If still over, ask the user to split or provide a shorter description.
2. **Body**: optional; max 100 chars per line, wrapped at word boundaries.
   Recommended: bullet list with imperative verbs.
3. **Language**: English; even if the user writes in another language.
   Preserve identifiers, paths, and code tokens verbatim.
4. **Punctuation**: do not use em-dash, en-dash, or `--` as clause separators or to introduce explanations.
   Use `;`, `,`, `.`, parentheses, or split into two sentences instead.

## Examples

```
feat(deploy): add SSL, server bootstrap, and certbot renewal

- Add Let's Encrypt certbot integration and server bootstrap script
- Inject Prometheus runtime config at deploy time
- Document deployment process
```

```
chore: remove VERSION file, simplify release to tag-only flow

The VERSION file was a redundant source of truth. The git tag is
the single source of truth for releases.
```

```
feat(api): add rate limiting to login endpoint

- Limit login attempts to 5 per minute per IP
- Return 429 with Retry-After header on limit exceeded
```
