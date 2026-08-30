# Render a Custom Browser GUI Surface

Use `BrowserView` with `BrowserGuiSurface` when a web interface must be rendered outside a normal Minecraft widget. For
ordinary screens, prefer `GrapheneWebViewWidget`, which already handles rendering, focus, input, cursor changes, resize,
and lifecycle integration.

## Create the view and surface

```java
BrowserView view =
        BrowserView.builder(context)
                .url(context.appAssets().url("ui/panel.html"))
                .build();

BrowserGuiSurface surface =
        BrowserGuiSurface.builder(view)
                .size(320, 180)
                .build();
```

The view owns the browser and GPU texture. The GUI surface borrows the view.

Render the latest browser frame from a GUI render path:

```java
surface.render(graphics, x, y);
```

Render at a different display size without changing the logical surface size:

```java
surface.render(graphics, x, y, renderedWidth, renderedHeight);
```

No frame is drawn before Chromium produces its first complete paint.

## Understand logical size and resolution

- **Logical size** controls GUI layout and input mapping.
- **Browser resolution** controls the Chromium viewport and GPU texture dimensions.
- **Rendered size** is the size supplied to a specific draw call.

Automatic resolution is enabled by default and derives browser pixels from logical size and Minecraft's GUI scale:

```java
surface.resize(400, 240);
surface.useAutoResolution();
```

Select a fixed browser resolution when the view needs a stable viewport:

```java
surface.setResolution(1280, 720);
```

Higher resolutions improve detail but increase Chromium rendering and texture-upload cost.

## Forward input

```java
BrowserGuiSurfaceInputAdapter input = new BrowserGuiSurfaceInputAdapter(surface);

input.setFocused(true);
input.mouseMoved(mouseX, mouseY, x, y, width, height, modifiers);
input.mouseButton(mouseX, mouseY, x, y, width, height, button, true, 1, modifiers);
input.mouseScrolled(mouseX, mouseY, x, y, width, height, horizontal, vertical, modifiers);
input.key(keyCode, scanCode, true, modifiers);
input.text(text, modifiers);
```

Pass the actual rendered bounds used for the surface. Incorrect bounds cause browser hit testing to disagree with the
displayed content.

## Close owned resources

```java
input.close();
view.close();
```

Closing the input adapter removes its adapter-specific subscriptions. It does not close the view. Use try-with-resources
for a view with a bounded synchronous lifetime.

## Advanced texture access

`BrowserView.texture()` uploads the latest available frame and returns borrowed access to the current Minecraft GPU
texture. This API is marked `@ExperimentalGrapheneApi` and is intended for custom renderers that cannot use a Graphene
surface.

Do not close or retain the returned GPU objects across frames. A resolution change can replace them.

## Next steps

- [Render a browser in the world](render-a-browser-in-the-world.md).
- [Understand the browser layer hierarchy](../explanation/browser-layers.md).
- [Control the underlying browser session](control-and-observe-the-browser.md).
