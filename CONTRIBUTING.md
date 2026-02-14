# Contributing to Reconciler

Welcome! This guide covers everything you need to know to contribute to Reconciler.

<details>
<summary>Table of Contents</summary>

- [Getting Started](#getting-started)
- [Opening an Issue](#opening-an-issue)
- [Contributing Code](#contributing-code)
- [Testing](#testing)
- [Code Standards](#code-standards)
- [Versioning](#versioning)

</details>

## Getting Started

Before contributing, make sure you can run the project by following the [Getting Started](/README.md#getting-started) section in the README.

Once the project is running, you're ready to start contributing. Continue with the sections below to understand our contribution workflow.

## Opening an Issue

Issues help track bugs, features, and tasks. For substantial changes, creating an issue first is recommended.

### When to create an issue

- New features or enhancements
- Bugs that need investigation
- Refactors or maintenance tasks
- Documentation improvements that require discussion

### When you can skip creating an issue

- Typo fixes
- Comment changes
- Small documentation edits
- Minor bug fixes with self-evident context

### How to create an issue

1. Go to the [Issues page](https://github.com/anibalxyz/reconciler/issues)
2. Click "New Issue"
3. Choose the appropriate template:
   - **Bug Report** - Something isn't working as expected
   - **Feature Request** - Propose a new feature or enhancement
   - **Task/Chore** - Maintenance, refactoring, or technical improvements
4. Fill out the template completely
5. Submit the issue

## Contributing Code

### Workflow Overview

1. Create or identify an issue to work on (optional for small changes)
2. Create a branch from main
3. Make your changes following our conventions
4. Commit using conventional commit format
5. Push your branch and open a pull request
6. Merge the PR to main

### 1. Create a Branch

Branch from `main`:

```bash
git switch main
git pull
git switch -c <type>/<description>
```

**Branch naming conventions:**

- `feature/description` - New features
- `fix/description` - Bug fixes
- `chore/description` - Maintenance tasks
- `docs/description` - Standalone documentation improvements

**Examples:**

- `feature/excel-export`
- `fix/token-refresh-timing`
- `chore/update-dependencies`
- `docs/expand-getting-started`

### 2. Make Your Changes

Make your changes following our [Code Standards](#code-standards).

### 3. Commit Your Changes

This project uses [Conventional Commits](https://www.conventionalcommits.org/).

**Format:**

```text
<type>[optional scope]: <description>

[optional body]

[optional footer]
```

**Types:**

- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation only
- `chore` - Maintenance (deps, config, etc)
- `refactor` - Code restructuring (no functional changes)
- `test` - Adding or updating tests
- `style` - Formatting, whitespace (no code changes)

**Scopes** (optional, project-specific):

- `auth` - Authentication/authorization
- `dashboard` - Dashboard frontend
- `public-site` - Public-facing site
- `api` - Backend API
- `cli` - CLI tool
- `db` - Database/migrations
- `infra` - Infrastructure (Docker, Nginx)
- `deps` - Dependencies

Omit scope if change spans multiple areas or does not add information

**Examples:**

```bash
git commit -m "feat(auth): add logout endpoint with token revocation"
git commit -m "fix(dashboard): resolve token refresh timing issue"
git commit -m "chore(deps): update React to v19.2.3"
git commit -m "docs: update setup instructions"
```

**Tips:**

- Use present tense ("add feature" not "added feature")
- Keep the subject under 50 characters
- Add a body if more context is needed (wrap lines at 72 characters)
- Reference issues in footer: "Closes #123" or "Fixes #456"

### 4. Push and Open a Pull Request

```bash
git push origin <your-branch-name>
```

Go to GitHub and open a Pull Request to `main`. Fill out the [PR template](/.github/pull_request_template.md), ensuring you:

- Explain **what** changed and **why**
- Describe **how** it was implemented (if complex)
- Note how you tested it
- Link related issues with `Closes #123`

## Testing

### Running Tests

```bash
# Run full test suite
cli compose test
```

> [!NOTE]
> Currently, tests are only implemented for the backend API. Frontend testing will be added in the future.

### Testing Guidelines

**When to write tests:**

- ✅ New features (required)
- ✅ Bug fixes (required)
- ✅ Refactored code (if behavior could change)

**Types of tests:**

- **Unit tests** - Test individual functions/methods
- **Integration tests** - Test API endpoints and database interactions

**Test environment:**

- Tests run in isolated `test` environment
- Database is reset before each test run (fresh state guaranteed)
- No persistence between test runs

## Code Standards

### Backend (Java)

**Formatting & Naming:**

- Use [Google Java Format](https://github.com/google/google-java-format) plugin for IntelliJ IDEA (latest version). No extra configuration needed
- Use descriptive names without interface prefixes. Example: `JpaUserRepository` or `RedisUserRepository` (not `IUserRepository` or `UserRepositoryImpl`)
- Controllers, Services, and Repositories use their respective suffixes naturally

**Documentation:**

- It is recommended to write JavaDoc for all sourcecode methods. Include at least one explanatory paragraph + `@param`, `@return`, `@throws` where applicable
- Important packages have internal READMEs explaining its architecture, content and other relevant information. These files should be always up-to date to help AI agents have more context

**Testing:**

- Unit tests: `methodName_context_result` format
  - Method name example: `updateUserById_validIdAndPassword_returnUpdatedUser()`
  - `@DisplayName` example: `"updateUserById: given valid id and password, then return updated user"`
- Integration tests: `METHOD_path_context_result` format
  - Method name example: `PUT_users_id_invalidEmailFormat_return400()`
  - `@DisplayName` example: `"PUT /users/{id}: given invalid email format, then return 400 Bad Request"`

<!-- For detailed backend conventions, see [backend/api/README.md](backend/api/README.md).-->

### Frontend (React/TypeScript)

- ESLint and Prettier are configured in frontend root directory
- Configuration can be extended in sub-frontends (e.g., dashboard has React-specific rules)
- Prettier plugin automatically orders Tailwind classes on save
- Run manually using npm commands (see [package.json](./frontend/package.json) or sub-frontend package.json)

### General

**Comments:**

- Write self-documenting code where possible
- Use comments for justifications, explanations, or complex logic
- TODOs and FIXMEs are allowed. Try to resolve them before opening the PR, and if unrelated to current work, create a separate issue for it

**Commits:**

- Follow [Commit Your Changes](#3-commit-your-changes)
- Prefer small, focused commits representing logical units of work
- Clean commits are ideal, but it's acceptable if a commit touches multiple related things
- Before opening a PR, ensure the overall history is clean and follows conventions

**Merge commits:**

- When merging a PR, use GitHub's default merge commit message; no modification needed.
- The 50-character limit does not apply to merge commits.

## Versioning

Reconciler versioning is based on [SemVer](https://semver.org/).

- **Version format:** `MAJOR.MINOR.PATCH` (e.g., `0.1.0`)
  - **MAJOR:** Breaking changes (e.g. new incompatible API response format)
  - **MINOR:** New features, backward-compatible
  - **PATCH:** Bug fixes, refactors, optimizations

- **Source of truth:** `VERSION` file in project root
- **When to release:** When pushing a version tag (e.g., `v0.2.0`)
- **Docker images:** Tagged with version (`0.1.0`) and `latest` (stable releases only)

### Creating a Release

#### 1. Ensure `main` is up to date

```bash
git switch main
git pull origin main
```

#### 2. Bump the version and create tag

Copy this block exactly, replacing `X.Y.Z` with the actual version number (e.g., `0.2.0`):

```bash
VERSION="X.Y.Z"

echo "$VERSION" > VERSION
git add VERSION
git commit -m "chore(release): bump version to $VERSION"
git push origin main

git tag -a "v$VERSION" -m "Release v$VERSION"
git push origin "v$VERSION"
```

#### 3. Automated release

GitHub Actions automatically:

- Builds and pushes Docker images to Docker Hub
- Creates a GitHub Release with auto-generated changelog

> [!NOTE]
> The Release workflow is triggered automatically when a tag matching `v*` is pushed to the repository.
