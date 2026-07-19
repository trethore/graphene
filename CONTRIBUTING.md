# Contributing to Graphene

Contributions to Graphene are welcome. By participating, you agree to follow the
[Code of Conduct](CODE_OF_CONDUCT.md).

## Issues

Search existing issues before opening a new one.

Bug reports should describe:

- What happened and what you expected to happen.
- Clear steps to reproduce the problem.
- The Graphene, Minecraft, Fabric API, Java, and operating system versions involved.
- Relevant logs or a minimal reproduction when available.

Keep each issue focused on a single problem or request. For substantial features or API changes, open an issue first so
the approach and scope can be discussed before implementation begins.

## Pull Requests

Keep pull requests focused and avoid unrelated changes. A pull request should:

- Explain what changed and why.
- Link any related issues.
- Include or update tests when appropriate.
- Update documentation when public behavior or APIs change.
- Describe how the changes were tested.

Be prepared to respond to review feedback and explain the reasoning behind your implementation.

## Validation

Before opening a pull request, format the project and run all checks and tests:

```shell
./gradlew spotlessApply
./gradlew check
./gradlew build
```

Review any changes made by the formatter before committing them. If a check cannot be run or does not pass, explain why
in the pull request.

## AI-Generated Content

We do not accept AI-generated walls of text in issues, pull requests, or review discussions.

Keep descriptions concise and explain the problem and proposed changes in your own words. You are responsible for
understanding, verifying, and being able to explain everything you submit. Unreviewed, low-effort, or unexplained
generated content may be closed without detailed feedback.

## Security

Do not report security vulnerabilities in public issues. Use the process described in [SECURITY.md](SECURITY.md).
