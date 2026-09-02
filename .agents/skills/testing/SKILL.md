---
name: testing
description: Run and write tests for the reconciler project. Use when running tests, naming test methods, checking coverage, debugging test failures, or running specific test subsets.
---

## Test framework

- **JUnit** — test framework and runner
- **Mockito** — inline mockmaker
- **AssertJ** — fluent assertions
- **OkHttp** — HTTP client for integration tests
- **JaCoCo** — opt-in coverage via the `coverage` profile (surefire + failsafe)

## Run tests

Two ways to run the backend suite; pick by what matters for the moment:

### Docker: `cli compose test`

The canonical command: always works regardless of environment state.

1. Builds the API test Docker image
2. Starts `db` + `flyway`
3. Runs `mvn verify` in the API container (surefire + failsafe)
4. Tears down non-DB services
5. Restores your previous app env

**Pros**: one command, hermetic. The DB is always brought up with migrations, so you know the full suite ran against a known state.
**Cons**: slowest feedback loop; no coverage, no test subsets.

### Local: plain `mvn` from `backend/api/`

Needs the test DB running only when integration tests run.

| Command                 | Runs                                 | Needs DB |
| ----------------------- | ------------------------------------ | -------- |
| `mvn test`              | unit tests only                      | no       |
| `mvn verify`            | unit + integration                   | yes      |
| `mvn verify -Pcoverage` | unit + integration + coverage report | yes      |

**Pros**: fastest iteration; granular control (unit-only without a DB, coverage or not, single-test reruns with `-Dtest=EmailTest`).
**Cons**: remember to launch the test DB.

**No frontend tests exist currently.** CI only lints, type-checks, and formats-check the frontend.

## Test naming

### Class and file naming

- **Unit tests**: `<Subject>Test`, extending `UnitTest`.
  Subject is the unit under test: `EmailTest`, `AuthServiceTest`, `CreateUserHandlerTest`
- **Integration tests**: `<Action>IT`, extending `IntegrationTest`.
  Named after the action or route group, not the HTTP method: `CreateUserIT`, `GetAllUsersIT`, `AuthRoutesIT`, `UsersIT`
- The `Test` / `IT` suffix is how surefire and failsafe pick up each suite
- Tests mirror the production structure

### Method names

Format: `<subject>_<givenClause>_<thenClause>`

All verbs in imperative / infinitive: `return`, `throw`, `respond`, `map`, `preserve`, `clear`, `doNotThrow`.

- **Unit**: `methodName_givenClause_thenClause`.
  - Success: `methodName_validInput_returnResult`
  - Failure: `methodName_invalidInput_returnError`
  - Exception: `methodName_invalidInput_throwException`
- **Integration**: `METHOD_path_givenClause_thenClause` | `givenClause_thenClause`.
  Always use `respond` for HTTP outcomes (`respond200`, `respond401`), not `return`.
  `ANY` is valid when the behavior applies to multiple HTTP methods on the same path.
  Can omit `subject` in some cases (see below).

**Examples:**

- No path param: `validRefreshToken_respond200WithRefreshedTokens()`
- Path param at end: `GET_users_id_existingId_respond200()`
- Path param in middle: `GET_users_id_posts_existingUser_respond200()`
- ANY (multi-method): `ANY_users_id_nonExistingId_respond404()`

### Subject may be omitted in `@Nested` classes

The subject (method name or HTTP method + path) may be omitted from test methods **only** when a `@Nested` class is explicitly named after the method or endpoint under test.

In that case, the method name simplifies to `<givenClause>_<thenClause>`, and `@DisplayName` starts directly with `"given ..., then ..."`.

Example:

```java
@Nested
@DisplayName("map()")
class Map {
    @Test
    @DisplayName("given invalid credentials error, then respond 401 with correct detail")
    void invalidCredentialsError_respond401WithDetail() { ... }
}
```

### DisplayName

Same structure in natural language with `:` after the subject.

Format: `@DisplayName("<subject>: given <givenClause>, then <thenClause>")`

- Unit: `@DisplayName("updateUserById: given a valid id and password, then return the updated user")`
- Integration: `@DisplayName("GET /users/{id}: given an existing id, then respond 200 and return the user")`
- Use-case: `@DisplayName("execute: given valid command, then return auth result")`

### No match

If the naming convention above does not fit the test case, ask the user for guidance.

## Test structure & conventions

### Execution flow & Test ordering

- **Follow production code flow**: Order test methods following the natural execution path of the code under test.
- Typically, this means placing **failure, validation, and early-exit scenarios first**, ending with the **happy path / success scenario(s)** last.

### `@Nested` classes usage

- **Grouping scenarios**: Use `@Nested` when grouping multiple scenarios for a specific method or context.
- **Flatten when sparse**: If a category or nested class contains only 1 or 2 tests (very common for success cases),
  flatten it (and the other nested class) directly into the parent test class to avoid unnecessary boilerplate.
- **Refactor signal**: If a `@Nested` class grouping tests for a single method becomes too large or complex,
  it often signals that the production method or class should be split into smaller, focused units.

### Dependencies: Mocks vs. Real Implementations

- **Prefer real implementations** for dependencies that have static, predictable behavior across all tests
  (e.g., state-free mappers, utility classes, or simple environment records).
  This eliminates `when()` / `verify()` setup overhead.
- **Use mocks** when you need dynamic, method-level control over responses or need to verify specific interactions.
- **Combine freely**: Combining real implementations for some parameters (like `Env` or pure mappers) and mocks for others (like services) is standard practice.

### Shared Test Utilities

A `shared` test package provides common helpers and utilities to be reused across unit and integration tests, for example:

- **`Constants`**: Common constant definitions for tests (test data, defaults, etc.).
- **`Helpers`**: Utility methods for setup, transformations, and boilerplate reduction.
- **`HttpRequest`**: Builders and helpers for HTTP request assertions and payloads in integration tests.

Always check `shared` before writing custom setup code or static assertions.

## Coverage

Opt-in via the `coverage` profile and **local-only**.

From `backend/api/`:

| Command                           | Runs               | Needs DB | Produces                                                 |
| --------------------------------- | ------------------ | -------- | -------------------------------------------------------- |
| `mvn verify -Pcoverage`           | unit + integration | yes      | HTML report in `target/site/jacoco/` (open `index.html`) |
| `mvn test -Pcoverage`             | unit only          | no       | `target/jacoco.exec`, no HTML report                     |
| `mvn verify` / `cli compose test` | unit + integration | yes      | no coverage                                              |

The report always covers unit and integration tests together.
