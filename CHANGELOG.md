# Changelog

## [Unreleased]

### Added

- Added Fabric support for Minecraft 26.1.2.
- Added MP4 and WebM MIME detection for Graphene-managed assets.
- Added single byte-range responses for HTTP, app, and classpath resources.

### Changed

- Reworked Fabric version-specific packages to deduplicate shared code.

## [2.2.0] - 2026-08-29

### Added

- Gradle 9.7.1.
- Qodana static analysis as a manual scan.
- Added per-browser file-dialog authorization policies.
- Added routed support for the File System Access API's `showDirectoryPicker()` method.

### Changed

- Reworked `packages/` and `debug-client/` structure to deduplicate shared code.
- Directory-picker routing now works without exposing the public JavaScript bridge and fails closed when unavailable.
- Folder selections are validated before they are returned to Chromium.

### Fixed

- Fixed a bug where `window.showDirectoryPicker()` method would let you select files instead of folders.

## [2.1.0] - 2026-07-23

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
