# Troubleshooting

Common integration issues and direct fixes.

## Bridge Not Ready

Symptom:

- Java events/requests appear to do nothing immediately after page load.

Likely cause:

- JS bridge bootstrap has not finished and `ready` handshake was not received yet.

Fix:

- Register `bridge.onReady(...)` and send startup messages only after it fires.
- Verify `globalThis.grapheneBridge` exists in DevTools console.

## Request Fails With `handler_not_found`

Symptom:

- Request future completes exceptionally or JS promise rejects with handler-not-found error.

Likely cause:

- No handler registered on the target side for that channel.

Fix:

- Ensure `bridge.onRequest("channel", ...)` exists on Java for JS->Java calls.
- Ensure `grapheneBridge.handle("channel", ...)` exists on JS for Java->JS calls.
- Check exact channel spelling and namespace.

## Invalid JSON Errors

Symptom:

- `IllegalArgumentException` on Java emit/request payloads.
- `invalid_response` from bridge.

Likely cause:

- Payload/response strings are not valid JSON.

Fix:

- Always send proper JSON text from Java (object/array/primitive all valid).
- If building JSON manually, prefer a JSON library instead of string concatenation.

## Surface Renders But Input Feels Off

Symptom:

- Clicks land in wrong positions, especially with scaling/cropping.

Likely cause:

- Render size and source viewBox mapping are inconsistent.

Fix:

- Keep surface size and rendered widget size in sync.
- Re-check `setViewBox(...)` usage.
- Use `toBrowserPoint(...)` when forwarding custom input.

## Blank Page / 404 For Classpath Asset

Symptom:

- Page fails to load from `classpath:///...`.

Likely cause:

- Wrong asset path or wrong namespace under `assets/`.

Fix:

- Confirm resource exists at `src/client/resources/assets/<mod-id>/...`.
- Use `classpath:///assets/<mod-id>/...` URLs.
- For Graphene bundled samples, use `GrapheneClasspathUrls.asset("...")`.

## Pending Requests Fail On Navigation

Symptom:

- In-flight Java requests fail when page changes.

Likely cause:

- Bridge intentionally fails pending requests when page load starts.

Fix:

- Retry request after next `onReady`.
- Avoid navigation during critical request windows.

## CEF Process Does Not Shut Down Cleanly

Symptom:

- Client exits slowly or logs timeout warnings while disposing CEF.

Likely cause:

- Native CEF termination delay.

Fix:

- Ensure surfaces are closed cleanly.
- Avoid long-running JS tasks during shutdown.
- Collect logs and reproduce with minimal UI for diagnosis.

---
Next: [Testing](testing.md)
