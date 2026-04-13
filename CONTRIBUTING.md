# Contributing to Graphene

If you want to contribute to Graphene, follow these guidelines to keep the process smooth for everyone involved.
Here are the most common types of changes that get merged:

- Bug fixes
- New APIs and features that allow Graphene to be used in new ways or make it easier to use.
- Documentation improvements
- Support for keybind layouts and region specific behaviors.

## Development

Here is the recommended development setup that ensures consistency and compatibility with other contributors and users.

### Development Setup

- Java `21`
- Minecraft `1.21.11`
- Fabric Loader `0.18.4`
- Fabric API `0.141.3+1.21.11`

### Public API and Internal Code

- `tytoo.grapheneui.api.*` -> supported public API for consumers
- `tytoo.grapheneui.internal.*` -> internal implementation details that may change without notice

Changes under `api/` should be reviewed as public API changes.
If a pull request changes public behavior, update the relevant documentation in `README.md` and `docs/`.

### Testing and Manual Validation

Run these commands from the repository root:

```bash
./gradlew compileJava
./gradlew test
./gradlew build
./gradlew runDebugClient
```

When changing browser runtime, bridge, input, rendering, or loading behavior, also validate in game:

1. Run `./gradlew runDebugClient`.
2. Press `F10` to open `GrapheneBrowserDebugScreen`.
3. Validate the manual test pages pass.
4. Record the manual steps you ran in the pull request.

Changes in areas such as `internal/bridge`, `internal/http`, `internal/cef`, and `internal/input` should usually include tests.

## Pull Request Expectations

### Issue First Policy

All PRs must reference an existing issue.
Open one [HERE](https://github.com/trethore/graphene/issues) and describe the problem you are trying to solve or the feature you want to add.

- Use Fixes #12 or Closes #12 in your PR description to link the issue.
- For small fixes, a brief issue is fine - provide enough context for maintainers to understand the problem.

### General Requirements

- Keep pull requests small and focused
- Explain the issue and why your change fixes it
- Before adding new functionality, ensure it doesn't already exist elsewhere in the codebase
- Ensure the changes are tested and documented as needed.

### No AI-Generated Walls of Text

Long, AI-generated PR descriptions and issues are not acceptable and may be ignored. Respect the maintainers' time:

- Write short, focused descriptions
- Explain what changed and why in your own words
- If you can't explain it briefly, your PR might be too large

### Pull Request Titles

Pull request titles should use the Conventional Commit style:

- `feat(ui): add browser surface load listener helpers`
- `fix(bridge): prevent duplicate request completion`
- `docs: update installation guide for Maven Central`

## Security

Do not report security vulnerabilities in public issues.
Use the process described in `SECURITY.md` once the security policy is added.
