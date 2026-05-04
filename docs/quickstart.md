# Quickstart

This guide shows the shortest path to render a web UI in a Minecraft screen.

**Flow**

| Step | Action                                              |
|------|-----------------------------------------------------|
| 1    | Register your mod with `GrapheneCore.register(...)` |
| 2    | Create a `GrapheneWebViewWidget` in a `Screen`      |
| 3    | Put assets under `assets/<mod-id>/...`              |
| 4    | Open the screen from a client-side callback         |
| 5    | Optionally enable DevTools or HTTP mode             |

**Main API**

- `GrapheneCore.register(...)`
- `GrapheneCore.handle(...)`
- `GrapheneWebViewWidget`

## Register Once During Client Init

```java
package com.example.mymod;

import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneCore.register(MyModClient.class);
    }
}
```

For custom setup, use `GrapheneCore.register(MyModClient.class, GrapheneConfig)`.
Later, access the scoped handle with `GrapheneCore.handle(MyModClient.class)`.

Preferred usage is still the anchor-class form because it keeps registration and handle lookup tied to a real mod class.
If a project has an unusual source-set or entrypoint layout, you can register explicitly by Fabric mod id instead:

```java
GrapheneCore.register("my-mod-id");
GrapheneHandle graphene = GrapheneCore.handle("my-mod-id");
```

## Create a Screen With `GrapheneWebViewWidget`

```java
package com.example.mymod.client.screen;

import com.example.mymod.MyModClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.widget.GrapheneWebViewWidget;

public final class MyWebScreen extends Screen {
    private GrapheneWebViewWidget webView;

    public MyWebScreen() {
        super(Component.literal("My Web Screen"));
    }

    @Override
    protected void init() {
        int margin = 8;
        int webX = margin;
        int webY = margin;
        int webWidth = width - margin * 2;
        int webHeight = height - margin * 2;

        String url = GrapheneCore.handle(MyModClient.class).appAssets().asset("web/index.html");
        webView = new GrapheneWebViewWidget(this, webX, webY, webWidth, webHeight, Component.empty(), url);
        addRenderableWidget(webView);
    }
}
```

Notes:

- The widget manages rendering and input forwarding for the browser surface.
- `ScreenMixin` auto-closes tracked web views by default when the screen closes.

## Put Assets Under Your Namespace

Typical layout in a consumer Fabric mod:

```text
src/client/resources/
  assets/
    my-mod-id/
      web/
        index.html
        app.js
        styles.css
```

In this repository, version-specific assets live under `fabric-1.21.11/src/client/resources/` or the matching
`fabric-<minecraft-version>/src/client/resources/` module.

Then load with:

```java
String url = GrapheneCore.handle(MyModClient.class).appAssets().asset("web/index.html");
```

For this repository's debug module, sample page URL:

```java
String url = GrapheneCore.handle(GrapheneDebugClient.class).appAssets().asset("graphene_test/pages/welcome.html");
```

## Open the Screen

Open the screen from any client-side callback that already has access to the Minecraft client, such as a keybind
handler, command callback, or tick hook.
For example, inside a Fabric callback that exposes a `client` parameter:

```java
client.setScreen(new MyWebScreen());
```

## Optional: Enable Remote Debugging

Use this instead of the default registration in step 1:

```java
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.config.GrapheneRemoteDebugConfig;

GrapheneConfig config = GrapheneConfig.builder()
        .global(GrapheneGlobalConfig.builder()
                .remoteDebugging(GrapheneRemoteDebugConfig.builder()
                        .randomPort()
                        .allowedOrigins("*")
                        .build())
                .build())
        .build();

GrapheneCore.register(MyModClient.class, config);
// Alternative for unusual project layouts:
// GrapheneCore.register("my-mod-id", config);
```

Open DevTools for a surface:

```java
GrapheneCore.runtime().openDevTools(surface);
```

Query runtime state:

```java
int debugPort = GrapheneCore.runtime().getRemoteDebuggingPort();
// -1 when disabled, > 0 when enabled
```

## Optional: Enable HTTP Mode

Use this instead of the default registration in step 1:

```java
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneHttpConfig;

GrapheneConfig config = GrapheneConfig.builder()
        .container(GrapheneContainerConfig.builder()
                .http(GrapheneHttpConfig.builder()
                        .bindHost("127.0.0.1")
                        .randomPortInRange(20_000, 21_000)
                        .fileRoot("C:/dev/my-ui-dist")
                        .spaFallback("/assets/my-mod-id/web/index.html")
                        .build())
                .build())
        .build();

GrapheneCore.register(MyModClient.class, config);
// Alternative for unusual project layouts:
// GrapheneCore.register("my-mod-id", config);

String classpathHttpUrl = GrapheneCore.handle(MyModClient.class).httpAssets().asset("web/index.html");
String mountedHttpUrl = GrapheneCore.handle(MyModClient.class).httpUrl("web/index.html");
```

HTTP resolution order for `handle.httpUrl("...")`:

1. `<fileRoot>/<request-path>`
2. `assets/<mod-id>/<request-path>` on the classpath
3. optional `spaFallback`

`handle.httpAssets().asset(...)` always targets shared classpath assets under `/assets/<mod-id>/...`.

Use `handle.httpUrl(...)` when you want filesystem-first development through `fileRoot(...)`, and use
`handle.httpAssets().asset(...)` when you only need shared classpath assets.
