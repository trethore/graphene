# Bridge

`GrapheneBridge` is Graphene's messaging layer between Java and page JavaScript.

- Java API: `GrapheneBridge` from each `BrowserSurface` or `GrapheneWebViewWidget`
- JS API: `globalThis.grapheneBridge` injected into the page

## Message Model

- `event`: fire-and-forget payload on a channel
- `request`: request/response call with success or error
- `ready`: JS bootstrap handshake that marks the bridge ready

## Java API

Get the bridge:

```java
GrapheneBridge bridge = webView.bridge();
```

Subscribe:

```java
GrapheneBridgeSubscription readySub = bridge.onReady(() -> {
    // JS bootstrap is ready for this page.
});

GrapheneBridgeSubscription eventSub = bridge.onEvent("chat:ping", (channel, payloadJson) -> {
    // payloadJson is a JSON string.
});

GrapheneBridgeSubscription requestSub = bridge.onRequest("math:add", (channel, payloadJson) -> {
    return CompletableFuture.completedFuture("{\"result\":42}");
});
```

Send from Java:

```java
bridge.emit("chat:ping", "{\"text\":\"hello\"}");

CompletableFuture<String> responseFuture = bridge.request("math:add", "{\"a\":10,\"b\":32}");
responseFuture.thenAccept(responseJson -> {
    // responseJson is JSON from JS.
});
```

Typed JSON helpers are available:

- `emitJson(channel, payloadObject)`
- `requestJson(channel, payloadObject, responseType)`
- `onEventJson(channel, payloadType, listener)`
- `onRequestJson(channel, requestType, handler)`

## JavaScript API

The injected object:

- `on(channel, listener)` returns unsubscribe function
- `off(channel, listener)`
- `handle(channel, handler)` returns unhandle function
- `emit(channel, payload)` returns a promise
- `request(channel, payload)` returns a promise

```js
const bridge = globalThis.grapheneBridge;

const off = bridge.on("chat:ping", (payload) => {
  console.log("Java event", payload);
});

const unhandle = bridge.handle("math:add", (payload) => {
  return { result: Number(payload.a) + Number(payload.b) };
});

await bridge.emit("chat:pong", { ok: true });
const result = await bridge.request("math:add", { a: 1, b: 2 });

off();
unhandle();
```

## Readiness And Navigation

- Java outbound messages queue until JS sends `ready`.
- `onReady` fires after queue flush.
- On navigation or page load start, readiness resets and pending Java requests fail.
- On close/detach, handlers, queue state, and pending requests are cleared.

## Error Codes

Common codes returned by bridge request failures:

- `handler_not_found`
- `java_handler_error`
- `js_handler_error`
- `invalid_request`
- `invalid_response`
- `bridge_error`

Java request failures complete with `GrapheneBridgeRequestException` (`getCode`, `getRequestId`, `getChannel`).
On JS, rejected errors include `error.code` when available.

## Channel And Payload Rules

- Channel must be non-null and non-blank.
- Java event/request payloads are JSON strings.
- JS payloads are normal JS values serialized by the bridge.
- Invalid Java JSON payloads throw `IllegalArgumentException`.

## Side Mouse Bridge

Graphene also injects `globalThis.grapheneMouse` for side mouse buttons (`3` to `7`, GLFW buttons 4 to 8).

- Event channel: `graphene:mouse:button`
- State request channel: `graphene:mouse:state`
- Event payload: `{ button: number, pressed: boolean, released: boolean }`

```js
const mouse = globalThis.grapheneMouse;

const unsubscribe = mouse.on((event) => {
  if (event.button === mouse.BUTTON_4 && event.pressed) {
    console.log("Side button 4 pressed");
  }
});

const isButton8Down = mouse.isPressed(mouse.BUTTON_8);
const snapshot = mouse.snapshot();

unsubscribe();
```

`snapshot()` returns `eventCount`, `lastEvent`, and `pressedButtons`.

## Cleanup

Unsubscribe when listeners are no longer needed:

```java
readySub.unsubscribe();
eventSub.unsubscribe();
requestSub.unsubscribe();
```

`GrapheneBridgeSubscription` is `AutoCloseable`, so try-with-resources works.

---

Next: [Assets And URLs](assets-and-urls.md)
