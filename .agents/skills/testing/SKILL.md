---
name: testing
description: Run and write tests for the reconciler project. Use when running tests, naming test methods, or checking coverage.
---

## Test framework

- **JUnit** (6.1.0) — test framework and runner
- **Mockito** (5.23.0) — inline mockmaker (configured via surefire/failsafe argLine)
- **AssertJ** (3.27.7) — fluent assertions
- **OkHttp** (5.3.2) — HTTP client for integration tests
- **JaCoCo** (0.8.14) — coverage at `verify` phase (surefire + failsafe)

## Run tests

```bash
cli compose test
```

This is the canonical test command. It always works regardless of environment state:

1. Builds the API test Docker image
2. Starts `db` + `flyway`
3. Runs `mvn verify` in the API container (surefire + failsafe + jacoco)
4. Tears down non-DB services
5. Restores your previous app env

Running `mvn verify` directly from the host also works if the test DB is already running, either from a previous `cli compose test` run or started manually.
A local run is currently the only way to access coverage reports (see [Coverage](#coverage) below).
Use the CLI when you need a clean full lifecycle; use a local run when you need results with coverage.

**No frontend tests exist currently.** CI only lints, type-checks, and formats-check the frontend.

## Test naming

Format: `<subject>_<givenClause>_<thenClause>`

All verbs in imperative / infinitive: `return`, `throw`, `respond`, `map`, `preserve`, `clear`, `doNotThrow`.

- **Unit**: `methodName_givenClause_thenClause`.
  - Success: `methodName_validInput_returnResult`
  - Failure: `methodName_invalidInput_returnError`
  - Exception: `methodName_invalidInput_throwException`
  - Example: `updateUserById_validIdAndPassword_returnUpdatedUser()`
- **Integration**: `METHOD_path_givenClause_thenClause`. Prefer `respond` instead of `return`.
  `ANY` is valid when the behavior applies to multiple HTTP methods on the same path.

  Examples:
  - No path param: `GET_users_existingUsers_respond200()`
  - Path param at end: `GET_users_id_existingId_respond200()`
  - Path param in middle: `GET_users_id_posts_existingUser_respond200()`
  - ANY (multi-method): `ANY_users_id_nonExistingId_respond404()`

### Subject may be omitted

When inside a `@Nested` class whose name already provides context, the subject (method name or METHOD path) may be omitted.

```java
@Nested
@DisplayName("Tests for POST /login")
class Login {
    @Test
    @DisplayName("given valid credentials, then respond 200 with refresh and access tokens")
    void validCredentials_respond200WithTokens() { ... }
}
```

### DisplayName

Same structure in natural language with `:` after the subject.

Format: `@DisplayName("<subject>: given <givenClause>, then <thenClause>")`

- Unit: `@DisplayName("updateUserById: given a valid id and password, then return the updated user")`
- Integration: `@DisplayName("GET /users/{id}: given an existing id, then respond 200 and return the user")`

### No match

If the naming convention above does not fit the test case, ask the user for guidance.

## Coverage

JaCoCo generates coverage reports at the `verify` phase. Reports are available in `backend/api/target/site/jacoco/`. Currently the report is only accessible when tests are run from the host. A Docker volume to expose it from the test container is planned.
