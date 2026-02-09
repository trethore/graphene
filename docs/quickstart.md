# Quickstart

This guide shows the minimal path to render a web UI in a Minecraft screen.

## 1) Initialize Graphene Once

Call `GrapheneCore.init()` in your client initializer.

```java
package com.example.mymod;

import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.GrapheneCore;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneCore.init();
    }
}
```

## 2) Create A Screen With A WebView

`GrapheneWebViewWidget` wraps a browser surface and handles rendering + input forwarding.

```java
package com.example.mymod.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import tytoo.grapheneui.browser.GrapheneWebViewWidget;
import tytoo.grapheneui.cef.GrapheneClasspathUrls;

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

        String url = GrapheneClasspathUrls.asset("graphene_test/welcome.html");
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

For your own mod assets, use a direct classpath URL:

```java
String url = "classpath:///assets/my-mod-id/web/index.html";
```

Or keep using Graphene test assets while integrating:

```java
String url = GrapheneClasspathUrls.asset("graphene_test/example-bridge.html");
```

## 4) Open The Screen

Open your screen from a keybind, command, or other client event:

```java
minecraft.setScreen(new MyWebScreen());
```

![Quickstart result](images/demo.png)

## Next Step

Once your page renders, wire Java <-> JS messaging with `GrapheneBridge`: `docs/bridge.md`.

---
Next: [Bridge](bridge.md)
