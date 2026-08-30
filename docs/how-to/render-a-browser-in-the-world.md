# Render a Browser in the World

`BrowserWorldSurface` projects a `BrowserView` onto a rectangular plane in local XY space. The browser remains a 2D
framebuffer; the world surface places that framebuffer in a 3D render path.

Use an opaque browser when the surface content does not need transparency. Opaque surfaces write depth, preventing
translucent world geometry behind them from showing through.

## Create a view and world surface

World views normally use an explicit browser resolution:

```java
BrowserView view =
        BrowserView.builder(context)
                .url(context.appAssets().url("ui/monitor.html"))
                .options(BrowserOptions.builder().transparent(false).build())
                .resolution(1024, 576)
                .build();

BrowserWorldSurface surface =
        BrowserWorldSurface.builder(view)
                .dimensions(2.0F, 1.125F)
                .fullBright(true)
                .transparencyMode(WorldSurfaceTransparency.OPAQUE)
                .build();
```

The plane is centered at the local origin and faces local positive Z. Its dimensions are local world units.

## Submit the plane

Apply the desired world transform to a `PoseStack`, then submit the surface from a block entity renderer, entity renderer,
or Fabric level-render callback:

```java
poseStack.pushPose();
poseStack.translate(x, y, z);
poseStack.mulPose(rotation);
surface.submit(submitNodeCollector, poseStack);
poseStack.popPose();
```

The plane is depth-tested and back-face culled. Full-bright mode uses maximum light and disables directional surface
shading. It does not disable distance or environmental fog. Enable rendering from both sides when required:

```java
surface.setDoubleSided(true);
```

World-surface transparency is independent of whether the browser preserves its alpha channel:

- `OPAQUE` ignores alpha for blending and writes depth.
- `CUTOUT` discards low-alpha pixels and writes depth for the remaining pixels.
- `BLENDED` preserves partial alpha but does not write depth.

Use `BrowserOptions.transparent(true)` when `CUTOUT` or `BLENDED` needs the page's alpha channel. A surface-wide opacity
multiplier is also available:

```java
surface.setTransparencyMode(WorldSurfaceTransparency.BLENDED);
surface.setOpacity(0.5F);
```

An opacity below `1.0` uses blended rendering regardless of the selected transparency mode because partial opacity
cannot be represented by an opaque or cutout pipeline.

Graphene does not register the surface in the world or manage chunk and entity lifecycle. The owning integration must
close the `BrowserView` when its world object is permanently removed.

## Map a camera ray to browser coordinates

Pass the same local-to-world transform used for rendering:

```java
Optional<BrowserWorldSurfaceHit> hit =
        surface.hitTest(rayOrigin, rayDirection, localToWorld);
```

The hit contains normalized UV coordinates, browser pixel coordinates, the world hit position, and distance from the ray
origin.

Forward a hit through the world input adapter:

```java
BrowserWorldSurfaceInputAdapter input = new BrowserWorldSurfaceInputAdapter(surface);

input.setFocused(true);
input.mouseMoved(hit.get(), modifiers);
input.mouseButton(hit.get(), button, true, clickCount, modifiers);
input.mouseScrolled(hit.get(), horizontal, vertical, modifiers);
input.key(keyCode, scanCode, true, modifiers);
input.text(text, modifiers);
```

The caller remains responsible for selecting the nearest surface, comparing the hit against world occlusion, assigning
focus, and suppressing gameplay input while the browser is active.

## Close resources

```java
input.close();
view.close();
```

The world surface borrows its view and does not need to be closed.
