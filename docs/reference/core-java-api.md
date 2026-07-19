# Core Java API

This is a curated map of Graphene's public entry points. Follow the source links for complete signatures and Javadocs.

## Registration and context

| Type                                                                                                                    | Use                                                                                            |
|-------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| [`Graphene`](../../packages/common/src/main/java/io/github/trethore/graphene/api/Graphene.java)                         | Register a consuming mod and access the process-wide runtime or resolved global configuration. |
| [`GrapheneContext`](../../packages/common/src/main/java/io/github/trethore/graphene/api/GrapheneContext.java)           | Access one consumer's ID, configuration, asset URLs, browser factory, and runtime.             |
| [`GrapheneSubscription`](../../packages/common/src/main/java/io/github/trethore/graphene/api/GrapheneSubscription.java) | Remove a registered listener or handler through `unsubscribe()` or `close()`.                  |

## Browser

| Type                                                                                                                  | Use                                                                                                             |
|-----------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| [`BrowserSessions`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/BrowserSessions.java) | Create loader-independent off-screen browser sessions while the runtime is running.                             |
| [`BrowserSession`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/BrowserSession.java)   | Navigate, inspect state, send input, receive frames and events, manage downloads, and access the bridge.        |
| [`BrowserOptions`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/BrowserOptions.java)   | Configure rendering, JavaScript, bridge, navigation, downloads, context menus, and dialogs at browser creation. |
| [`BrowserFrame`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/BrowserFrame.java)       | Read an immutable off-screen frame snapshot and dirty regions.                                                  |

## Java/JavaScript bridge

| Type                                                                                                                                               | Use                                                                              |
|----------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| [`GrapheneBridge`](../../packages/common/src/main/java/io/github/trethore/graphene/api/bridge/GrapheneBridge.java)                                 | Exchange events and asynchronous requests between Java and the current document. |
| [`GrapheneBridgeJson`](../../packages/common/src/main/java/io/github/trethore/graphene/api/bridge/GrapheneBridgeJson.java)                         | Serialize or deserialize bridge payloads with Gson.                              |
| [`GrapheneBridgeRequestException`](../../packages/common/src/main/java/io/github/trethore/graphene/api/bridge/GrapheneBridgeRequestException.java) | Inspect a failed remote request's code, ID, and channel.                         |

The listener and handler interfaces in the same package support raw JSON strings or typed JSON helpers.

## Assets

| Type                                                                                                                          | Use                                                        |
|-------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| [`GrapheneAssetUrls`](../../packages/common/src/main/java/io/github/trethore/graphene/api/url/GrapheneAssetUrls.java)         | Create an asset URL using a default or explicit namespace. |
| [`GrapheneClasspathUrls`](../../packages/common/src/main/java/io/github/trethore/graphene/api/url/GrapheneClasspathUrls.java) | Create and interpret public `classpath://` asset URLs.     |
| [`AssetId`](../../packages/common/src/main/java/io/github/trethore/graphene/api/url/AssetId.java)                             | Represent and validate a normalized namespace/path pair.   |

Consumer app and HTTP URL factories are obtained from `GrapheneContext`.

## Runtime and DevTools

| Type                                                                                                                         | Use                                                                                  |
|------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| [`GrapheneRuntime`](../../packages/common/src/main/java/io/github/trethore/graphene/api/runtime/GrapheneRuntime.java)        | Observe initialization, runtime state, remote-debug port, DevTools, and HTTP server. |
| [`GrapheneHttpServer`](../../packages/common/src/main/java/io/github/trethore/graphene/api/runtime/GrapheneHttpServer.java)  | Read the shared HTTP server's running state and address.                             |
| [`GrapheneDevTools`](../../packages/common/src/main/java/io/github/trethore/graphene/api/devtools/GrapheneDevTools.java)     | Discover inspectable pages or the target associated with a browser session.          |
| [`DevToolsPageTarget`](../../packages/common/src/main/java/io/github/trethore/graphene/api/devtools/DevToolsPageTarget.java) | Read a target's identity, page metadata, and inspector URI.                          |

## Fabric UI integration

| Type                                                                                                                                                       | Use                                                         |
|------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| [`GrapheneWebViewWidget`](../../packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/fabric/api/widget/GrapheneWebViewWidget.java)            | Add an interactive browser to a Minecraft screen.           |
| [`BrowserSurface`](../../packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/fabric/api/surface/BrowserSurface.java)                         | Own and render a browser session through a custom GUI path. |
| [`BrowserSurfaceInputAdapter`](../../packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/fabric/api/surface/BrowserSurfaceInputAdapter.java) | Translate Minecraft and GLFW input for a custom surface.    |
| [`GrapheneScreens`](../../packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/fabric/api/screen/GrapheneScreens.java)                        | Read or change automatic web-view closure for a screen.     |

## Policy entry points

| Type                                                                                                                                               | Use                                                                       |
|----------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|
| [`BrowserBridgePolicy`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/bridge/BrowserBridgePolicy.java)               | Decide which main-frame documents receive the bridge.                     |
| [`BrowserNavigationPolicy`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/navigation/BrowserNavigationPolicy.java)   | Route, cancel, or externally open browser navigation.                     |
| [`BrowserDownloadPolicy`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/download/BrowserDownloadPolicy.java)         | Cancel a download, save it directly, or show a save dialog.               |
| [`BrowserContextMenuPolicy`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/menu/BrowserContextMenuPolicy.java)       | Select which browser-proposed context-menu commands are available.        |
| [`BrowserContextMenuPresenter`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/menu/BrowserContextMenuPresenter.java) | Present a configured menu asynchronously.                                 |
| [`BrowserFileDialogPresenter`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/dialog/BrowserFileDialogPresenter.java) | Present file selection asynchronously.                                    |
| [`BrowserJsDialogPresenter`](../../packages/common/src/main/java/io/github/trethore/graphene/api/browser/dialog/BrowserJsDialogPresenter.java)     | Present alert, confirm, prompt, and before-unload dialogs asynchronously. |

## Configuration

See [Configuration and defaults](configuration-and-defaults.md) for `GrapheneConfig`, HTTP hosting, remote debugging,
global settings, and browser defaults.
