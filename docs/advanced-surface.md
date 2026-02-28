# Advanced Surface

`BrowserSurface` gives direct control over rendering, sizing, viewport cropping, navigation, and bridge access.

## Builder Options

- `url(String)` initial URL, default `about:blank`
- `transparent(boolean)` off-screen transparency, default `true`
- `surfaceSize(int, int)` logical render size
- `resolution(int, int)` explicit browser pixel resolution
- `autoResolution()` resolution follows surface size and window scale
- `viewBox(int, int, int, int)` crop source content region
- `client(CefClient)` custom CEF client
- `requestContext(CefRequestContext)` custom CEF request context
- `requestContextCustomizer(Consumer<CefRequestContext>)` mutate context before use
- `renderer(GrapheneRenderer)` custom renderer
- `config(BrowserSurfaceConfig)` browser settings config
- `maxFps(int)` convenience setter for windowless frame rate
- `settingsCustomizer(Consumer<CefBrowserSettings>)` mutate low-level CEF settings
- `owner(Object)` register owner for lifecycle-managed cleanup

## Sizing Modes

Fixed resolution example:

```java
BrowserSurface surface = BrowserSurface.builder()
        .url("app://assets/my-mod-id/web/index.html")
        .surfaceSize(400, 240)
        .resolution(800, 480)
        .build();
```

Auto resolution example:

```java
surface.useAutoResolution();
surface.setSurfaceSize(600, 340);
```

## ViewBox Cropping

```java
surface.setViewBox(100, 50, 300, 200);
```

Reset to full frame:

```java
surface.resetViewBox();
```

## Rendering

If you are not using `GrapheneWebViewWidget`, call `render(...)` every frame.

```java
surface.render(guiGraphics, x, y, width, height);
```

Overloads are available for `GuiGraphics` and `GrapheneRenderTarget`.
`render(...)` also triggers bridge bootstrap fallback checks and paint frame updates.

## Navigation And State

`BrowserSurface` exposes browser navigation and state:

- `loadUrl`, `reload`, `goBack`, `goForward`
- `currentUrl`, `canGoBack`, `canGoForward`, `isLoading`

## Input Adapter

Use `BrowserSurfaceInputAdapter` for custom input pipelines:

```java
BrowserSurfaceInputAdapter input = new BrowserSurfaceInputAdapter(surface);
input.setFocused(true);

input.mouseMoved(localMouseX, localMouseY, width, height);
input.mouseClicked(button, isDoubleClick, localMouseX, localMouseY, width, height);
input.mouseReleased(button, localMouseX, localMouseY, width, height);
input.mouseDragged(button, localMouseX, localMouseY, width, height);
input.mouseScrolled(localMouseX, localMouseY, scrollY, width, height);
```

Keyboard forwarding methods exist for both event objects and raw key values.

## Coordinate Mapping Helpers

For manual forwarding, use:

- `toBrowserPoint(...)`
- `toBrowserX(...)`
- `toBrowserY(...)`

These apply current viewBox and rendered dimensions.

## Ownership And Cleanup

Owner-tracked lifecycle:

```java
BrowserSurface surface = BrowserSurface.builder()
        .owner(owner)
        .build();

surface.setOwner(otherOwner);
surface.clearOwner();

surface.close();
```

Always close surfaces you create.

---

Next: [Troubleshooting](troubleshooting.md)
