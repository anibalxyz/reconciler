---
name: release
description: Create a release for the reconciler project. Use when asked to cut a release, deciding version bumps with semver, generating changelogs, or writing release notes.
---

## Versioning

SemVer: `MAJOR.MINOR.PATCH`. The git tag is the single source of truth.

- PATCH: bug fixes
- MINOR: new features, backward compatible
- MAJOR: breaking changes

## Process

Here is a snippet to easily create a release:

```bash
# Replace X.Y.Z with the new version
VERSION="X.Y.Z"

git switch main
git pull origin main
git tag -a "v$VERSION" -m "Release v$VERSION"
git push origin "v$VERSION"
```

Pushing the tag triggers the release workflow in CI:

- Builds + pushes Docker images
- Builds CLI Python wheel
- Creates a GitHub Release with `release-assets.tar.gz`
  - Compose files
  - Environment templates
  - DB migrations
  - Monitoring configs
  - CLI wheel

## When to release

- A complete feature or milestone
- A significant bug fix
- Before deploying to production

Do NOT release per PR necessarily. Multiple PRs can accumulate before a release.
