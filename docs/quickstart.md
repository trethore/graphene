# Quickstart

This guide shows the minimal path to render a web UI in a Minecraft screen.

## 1) Register Your Mod Once

Call `GrapheneCore.init("your-mod-id")` in your client initializer.

```java
package com.example.mymod;

import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneCore.init("my-mod-id");
    }
}
```

For custom setup, use `GrapheneCore.init("my-mod-id", GrapheneConfig)` with `jcefDownloadPath(...)` and
`extensionFolder(...)`. Graphene stores JCEF in `<jcef-mvn-version>/<platform>` under the configured base path.

If your framework prefers `http://` origins, enable Graphene's loopback HTTP server:

```java
GrapheneConfig config = GrapheneConfig.builder()
        .http(GrapheneHttpConfig.builder()
                .bindHost("127.0.0.1")
                .randomPortInRange(20_000, 21_000)
                .fileRoot("C:/dev/my-ui-dist")
                .spaFallback("/assets/my-mod-id/web/index.html")
                .build())
        .build();

GrapheneCore.init("my-mod-id", config);
```

Inspect runtime HTTP server state:

```java
GrapheneHttpServer server = GrapheneCore.runtime().httpServer();
int port = server.port();
String baseUrl = server.baseUrl();
```

HTTP request resolution order:

- If `fileRoot(...)` is set, Graphene tries `<fileRoot>/<request-path>` first.
- If no filesystem file exists, Graphene falls back to classpath assets.
- If still missing, `spaFallback(...)` applies for non-`/assets/...` `GET` requests.

## 2) Create A Screen With A WebView

`GrapheneWebViewWidget` wraps a browser surface and handles rendering + input forwarding.

```java
package com.example.mymod.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import tytoo.grapheneui.api.widget.GrapheneWebViewWidget;
import tytoo.grapheneui.api.url.GrapheneAppUrls;

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

        String url = GrapheneAppUrls.asset("my-mod-id", "web/index.html");
        webView = new GrapheneWebViewWidget(this, webX, webY, webWidth, webHeight, Component.empty(), url);
        addRenderableWidget(webView);
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
```

Notes:

- Graphene injects a mixin into `Screen`, so widgets are tracked and auto-closed by default when the screen closes.
- You can still call `webView.close()` manually if you need explicit timing.

## 3) Load Your Own HTML

For your own mod assets, use the namespace-aware helper:

```java
String url = GrapheneAppUrls.asset("my-mod-id", "web/index.html");
```

If you are working in this repository's debug module, you can also load debug sample assets:

```java
String url = GrapheneAppUrls.asset("graphene-ui-debug", "graphene_test/example-bridge.html");
```

When HTTP mode is enabled, build URLs from classpath assets with:

```java
String url = GrapheneHttpUrls.asset("my-mod-id", "web/index.html");
```

With the `fileRoot(...)` example above, this URL maps to:

- `http://127.0.0.1:<port>/assets/my-mod-id/web/index.html`
- filesystem path `C:/dev/my-ui-dist/assets/my-mod-id/web/index.html` (when present)

## 4) Open The Screen

Open your screen from a keybind, command, or other client event:

```java
minecraft.setScreen(new MyWebScreen());
```

![Quickstart result](images/demo.png)

## Next Step

Once your page renders, wire Java <-> JS messaging with `GrapheneBridge`: [bridge.md](bridge.md).

---
Next: [Bridge](bridge.md)
