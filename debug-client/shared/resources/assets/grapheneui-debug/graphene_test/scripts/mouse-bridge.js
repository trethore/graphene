(function () {
    let removeListener = null;

    function statusElement() {
        return document.getElementById("status");
    }

    function detailsElement() {
        return document.getElementById("details");
    }

    function renderSnapshot(mouseApi) {
        const snapshot = mouseApi.snapshot();
        const lastEvent = snapshot.lastEvent
            ? ("button=" + snapshot.lastEvent.button + ", pressed=" + snapshot.lastEvent.pressed)
            : "none";

        detailsElement().textContent =
            "Event count: " + snapshot.eventCount + "\n" +
            "Pressed buttons: [" + snapshot.pressedButtons.join(", ") + "]\n" +
            "Last event: " + lastEvent;
    }

    function renderMouseEvent(mouseApi, eventPayload) {
        const action = eventPayload.pressed ? "pressed" : "released";
        statusElement().innerHTML =
            "<span class=\"ok\">Bridge connected:</span> button " + eventPayload.button + " " + action;
        renderSnapshot(mouseApi);
    }

    function connectMouseBridgeWhenAvailable() {
        const mouseApi = globalThis.grapheneMouse;
        if (!mouseApi || typeof mouseApi.on !== "function" || typeof mouseApi.snapshot !== "function") {
            setTimeout(connectMouseBridgeWhenAvailable, 50);
            return;
        }

        if (typeof removeListener === "function") {
            removeListener();
        }

        statusElement().innerHTML = "<span class=\"ok\">Bridge connected:</span> press a side mouse button";
        renderSnapshot(mouseApi);
        removeListener = mouseApi.on(function (eventPayload) {
            renderMouseEvent(mouseApi, eventPayload);
        });
    }

    connectMouseBridgeWhenAvailable();
})();
