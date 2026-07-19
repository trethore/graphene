# Security Policy

## Supported Versions

Only the latest released version of Graphene receives security fixes. Supported Minecraft, loader, and Java versions are
listed in the [compatibility documentation](docs/reference/compatibility-and-installation.md).

## Reporting a Vulnerability

Do not report security vulnerabilities through public issues, discussions, or pull requests.

Use GitHub's private **Report a vulnerability** feature. If it is unavailable, contact me through the methods listed on
my [GitHub profile](https://github.com/trethore).

Include:

- The affected Graphene version and environment.
- The relevant configuration.
- Clear reproduction steps or a minimal proof of concept.
- The security impact and required attacker access.

Automated scanner results must be manually verified. I will not accept unverified reports or AI-generated walls of text
without a clear, reproducible vulnerability.

I aim to acknowledge valid reports within **14 days**, but this is not a guaranteed response or remediation timeline. I
may ask for more information while investigating.

## Scope

Examples of issues that are in scope:

- Bypassing bridge origin or navigation policies.
- Accessing local files without the required explicit configuration.
- Escaping asset paths or the loopback HTTP server's security restrictions.
- Enabling downloads, dialogs, DevTools, or other browser capabilities despite a denying policy.
- Compromising Graphene release artifacts or its release process.

The following are generally out of scope:

- A mod deliberately granting untrusted content privileged bridge access.
- Application-specific handlers that fail to validate their own data.
- Development features that were explicitly enabled and exposed by the user.
- Bugs in remote websites or upstream dependencies unless Graphene introduces or unexpectedly exposes the issue.
- Unsupported versions, social engineering, or crashes without a demonstrated security impact.

For guidance on configuring Graphene securely, see
[Assets, Origins, and Bridge Security](docs/explanation/assets-origins-and-bridge-security.md).

## Disclosure

Please keep the report private until a fix or mitigation is available and disclosure has been coordinated. Reporter
credit will be given when requested.
