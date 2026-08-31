# Browser Sessions, Views, Surfaces, and Widgets

Graphene separates browser ownership from its GUI and world-space projections.

```text
GrapheneWebViewWidget -> BrowserGuiSurface -> BrowserView -> BrowserSession -> JCEF browser
                         BrowserWorldSurface ---^
```

`BrowserView` owns the session and GPU texture. Surfaces borrow a view and decide how its pixels are projected.

## `GrapheneWebViewWidget`

Use the widget for browser content inside a normal Minecraft `Screen`.

It provides widget rendering, input forwarding, browser cursor rendering, resize integration, screen registration, and
automatic lifecycle management. This is the correct starting point for ordinary settings screens, dashboards, and
editors.

Widget constructors that accept an existing `BrowserView` or `BrowserGuiSurface` take ownership of the associated view.
Closing the widget closes that view, so it must not be shared with a longer-lived projection.

## `BrowserView`

Use a view when browser content must be rendered outside a widget or through more than one projection.

It provides:

- Ownership of one `BrowserSession`.
- Browser pixel-resolution management.
- Uploading the latest off-screen frame to one shared GPU texture.
- Normalized coordinate mapping.
- An experimental advanced texture escape hatch.

A view has no logical GUI size, world dimensions, or position. Close the view to release its browser and GPU resources.

## `BrowserGuiSurface`

Use a GUI surface to project a view through a custom GUI render path. It manages logical dimensions, automatic GUI-scale
resolution, explicit resolution, and screen-coordinate mapping.

The surface borrows its view and does not close it. Use `BrowserGuiSurfaceInputAdapter` to translate Minecraft window
coordinates into browser input.

## `BrowserWorldSurface`

Use a world surface to project a view onto a rectangular plane in local XY space. The plane is centered at the origin,
faces local positive Z, and maps the browser's top-left corner to its top-left UV coordinate.

The surface submits geometry through Minecraft's `SubmitNodeCollector`. The consumer controls its world transform by
modifying the supplied `PoseStack` before submission. `hitTest(...)` maps a world-space ray to UV and browser coordinates;
`BrowserWorldSurfaceInputAdapter` forwards input from that hit.

The surface does not register itself in the world or manage block entities, entities, focus selection, or occlusion.

## `BrowserSession`

The session is the loader-independent off-screen browser API. It provides navigation, state, script execution,
browser-pixel input, frame snapshots, downloads, and the Java/JavaScript bridge.

Use a session directly for a new loader integration, browser automation, or a renderer that does not use Graphene's GPU
texture.

## Frame flow

```text
Chromium frame -> BrowserSession -> BrowserView GPU texture
                                      |-> BrowserGuiSurface
                                      `-> BrowserWorldSurface
```

Frame notifications are latest-only. Intermediate frames may be coalesced when Chromium paints faster than Minecraft
uploads frames.

## Size and resolution

Graphene distinguishes:

- Browser resolution, owned by `BrowserView`, in browser pixels.
- Logical GUI dimensions, owned by `BrowserGuiSurface`, in GUI units.
- Rendered GUI dimensions supplied to a GUI draw call.
- Physical world dimensions, owned by `BrowserWorldSurface`, in local world units.

Automatic GUI resolution multiplies logical dimensions by Minecraft's GUI scale. World surfaces normally use an explicit
view resolution chosen for their expected physical size and viewing distance.

## Ownership and closure

- Close a widget -> its owned view and session close.
- Close a view -> its GPU texture and session close.
- GUI and world surfaces borrow their view and have no close operation.
- Close an input adapter -> only adapter-specific subscriptions close.

## Related documentation

- [Build your first web screen](../tutorials/first-web-screen.md)
- [Render a custom browser surface](../how-to/render-a-custom-browser-surface.md)
- [Render a browser in the world](../how-to/render-a-browser-in-the-world.md)
- [Manage browser lifecycle](../how-to/manage-browser-lifecycle.md)
