# Reconciler

Financial transaction reconciliation tool. Java 21 + Javalin backend, TypeScript + TailwindCSS frontend, PostgreSQL 17, Flyway migrations.

Development workflow: [CONTRIBUTING.md](CONTRIBUTING.md) — setup, branching, commits, PRs, releases.

## CLI

Python CLI (`cli/`) manages environments, Docker Compose, image builds, and testing.

**When to use:** environment setup, building images, running services, running tests.

Full reference: [docs/infra/cli.md](docs/infra/cli.md)

## Architecture

Monorepo with two development areas:

- **Backend** (`backend/`): Java 21 + Javalin, PostgreSQL 17, Flyway migrations
- **Frontend** (`frontend/`): npm workspaces — `dashboard/` (React/Vite), `public-site/` (Astro); `common/` provides shared code imported via `@common/` alias

### Backend

Feature-based, hexagonal-ish packaging. Each feature follows a consistent four-layer structure:

```txt
features/{auth,users,system}/
  api/          controllers, routes, DTOs, error mappers
  application/  services, commands, results, environment configs
  domain/       entities, value objects, repository interfaces, domain errors
  infra/        JPA entities, repository implementations
server/         app wiring, config, context, dependency container
persistence/    EntityManager management
features/common/  shared types: Result, DomainError, Notification
```

**Key patterns**:

- **Interface naming:** descriptive, no prefix (`JpaUserRepository` not `IUserRepository`)
- **DI:** manual wiring in `DependencyContainer`, no framework
- **Error flow:** `DomainError` → `Result` → `FailureSignal` → `ErrorMapper` → `ErrorResponse` (RFC 9457)
- **Persistence:** request-scoped `EntityManager` via Javalin hooks; domain entities separate from JPA entities with `toDomain()`/`fromDomain()`
- **Entrypoint:** `com.anibalxyz.Main`

Testing conventions: `.agents/skills/testing/SKILL.md`.

### Frontend

npm workspaces — currently under restructuring; details will solidify soon.

### Infrastructure

- **Monitoring** (`monitoring/`): Prometheus + Grafana (metrics, dashboards), Loki + Promtail (log aggregation). JVM dashboard and RED dashboard.
- **Nginx** (`nginx/`): Production reverse proxy with SSL termination and Let's Encrypt (certbot). See [docs/infra/deploy.md](docs/infra/deploy.md).
- **Scripts** (`scripts/`): Server bootstrap; cron jobs for certbot renewal and docker prune.

## Code style

- Use [Google Java Format](https://github.com/google/google-java-format) plugin for IntelliJ IDEA (latest version). No extra configuration needed.
- JavaDoc: write it where it adds value (maintenance, understanding). Short descriptions are enough; include `@param` and `@return` only if they add information beyond the method header. Omit JavaDoc entirely when it adds no value.
- Prettier plugin auto-orders Tailwind classes on save (frontend).
- Write self-documenting code where possible. Use comments for justification, explanations, or complex logic. TODOs and FIXMEs are allowed; resolve them before opening a PR, or create a separate issue if unrelated.

## Day-to-day Tips

- **Test after backend changes:** run tests using the [CLI](#cli), or choose to run `mvn verify` from `backend/api/` for faster iteration if DB is already running.
- **Check frontend after changes:** run `npm run lint`, `npm run format:check`, and `npm run typecheck` from `frontend/`.
- **Sync Spanish README:** update `README.es.md` after `README.md` changes.
- **If you have a doubt:** ask instead of assume; do not do modifications without a clear reason.
- **When stuck on a task:** try to re-think the problem and take a new approach. Sometimes is more efficient starting from scratch.
- **Look for existing patterns first:** before creating a file, find a similar one in the codebase (same type: controller, service, route, test) and follow its structure. Patterns are consistent across features; copying an existing one saves time and prevents style mismatches.
- **Review the diff before committing:** read the full diff to catch accidental changes and have a clean context to write an accurate commit message.
