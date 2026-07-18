# Browser Sessions, Surfaces, and Widgets

Graphene exposes three browser layers so consumers can choose convenience or control without depending directly on JCEF.

```text
GrapheneWebViewWidget -> BrowserSurface -> BrowserSession -> JCEF browser
```

Each layer owns the layer to its right.

## `GrapheneWebViewWidget`

Use the widget for browser content inside a normal Minecraft `Screen`.

It provides:

- Minecraft widget rendering.
- Mouse, keyboard, scroll, text, and focus forwarding.
- Browser-requested cursor rendering.
- Resize integration.
- Registration with the containing screen.
- Automatic closure when the screen closes.
- Convenience navigation and bridge methods.

This is the correct starting point for settings screens, dashboards, editors, and other ordinary mod interfaces.

## `BrowserSurface`

Use a surface when browser pixels must be drawn through a custom GUI render path.

It provides:

- Ownership of one `BrowserSession`.
- Uploading the latest off-screen frame to a GPU texture.
- Rendering at a logical or explicit display size.
- Logical-size and browser-resolution management.
- Coordinate mapping for custom input forwarding.

A surface does not decide where it appears or automatically receive input. The integration that renders it must forward
focus and input, usually through `BrowserSurfaceInputAdapter` on Fabric.

## `BrowserSession`

The session is the loader-independent, off-screen browser API. It provides:

- Navigation and history.
- URL, title, load, and console state.
- Script execution, zoom, and find-in-page.
- Normalized pointer, keyboard, scroll, and text input.
- Frame snapshots.
- Downloads.
- The Java/JavaScript bridge.

Use a session directly when building a new loader integration, a nonstandard renderer, or browser automation that does
not need a Minecraft surface.

## Frame flow

Chromium paints off-screen frames into CPU-accessible buffers. `BrowserSession` exposes immutable frame snapshots.
`BrowserSurface` uploads the latest complete frame and renders it through Minecraft.

Frame notifications are latest-only. Intermediate frames may be coalesced when the browser paints faster than the
platform thread can process notifications. Consumers should render the latest state rather than treat frame callbacks as
a lossless stream.

## Size and resolution

Graphene distinguishes:

- Logical surface dimensions used for layout and input mapping.
- Browser pixel dimensions used by Chromium.
- Rendered dimensions used by a particular draw call.

Automatic resolution multiplies logical dimensions by Minecraft's GUI scale. Fixed resolution provides a stable browser
viewport but requires the consumer to choose an appropriate pixel density.

## Input flow

```text
Minecraft/GLFW event
  -> GrapheneWebViewWidget or BrowserSurfaceInputAdapter
    -> normalized browser input record
      -> BrowserSession
        -> Chromium
```

Pointer coordinates must be mapped from rendered surface bounds into browser pixels. Focus must also be synchronized so
Chromium knows whether to accept keyboard and text input.

## Ownership and closure

- Close a widget -> its input adapter, surface, and session close.
- Close a surface -> its renderer and session close.
- Close a session -> native browser resources are released.
- Close an input adapter -> only adapter-owned subscriptions close.

Close the highest-level object owned by your integration. Repeated closure is safe.

## Related documentation

- [Build your first web screen](../tutorials/first-web-screen.md)
- [Render a custom browser surface](../how-to/render-a-custom-browser-surface.md)
- [Manage browser lifecycle](../how-to/manage-browser-lifecycle.md)
