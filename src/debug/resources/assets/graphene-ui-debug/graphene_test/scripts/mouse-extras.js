(function () {
    function statusElement() {
        return document.getElementById("status");
    }

    function detailsElement() {
        return document.getElementById("details");
    }

    function renderSnapshot(mouseExtrasApi) {
        const snapshot = mouseExtrasApi.snapshot();
        const lastEvent = snapshot.lastEvent
            ? ("button=" + snapshot.lastEvent.button + ", pressed=" + snapshot.lastEvent.pressed)
            : "none";

        detailsElement().textContent =
            "Event count: " + snapshot.eventCount + "\n" +
            "Pressed buttons: [" + snapshot.pressedButtons.join(", ") + "]\n" +
            "Last event: " + lastEvent;
    }

    function renderMouseEvent(mouseExtrasApi, eventPayload) {
        const action = eventPayload.pressed ? "pressed" : "released";
        statusElement().textContent = "Bridge connected: button " + eventPayload.button + " " + action;
        renderSnapshot(mouseExtrasApi);
    }

    function connectMouseExtrasWhenAvailable() {
        const mouseExtrasApi = globalThis.grapheneMouseExtras;
        if (!mouseExtrasApi || typeof mouseExtrasApi.on !== "function" || typeof mouseExtrasApi.snapshot !== "function") {
            setTimeout(connectMouseExtrasWhenAvailable, 50);
            return;
        }

        statusElement().textContent = "Bridge connected: press an extra mouse button";
        renderSnapshot(mouseExtrasApi);
        mouseExtrasApi.on(function (eventPayload) {
            renderMouseEvent(mouseExtrasApi, eventPayload);
        });
    }

    connectMouseExtrasWhenAvailable();
})();
