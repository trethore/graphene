# Advanced Surface

`BrowserSurface` gives direct control over browser rendering and sizing behavior.

## Builder Options

- `url(String)`: initial URL (default `about:blank`)
- `transparent(boolean)`: renderer transparency (default `true`)
- `surfaceSize(int,int)`: logical UI size in screen space
- `resolution(int,int)`: explicit browser pixel resolution
- `autoResolution()`: tie browser resolution to surface size * window scale
- `viewBox(int,int,int,int)`: render a cropped source region
- `client(CefClient)`: use a specific CEF client
- `requestContext(CefRequestContext)`: custom context/profile
- `requestContextCustomizer(Consumer<CefRequestContext>)`: mutate context before use
- `renderer(GrapheneRenderer)`: custom renderer implementation
- `config(BrowserSurfaceConfig)`: full browser settings config
- `maxFps(int)`: convenience for windowless frame rate
- `settingsCustomizer(...)`: mutate underlying `CefBrowserSettings`

## Example: Fixed Internal Resolution

Useful when you want predictable pixel density regardless of window scale.

```java
BrowserSurface surface = BrowserSurface.builder()
        .url("classpath:///assets/my-mod-id/web/index.html")
        .surfaceSize(400, 240)
        .resolution(800, 480)
        .build();
```

## Example: Auto Resolution

```java
surface.useAutoResolution();
surface.setSurfaceSize(600, 340);
```

With auto resolution enabled, Graphene recalculates browser pixel size from current window scale.

## Example: ViewBox Cropping

Render only part of a larger browser frame:

```java
surface.setViewBox(100, 50, 300, 200);
```

Reset to full frame:

```java
surface.resetViewBox();
```

## Example: Tune Frame Rate

```java
BrowserSurface surface = BrowserSurface.builder()
        .url("about:blank")
        .surfaceSize(200, 120)
        .maxFps(30)
        .build();
```

Equivalent config style:

```java
BrowserSurfaceConfig config = BrowserSurfaceConfig.builder()
        .maxFps(30)
        .settingsCustomizer(settings -> {
            // advanced CefBrowserSettings tuning
        })
        .build();
```

## Example: Manual Render Integration

If you do not use `GrapheneWebViewWidget`, call `render(...)` each frame:

```java
surface.render(guiGraphics, x, y, width, height);
```

`BrowserSurface` now flushes pending paint updates automatically on each render call.

## Input Forwarding Adapter

For custom input pipelines, use `BrowserSurfaceInputAdapter`:

```java
BrowserSurfaceInputAdapter input = new BrowserSurfaceInputAdapter(surface);
input.setFocused(true);

input.mouseMoved(localMouseX, localMouseY, width, height);
input.mouseClicked(button, isDoubleClick, localMouseX, localMouseY, width, height);
input.mouseReleased(button, localMouseX, localMouseY, width, height);
input.mouseScrolled(localMouseX, localMouseY, scrollY, width, height);
```

## Input Mapping Helpers

For custom input pipelines, use coordinate mappers:

- `toBrowserPoint(...)`
- `toBrowserX(...)`
- `toBrowserY(...)`

These account for current viewBox and rendered dimensions.

## Ownership And Cleanup

Assign an owner once and rely on `close()` for cleanup:

```java
BrowserSurface surface = BrowserSurface.builder()
        .owner(owner)
        .build();

// or later
surface.setOwner(owner);
surface.clearOwner();

surface.close();
```

---
Next: [Troubleshooting](troubleshooting.md)
