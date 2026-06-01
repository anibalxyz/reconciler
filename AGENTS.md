# Reconciler

Financial transaction reconciliation tool. Java 21 + Javalin backend, TypeScript + TailwindCSS frontend, PostgreSQL 17, Flyway migrations.

## CLI

Python CLI (`cli/`) manages environments, Docker Compose, image builds, and testing.

**When to use:** environment setup, building images, running services, running tests.

Full reference: [docs/infra/cli.md](docs/infra/cli.md)

## Architecture

Feature-based, hexagonal-ish packaging:

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

Interface naming: descriptive, no prefix (`JpaUserRepository` not `IUserRepository` or `UserRepositoryImpl`). Server entrypoint: `com.anibalxyz.Main`.

Testing conventions: `.agents/skills/testing/SKILL.md`.

## Code style

- Java 21. Interface naming: descriptive, no prefix.
- Use [Google Java Format](https://github.com/google/google-java-format) plugin for IntelliJ IDEA (latest version). No extra configuration needed.
- JavaDoc: write it where it adds value (maintenance, understanding). Short descriptions are enough; include `@param` and `@return` only if they add information beyond the method header. Omit JavaDoc entirely when it adds no value.
- Prettier plugin auto-orders Tailwind classes on save (frontend).
- Write self-documenting code where possible. Use comments for justification, explanations, or complex logic. TODOs and FIXMEs are allowed; resolve them before opening a PR, or create a separate issue if unrelated.
- Frontend: TypeScript strict mode, ESLint + Prettier, TailwindCSS v4.
- Frontend commands: `npm run lint`, `npm run format:check`, `npm run typecheck` (from `frontend/`).

## Boundaries

- **Always**: run `cli compose test` after any backend change.
- **Always**: run `npm run lint` + `npm run format:check` + `npm run typecheck` after any frontend change.
- **Never**: push to `main` directly. Always branch + PR.
