# Graphene Documentation

Graphene is a client-side UI library for Minecraft mods. It renders HTML, CSS, and JavaScript inside Minecraft by
embedding Chromium through JCEF, while exposing a Java API for screens, browser control, assets, and JavaScript
communication.

![A Three.js scene rendered by Graphene inside Minecraft](images/threejs-showcase.png)

## Supported versions

Graphene supports Minecraft 26.2 and 1.21.11 on Fabric.

Check [compatibility and installation](reference/compatibility-and-installation.md) for the exact versions and dependency
coordinates.

## Start here

1. [Build your first web screen](tutorials/first-web-screen.md) to register Graphene, load packaged assets, and display
   a browser widget.
2. [Connect Java and JavaScript](tutorials/connect-java-and-javascript.md) to exchange events and request/response
   messages.

## Solve a specific task

- [Manage assets and frontend development](how-to/manage-assets-and-frontend-development.md)
- [Control and observe the browser](how-to/control-and-observe-the-browser.md)
- [Render a custom browser surface](how-to/render-a-custom-browser-surface.md)
- [Configure browser policies](how-to/configure-browser-policies.md)
- [Use Chromium DevTools](how-to/use-devtools.md)
- [Manage browser lifecycle](how-to/manage-browser-lifecycle.md)
- [Troubleshoot common problems](how-to/troubleshoot.md)

## Understand Graphene

- [Architecture and runtime](explanation/architecture-and-runtime.md)
- [Browser sessions, surfaces, and widgets](explanation/browser-layers.md)
- [Assets, origins, and bridge security](explanation/assets-origins-and-bridge-security.md)

## Look up an API

- [Compatibility and installation](reference/compatibility-and-installation.md)
- [Core Java API](reference/core-java-api.md)
- [JavaScript bridge API](reference/javascript-bridge-api.md)
- [Configuration and defaults](reference/configuration-and-defaults.md)

## Distribution

- [Maven Central for Minecraft 26.2](https://central.sonatype.com/artifact/io.github.trethore/graphene-ui-26.2)
- [Maven Central for Minecraft 1.21.11](https://central.sonatype.com/artifact/io.github.trethore/graphene-ui-1.21.11)
- [Modrinth](https://modrinth.com/mod/grapheneui)
- [GitHub Releases](https://github.com/trethore/graphene/releases)
