# Render a Custom Browser Surface

Use `BrowserSurface` when a web interface must be rendered outside a normal Minecraft widget. For ordinary screens,
prefer `GrapheneWebViewWidget`, which already handles rendering, focus, input, cursor changes, resize, and lifecycle
integration.

## Create and render a surface

```java
BrowserSurface surface =
        BrowserSurface.builder(context)
                .url(context.appAssets().url("ui/panel.html"))
                .size(320, 180)
                .build();
```

Render the latest browser frame from your GUI render path:

```java
surface.render(graphics, x, y);
```

Render at a different display size without changing the logical surface size:

```java
surface.render(graphics, x, y, renderedWidth, renderedHeight);
```

No frame is drawn before Chromium produces its first complete paint.

## Understand logical size and resolution

- **Logical size** controls layout and input mapping in Minecraft GUI units.
- **Browser resolution** controls the Chromium viewport and rendered pixel dimensions.
- **Rendered size** is the size supplied to a specific `render(...)` call.

Automatic resolution is enabled by default and derives browser pixels from logical size and Minecraft's GUI scale:

```java
surface.resize(400, 240);
surface.useAutoResolution();
```

Select a fixed browser resolution when the surface needs a stable viewport:

```java
surface.setResolution(1280, 720);
```

Higher resolutions improve detail but increase browser rendering and texture-upload cost.

## Map input coordinates

If a surface is displayed at `renderedWidth` by `renderedHeight`, map local display coordinates before creating
low-level browser input:

```java
int browserX = surface.toBrowserX(localX, renderedWidth);
int browserY = surface.toBrowserY(localY, renderedHeight);
```

## Forward Fabric input

`BrowserSurfaceInputAdapter` translates Minecraft and GLFW input into Graphene's loader-independent input records:

```java
BrowserSurfaceInputAdapter input = new BrowserSurfaceInputAdapter(surface);

input.setFocused(true);
input.mouseMoved(mouseX, mouseY, x, y, width, height, modifiers);
input.mouseButton(mouseX, mouseY, x, y, width, height, button, true, 1, modifiers);
input.mouseScrolled(mouseX, mouseY, x, y, width, height, horizontal, vertical, modifiers);
input.key(keyCode, scanCode, true, modifiers);
input.text(text, modifiers);
```

Pass the real rendered bounds used for the surface. Incorrect bounds cause pointer coordinates and page hit-testing to
disagree.

## Close owned resources

```java
input.close();
surface.close();
```

Closing a `BrowserSurface` closes its renderer and owned `BrowserSession`. Closing the input adapter removes its
adapter-specific bridge subscriptions but does not close the surface.

Use `try`-with-resources where the surface has a bounded synchronous lifetime. For long-lived render objects, close both
resources from the owning object's lifecycle hook.

## Next steps

- [Understand the browser layer hierarchy](../explanation/browser-layers.md).
- [Control the underlying browser session](control-and-observe-the-browser.md).
- [Troubleshoot focus, input, and blurry rendering](troubleshoot.md).
