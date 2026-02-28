# Quickstart

This guide shows the shortest path to render a web UI in a Minecraft screen.

## 1) Register Once During Client Init

```java
package com.example.mymod;

import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.GrapheneMod;

public final class MyModClient implements ClientModInitializer {
    private static GrapheneMod graphene;

    @Override
    public void onInitializeClient() {
        graphene = GrapheneCore.register("my-mod-id");
    }

    public static GrapheneMod graphene() {
        return graphene;
    }
}
```

For custom setup, use `GrapheneCore.register("my-mod-id", GrapheneConfig)`.

## 2) Create A Screen With `GrapheneWebViewWidget`

```java
package com.example.mymod.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tytoo.grapheneui.api.widget.GrapheneWebViewWidget;
import com.example.mymod.MyModClient;

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

        String url = MyModClient.graphene().appAssets().asset("web/index.html");
        webView = new GrapheneWebViewWidget(this, webX, webY, webWidth, webHeight, Component.empty(), url);
        addRenderableWidget(webView);
    }
}
```

Notes:

- The widget manages rendering and input forwarding for the browser surface.
- `ScreenMixin` auto-closes tracked web views by default when the screen closes.

## 3) Put Assets Under Your Namespace

Typical layout:

```text
src/client/resources/
  assets/
    my-mod-id/
      web/
        index.html
        app.js
        styles.css
```

Then load with:

```java
String url = MyModClient.graphene().appAssets().asset("web/index.html");
```

You can also use static helpers:

```java
String url = GrapheneAppUrls.asset("my-mod-id", "web/index.html");
```

For this repository's debug module, sample page URL:

```java
String url = GrapheneAppUrls.asset("graphene-ui-debug", "graphene_test/pages/welcome.html");
```

## 4) Open The Screen

Open from your keybind, command callback, or tick hook:

```java
import net.minecraft.client.Minecraft;

Minecraft.getInstance().setScreen(new MyWebScreen());
```

## 5) Optional: Enable Remote Debugging

Use this instead of the default registration in step 1:

```java
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneRemoteDebugConfig;

GrapheneConfig config = GrapheneConfig.builder()
        .remoteDebugging(GrapheneRemoteDebugConfig.builder()
                .randomPort()
                .allowedOrigins("https://chrome-devtools-frontend.appspot.com")
                .build())
        .build();

GrapheneCore.register("my-mod-id", config);
```

Query runtime state:

```java
int debugPort = GrapheneCore.runtime().getRemoteDebuggingPort();
// -1 when disabled, > 0 when enabled
```

## 6) Optional: Enable HTTP Mode

Use this instead of the default registration in step 1:

```java
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneHttpConfig;
import tytoo.grapheneui.api.url.GrapheneHttpUrls;

GrapheneConfig config = GrapheneConfig.builder()
        .http(GrapheneHttpConfig.builder()
                .bindHost("127.0.0.1")
                .randomPortInRange(20_000, 21_000)
                .fileRoot("C:/dev/my-ui-dist")
                .spaFallback("/assets/my-mod-id/web/index.html")
                .build())
        .build();

GrapheneCore.register("my-mod-id", config);

String httpUrl = GrapheneHttpUrls.asset("my-mod-id", "web/index.html");
```

HTTP resolution order:

1. `<fileRoot>/<request-path>`
2. classpath assets
3. optional `spaFallback` for non-`/assets/...` `GET` and `POST`

---

Next: [Bridge](bridge.md)
