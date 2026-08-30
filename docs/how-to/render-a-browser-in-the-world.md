# Render a Browser in the World

`BrowserWorldSurface` projects a `BrowserView` onto a rectangular plane in local XY space. The browser remains a 2D
framebuffer; the world surface places that framebuffer in a 3D render path.

## Create a view and world surface

World views normally use an explicit browser resolution:

```java
BrowserView view =
        BrowserView.builder(context)
                .url(context.appAssets().url("ui/monitor.html"))
                .resolution(1024, 576)
                .build();

BrowserWorldSurface surface =
        BrowserWorldSurface.builder(view)
                .dimensions(2.0F, 1.125F)
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

The default plane is depth-tested, emissive, and back-face culled. Enable rendering from both sides when required:

```java
surface.setDoubleSided(true);
```

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
