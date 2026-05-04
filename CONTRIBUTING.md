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
- Fabric Loader `0.18.4`

Supported version modules:

| Module            | Minecraft  | Fabric API             |
|-------------------|------------|------------------------|
| `fabric-1.21.11` | `1.21.11` | `0.141.3+1.21.11` |

### Public API and Internal Code

- `common/src/main/java/tytoo/grapheneui/api/*` -> shared public API for consumers
- `fabric-<minecraft-version>/src/client/java/tytoo/grapheneui/api/*` -> version-specific public API that may mention Minecraft or Fabric types
- `tytoo.grapheneui.internal.*` -> internal implementation details that may change without notice

Changes under `api/` should be reviewed as public API changes.
Keep Minecraft, Mojang, Fabric, and Mixin imports out of `common/`; put version-specific integration in the matching
`fabric-<minecraft-version>/` module.
If a pull request changes public behavior, update the relevant documentation in `README.md` and `docs/`.

### Adding or Updating Version Modules

When adding support for another Minecraft version:

1. Add a new `fabric-<minecraft-version>/` module.
2. Include it in `settings.gradle.kts`.
3. Put Minecraft and Fabric API versions in that module's `gradle.properties`.
4. Keep reusable logic in `common/` and only move version-specific integration into the new Fabric module.
5. Update the supported-version tables in `README.md`, `docs/README.md`, and `docs/installation.md`.
6. Update validation commands and CI references if the new module should run by default.

### Testing and Manual Validation

Run these commands from the repository root:

```bash
./gradlew :common:compileJava :fabric-1.21.11:compileClientJava
./gradlew test
./gradlew check
./gradlew :fabric-1.21.11:runDebugClient
```

When changing browser runtime, bridge, input, rendering, or loading behavior, also validate in game:

1. Run `./gradlew :fabric-1.21.11:runDebugClient`.
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
