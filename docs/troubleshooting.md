# Troubleshooting

Common integration issues and direct fixes.

## `Graphene is not initialized` Error

Symptom:

- Creating a surface or using runtime APIs throws an initialization error.

Likely cause:

- No consumer was registered before Graphene usage.

Fix:

- Register in `ClientModInitializer` with `GrapheneCore.register("my-mod-id")`.
- Ensure registration happens before first web view or surface creation.

## Bridge Not Ready

Symptom:

- Java events/requests seem ignored right after page load.

Likely cause:

- JS bridge bootstrap has not sent `ready` yet.

Fix:

- Gate startup messages behind `bridge.onReady(...)`.
- Verify `globalThis.grapheneBridge` in DevTools.

## Request Fails With `handler_not_found`

Symptom:

- Java future fails or JS promise rejects with handler-not-found.

Likely cause:

- Missing request handler on the receiving side.

Fix:

- For JS -> Java: register `bridge.onRequest("channel", ...)`.
- For Java -> JS: register `grapheneBridge.handle("channel", ...)`.
- Check exact channel spelling.

## Invalid JSON Errors

Symptom:

- `IllegalArgumentException` when sending Java payloads.
- Request fails with `invalid_response`.

Likely cause:

- Java payload/response is not valid JSON text.

Fix:

- Send valid JSON strings from Java.
- Prefer serializer helpers (`emitJson`, `requestJson`, `GrapheneBridgeJson`) over manual string assembly.

## Input Mapping Feels Wrong

Symptom:

- Clicks land in wrong places, especially with scaling or cropping.

Likely cause:

- Surface size, rendered widget size, and/or viewBox are misaligned.

Fix:

- Keep render size aligned with `surfaceSize`.
- Re-check `setViewBox(...)` usage.
- Use `toBrowserPoint(...)` for custom input forwarding.

## Asset URL 404 Or Blank Page

Symptom:

- `app://assets/...` page does not load.

Likely cause:

- Wrong namespace or wrong resource path.

Fix:

- Verify file exists under `assets/<mod-id>/...`.
- Use helper APIs (`GrapheneAppUrls.asset(...)`).
- For debug samples in this repo, use `graphene_test/pages/...` paths.

## `Graphene HTTP server is not running`

Symptom:

- `GrapheneHttpUrls.asset(...)` throws `IllegalStateException`.

Likely cause:

- HTTP mode was not configured in `GrapheneConfig`.

Fix:

- Register with `.http(GrapheneHttpConfig.builder() ... .build())`.
- Verify `GrapheneCore.runtime().httpServer().isRunning()` before generating HTTP URLs.

## HTTP `fileRoot` Not Serving Updated Files

Symptom:

- HTTP requests return stale content or 404 while using `fileRoot(...)`.

Likely cause:

- Request path does not match `fileRoot` layout.
- File is outside configured root.

Fix:

- Confirm request path resolves to `<fileRoot>/<request-path>`.
- Request explicit files, for example `/assets/my-mod-id/web/index.html`.
- Keep namespaced assets paths (`/assets/<mod-id>/...`).

## Pending Requests Fail During Navigation

Symptom:

- In-flight Java requests fail when page changes.

Likely cause:

- Graphene intentionally fails pending requests on page change.

Fix:

- Retry after next `onReady`.
- Avoid navigation during critical request windows.

## Slow Shutdown Or CEF Disposal Warnings

Symptom:

- Client exit is slow or logs CEF disposal warnings.

Likely cause:

- Native CEF shutdown delay and/or lingering active surfaces.

Fix:

- Ensure widgets/surfaces are closed cleanly.
- Reduce long-running page tasks during shutdown.
- Capture logs and reproduce with minimal UI.

---

Next: [Testing](testing.md)
