# JavaScript Bridge API

Graphene injects `globalThis.grapheneBridge` into main-frame documents allowed by the browser's `BrowserBridgePolicy`.

## Readiness

### `isReady()`

Returns whether the Java/JavaScript handshake completed.

```javascript
if (globalThis.grapheneBridge.isReady()) {
  // Messages can be sent.
}
```

### `ready()`

Returns a promise that resolves when the bridge becomes ready.

```javascript
await globalThis.grapheneBridge.ready();
```

### `onReady(listener)`

Calls `listener` when the bridge becomes ready and returns an unsubscribe function. If already ready, the listener is
scheduled asynchronously when the environment supports it.

```javascript
const unsubscribe = globalThis.grapheneBridge.onReady(() => connectUi());
```

## Events

### `on(channel, listener)`

Registers a listener for Java -> JavaScript events. It returns an unsubscribe function.

```javascript
const unsubscribe = bridge.on("example:status", (payload) => {
  console.log(payload);
});
```

Multiple listeners can observe the same channel.

### `off(channel, listener)`

Removes one previously registered event listener.

```javascript
bridge.off("example:status", listener);
```

### `emit(channel, payload)`

Sends a one-way JavaScript -> Java event and returns a promise for message delivery.

```javascript
await bridge.emit("example:changed", { value: 42 });
```

An absent Java event listener does not produce a response value.

## Requests

### `handle(channel, handler)`

Registers the JavaScript handler for Java -> JavaScript requests and returns an unregister function.

```javascript
const unregister = bridge.handle("example:lookup", async (payload) => {
  return { result: await lookup(payload.key) };
});
```

One handler is active per channel. Registering another handler replaces the previous handler. The unregister function
removes the handler only if it is still the active one.

Handlers can return a value or promise. A thrown error or rejected promise becomes a failed Java request.

### `request(channel, payload)`

Sends a JavaScript -> Java request and returns a promise for its response payload.

```javascript
const response = await bridge.request("example:greet", { name: "Alex" });
console.log(response.message);
```

The promise rejects when transport fails, no Java handler exists, the Java handler fails, or Graphene receives an
invalid response.

## Payloads

`undefined` payloads are normalized to `null`. Use JSON-compatible values: null, booleans, numbers, strings, arrays, and
objects with serializable properties.

On Java, `GrapheneBridge` offers raw JSON-string methods and Gson-backed helpers:

- `emitJson(...)`
- `requestJson(...)`
- `onEventJson(...)`
- `onRequestJson(...)`

Validate data at the boundary even when it is deserialized into a Java type.

## Errors

Failed JavaScript requests reject with an `Error`. Graphene attaches the remote error code to `error.code` when
available.

```javascript
try {
  await bridge.request("example:missing", null);
} catch (error) {
  console.error(error.code, error.message);
}
```

Java request failures complete exceptionally with `GrapheneBridgeRequestException`, which exposes the code, request ID,
and channel.

## Document lifetime

Bridge listeners and handlers belong to the current JavaScript document. Navigation creates a new document and a new
page-side registration state. Install handlers from each page's startup code.

Java subscriptions remain attached to the browser bridge until unsubscribed or the browser closes. Decide whether a Java
handler applies across navigation before retaining it.

## Channel names

Use consumer-owned names such as:

```text
example:settings:load
example:settings:save
```

Channels beginning with `graphene:` are reserved for Graphene platform integrations.

## Security

The bridge exists only in documents allowed by `BrowserBridgePolicy`. Packaged Graphene documents are allowed by
default. Do not broaden exposure to remote origins without reviewing every Java handler reachable from those pages.

## Related documentation

- [Bridge tutorial](../tutorials/connect-java-and-javascript.md)
- [Assets, origins, and bridge security](../explanation/assets-origins-and-bridge-security.md)
- [Configure browser policies](../how-to/configure-browser-policies.md)
