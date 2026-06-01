# Reconciler

Financial transaction reconciliation tool. Java 21 + Javalin backend, TypeScript + TailwindCSS frontend, PostgreSQL 17, Flyway migrations.

## Setup

```bash
python3 -m venv ./cli/.venv && source ./cli/.venv/bin/activate
pip install -e ./cli[dev]
cli set env dev --init          # creates .env.* from .example templates
```

From there `cli --help` discovers every command. Config: `cli.cfg` (current env), `backend/.env.{env}`, `frontend/.env.{env}`.

## Commands

```bash
cli compose test                # full test suite (builds, starts db+flyway, mvn verify, tears down)
cli image build all             # build all Docker images
cli compose up all              # start all services for current env
cli compose down all            # stop and remove containers
```

Testing runs `mvn verify` inside Docker (surefire + failsafe + jacoco). Backend only, no frontend tests. `mvn verify` from the host also works if the test DB is already running (fastest for iteration + coverage report at `backend/api/target/site/jacoco/`). Use `cli compose test` for a clean full lifecycle.

CI (`.github/workflows/ci.yaml`): PR to `main` runs frontend lint/format/typecheck, then `cli compose test`. Release: push tag `v*` builds Docker images + CLI wheel, creates GitHub Release.

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

## Testing

**Framework**: JUnit 5 + Mockito (inline mockmaker) + AssertJ + OkHttp. JaCoCo at `verify` phase.
**Naming**: `<subject>_<givenClause>_<thenClause>`; see `.agents/skills/testing/SKILL.md` for full convention.
**Nested classes** for success/failure grouping. Both unit and integration tests run via surefire.

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
