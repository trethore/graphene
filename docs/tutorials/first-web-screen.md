# Build Your First Web Screen

This tutorial creates a Fabric screen that displays HTML, CSS, and JavaScript packaged inside your mod.

## Prerequisites

- A Fabric client mod for a [supported Minecraft version](../reference/compatibility-and-installation.md).
- Java 21 or newer.
- A client entrypoint and a way to open a custom Minecraft `Screen`.

## 1. Add Graphene

Add Maven Central and Graphene to your Gradle build:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    modImplementation("io.github.trethore:graphene-ui:2.0.0")
}
```

Declare Graphene as a dependency in `fabric.mod.json` so Fabric loads it before your mod uses it:

```json
{
  "depends": {
    "grapheneui": ">=2.0.0"
  }
}
```

See [compatibility and installation](../reference/compatibility-and-installation.md) when updating versions.

## 2. Register your mod

Register during your Fabric client initializer. Keep the returned `GrapheneContext`; it owns your mod's asset URLs and
browser-session factory.

```java
package com.example.examplemod;

import io.github.trethore.graphene.api.Graphene;
import io.github.trethore.graphene.api.GrapheneContext;
import net.fabricmc.api.ClientModInitializer;

public final class ExampleModClient implements ClientModInitializer {
  private static GrapheneContext graphene;

  public static GrapheneContext graphene() {
    if (graphene == null) {
      throw new IllegalStateException("Example Mod has not initialized");
    }
    return graphene;
  }

  @Override
  public void onInitializeClient() {
    graphene = Graphene.register(ExampleModClient.class);
  }
}
```

`Graphene.register(ExampleModClient.class)` resolves the containing Fabric mod ID. Use `Graphene.register("examplemod")`
when class-based resolution is unavailable.

## 3. Add the web page

Create `src/main/resources/assets/examplemod/ui/index.html`:

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Example Mod</title>
    <link rel="stylesheet" href="style.css">
  </head>
  <body>
    <main>
      <h1>Rendered by Graphene</h1>
      <p id="status">JavaScript is loading...</p>
    </main>
    <script src="app.js"></script>
  </body>
</html>
```

Create `src/main/resources/assets/examplemod/ui/style.css`:

```css
:root {
  color-scheme: dark;
  font-family: system-ui, sans-serif;
}

body {
  display: grid;
  min-height: 100vh;
  margin: 0;
  color: #f7f7f7;
  background: linear-gradient(135deg, #12141d, #273358);
  place-items: center;
}

main {
  padding: 2rem;
  text-align: center;
}
```

Create `src/main/resources/assets/examplemod/ui/app.js`:

```javascript
document.getElementById("status").textContent =
    "HTML, CSS, and JavaScript are running.";
```

Replace `examplemod` with your actual Fabric mod ID.

## 4. Add a web-view widget

Create a screen that keeps one `GrapheneWebViewWidget` and resizes it with the screen:

```java
package com.example.examplemod;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ExampleWebScreen extends Screen {
  private GrapheneWebViewWidget webView;

  public ExampleWebScreen() {
    super(Component.literal("Example Web Screen"));
  }

  @Override
  protected void init() {
    int margin = 16;
    int viewWidth = Math.max(1, width - margin * 2);
    int viewHeight = Math.max(1, height - margin * 2);

    if (webView == null) {
      String url = ExampleModClient.graphene().appAssets().url("ui/index.html");
      webView =
          new GrapheneWebViewWidget(
              ExampleModClient.graphene(),
              this,
              margin,
              margin,
              viewWidth,
              viewHeight,
              Component.empty(),
              url);
    } else {
      webView.setPosition(margin, margin);
      webView.setSize(viewWidth, viewHeight);
    }

    addRenderableWidget(webView);
  }
}
```

Open the screen through your key binding, command, or existing UI:

```java
Minecraft.getInstance().setScreen(new ExampleWebScreen());
```

Graphene's Fabric screen integration forwards mouse, keyboard, focus, and resize events to the widget. Closing the
screen closes its registered web-view widgets by default.

## 5. Run the mod

Open `ExampleWebScreen`. The centered heading and JavaScript status message should appear inside Minecraft.

If the page is blank, check that:

- The resource namespace exactly matches your Fabric mod ID.
- The path passed to `appAssets().url(...)` is relative to that namespace.
- Graphene reached the `RUNNING` runtime state before the screen created its browser.

See [troubleshooting](../how-to/troubleshoot.md) for diagnostics.

## Next steps

- [Connect Java and JavaScript](connect-java-and-javascript.md).
- [Learn how packaged and development assets work](../how-to/manage-assets-and-frontend-development.md).
- [Understand widgets, surfaces, and sessions](../explanation/browser-layers.md).
