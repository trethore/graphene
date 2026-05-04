# Graphene Documentation

Graphene is a client-side Chromium UI library for Fabric Minecraft mods. These docs move from setup to advanced
integration.

**Baseline**

| Requirement | Version                         |
|-------------|---------------------------------|
| Java        | `21`                            |
| GPU         | NVIDIA GeForce GT 720 or better |

**Supported Fabric modules**

| Module            | Minecraft  | Fabric Loader | Fabric API             | Artifact                                                    |
|-------------------|------------|---------------|------------------------|-------------------------------------------------------------|
| `fabric-1.21.11` | `1.21.11` | `0.18.4`      | `0.141.3+1.21.11` | `io.github.trethore:graphene-ui-fabric-1.21.11:<version>` |

**What's to read**

| Page                                    | Covers                                        |
|-----------------------------------------|-----------------------------------------------|
| [Overview](overview.md)                 | Concepts, runtime model, typical flow         |
| [Installation](installation.md)         | Maven coordinates, Gradle setup, registration |
| [Quickstart](quickstart.md)             | First web screen                              |
| [Bridge](bridge.md)                     | Java <-> JavaScript messaging                 |
| [Input Capture](input-capture.md)       | Cursor and Escape capture for web UIs         |
| [Assets and URLs](assets-and-urls.md)   | App, classpath, and HTTP asset routes         |
| [Lifecycle](lifecycle.md)               | Runtime, widget, screen, and bridge cleanup   |
| [Debugging](debugging.md)               | DevTools, debug screens, logging              |
| [Advanced Surface](advanced-surface.md) | Direct `BrowserSurface` control               |
| [Troubleshooting](troubleshooting.md)   | Common failures and fixes                     |
| [Testing](testing.md)                   | Unit and in-game validation                   |

**Migration notes**

- [Shared Runtime API](shared-runtime-api.md) - shared runtime registration and merge behavior.
