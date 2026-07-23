# Graphene Changelog

## [Unreleased]

### Added

- Added Fabric support for Minecraft 26.2.
- Added Minecraft-version-specific Maven artifacts for all supported versions.
- Added structured release notes through this changelog.
- Added external SonarQube analysis configuration.
- Added a helper script for archiving released changelog entries.

### Changed

- Changed runtime JAR names to `graphene-<version>-<loader>-<minecraft-version>.jar`.
- Improved SonarQube coverage and duplication reporting.
- Centralized shared dependency and plugin versions in the Gradle version catalog.
- Simplified release notes to curated changes and a full GitHub comparison.
- Made publication targets sequential while keeping draft releases GitHub-only.
- Added promotion of an existing draft after the remaining publication targets succeed.
