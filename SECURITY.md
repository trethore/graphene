# Security Policy

Graphene is a client-side Chromium-based UI library for Minecraft `1.21.11` on Fabric.
If you think you found a real worth to fix security issue please follow the instructions below to report it.

## Supported Versions

| Version        | Supported |
|----------------|-----------|
| Latest release | Yes       |
| `main`         | Yes       |
| Older releases | No        |

## IMPORTANT

We do not accept AI generated security reports. If you submit one that will be an automatic ban from the project.

## Reporting a Vulnerability

Do not report security vulnerabilities through public issues, pull requests, or discussions.

Preferred reporting path:

1. Use GitHub private vulnerability reporting for this repository, if it is enabled.
2. If private reporting is not available, email `titou.rethore@gmail.com` or `Tyt2` (on Discord) with the subject prefix
   `[Graphene Security]`.

## What to Report

Security reports are in scope when they affect Graphene's expected trust boundaries, defaults, or documented guarantees.

| Report type                                                                          | In scope   | Notes                                                                                         |
|--------------------------------------------------------------------------------------|------------|-----------------------------------------------------------------------------------------------|
| HTTP server reachable beyond loopback-only restrictions                              | Yes        | Includes bypasses of Graphene's local-only binding expectations.                              |
| Unintended file read or write access                                                 | Yes        | Includes browser-exposed features reaching files outside documented boundaries.               |
| Privilege escalation through the Graphene bridge                                     | Yes        | Includes untrusted page content gaining unintended access to Java-side capabilities.          |
| Remote debugging exposure or origin restriction bypass                               | Yes        | Includes DevTools access beyond configured or documented limits.                              |
| Unsafe extension loading behavior                                                    | Yes        | Includes loading unexpected extensions or bypassing configured extension folder restrictions. |
| Path traversal or unsafe download handling                                           | Yes        | Includes writes outside intended download paths or unsafe filename/path handling.             |
| Code execution, sandbox escape, or native/JCEF integration issues caused by Graphene | Yes        | Report Graphene-triggered behavior with clear reproduction details.                           |
| Normal functional bugs with no security impact                                       | No         | Use regular issues instead.                                                                   |
| Problems affecting unsupported older releases only                                   | Usually no | Report them only if they also affect `main` or the latest release.                            |
| Explicitly enabled risky features behaving exactly as documented                     | Usually no | They are still in scope if Graphene bypasses documented boundaries or defaults.               |

## What to Include

Include as much of the following as possible:

- Graphene version or commit hash;
- operating system and architecture;
- Minecraft, Fabric Loader, and Java versions;
- the relevant Graphene configuration;
- whether remote debugging, extension loading, or file system access was enabled;
- clear reproduction steps;
- proof of concept, logs, screenshots, or videos if they help verify impact;
- your assessment of impact and attack prerequisites.

## Disclosure Process

- You will receive an acknowledgment within 7 days.
- A status update is usually provided within 14 days.
- Fix timelines depend on severity, complexity, and maintainer availability.
- Please allow time for investigation and coordinated disclosure before publishing details.

## Security Notes for Graphene Users

Graphene currently ships with these security-relevant defaults and boundaries:

- the built-in HTTP server is intended to bind to loopback addresses only;
- remote debugging is disabled unless explicitly configured;
- file system access is denied by default;
- browser extensions are loaded only from explicitly configured extension folders.

If you enable remote debugging, extension loading, or file system access, treat the environment as more sensitive and
review your configuration carefully.
