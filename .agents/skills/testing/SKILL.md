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

Opt-in via the `coverage` profile and **local-only**.

From `backend/api/`:

| Command                           | Runs               | Needs DB | Produces                                                 |
| --------------------------------- | ------------------ | -------- | -------------------------------------------------------- |
| `mvn verify -Pcoverage`           | unit + integration | yes      | HTML report in `target/site/jacoco/` (open `index.html`) |
| `mvn test -Pcoverage`             | unit only          | no       | `target/jacoco.exec`, no HTML report                     |
| `mvn verify` / `cli compose test` | unit + integration | yes      | no coverage                                              |

The report always covers unit and integration tests together.
