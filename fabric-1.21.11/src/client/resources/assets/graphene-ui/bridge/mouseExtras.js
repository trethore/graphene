(() => {
    const GRAPHENE_MOUSE_EXTRAS_INSTALLED_FLAG = "__grapheneMouseExtrasInstalled";
    const GRAPHENE_MOUSE_EXTRA_BUTTON_CHANNEL = "graphene:mouse:extra-button";
    const GRAPHENE_MOUSE_EXTRA_RESET_CHANNEL = "graphene:mouse:extra-reset";
    const GRAPHENE_MOUSE_EXTRA_STATE_REQUEST_CHANNEL = "graphene:mouse:extra-state";
    const GRAPHENE_MOUSE_EXTRA_BUTTON_5 = 5;
    const GRAPHENE_MOUSE_EXTRA_BUTTON_6 = 6;
    const GRAPHENE_MOUSE_EXTRA_BUTTON_7 = 7;

    const grapheneMouseExtrasListeners = new Set();
    const grapheneMouseExtrasPressedButtons = new Set();
    let grapheneMouseExtrasEventCount = 0;
    let grapheneMouseExtrasLastEvent = null;

    function grapheneMouseExtrasReportSuppressedError(context, error) {
    const consoleObject = globalThis.console;
    if (consoleObject && typeof consoleObject.debug === "function") {
        consoleObject.debug("[GrapheneMouseExtras] " + context, error);
    }
    }

    function grapheneMouseExtrasIsSupportedButton(button) {
    return button >= GRAPHENE_MOUSE_EXTRA_BUTTON_5 && button <= GRAPHENE_MOUSE_EXTRA_BUTTON_7;
    }

    function grapheneMouseExtrasCreateEvent(button, pressed) {
    return {
        button: button,
        pressed: pressed,
        released: !pressed
    };
    }

    function grapheneMouseExtrasSnapshot() {
    return {
        eventCount: grapheneMouseExtrasEventCount,
        lastEvent: grapheneMouseExtrasLastEvent,
        pressedButtons: Array.from(grapheneMouseExtrasPressedButtons).sort(function (left, right) {
            return left - right;
        })
    };
    }

    function grapheneMouseExtrasDispatch(eventPayload) {
    grapheneMouseExtrasListeners.forEach(function (listener) {
        try {
            listener(eventPayload);
        } catch (error) {
            grapheneMouseExtrasReportSuppressedError("Mouse extras listener failed", error);
        }
    });
    }

    function grapheneMouseExtrasResetState() {
    const pressedButtons = Array.from(grapheneMouseExtrasPressedButtons).sort(function (left, right) {
        return left - right;
    });
    grapheneMouseExtrasPressedButtons.clear();

    pressedButtons.forEach(function (button) {
        grapheneMouseExtrasEventCount += 1;
        grapheneMouseExtrasLastEvent = grapheneMouseExtrasCreateEvent(button, false);
        grapheneMouseExtrasDispatch(grapheneMouseExtrasLastEvent);
    });

    if (pressedButtons.length === 0) {
        grapheneMouseExtrasLastEvent = null;
    }
    }

    function grapheneMouseExtrasOnBridgeEvent(payload) {
    const button = Number(payload?.button);
    if (!grapheneMouseExtrasIsSupportedButton(button)) {
        return;
    }

    const pressed = Boolean(payload?.pressed);
    if (pressed) {
        grapheneMouseExtrasPressedButtons.add(button);
    } else {
        grapheneMouseExtrasPressedButtons.delete(button);
    }

    grapheneMouseExtrasEventCount += 1;
    grapheneMouseExtrasLastEvent = grapheneMouseExtrasCreateEvent(button, pressed);
    grapheneMouseExtrasDispatch(grapheneMouseExtrasLastEvent);
    }

    function grapheneMouseExtrasAddListener(listener) {
    grapheneMouseExtrasListeners.add(listener);
    }

    function grapheneMouseExtrasRemoveListener(listener) {
    grapheneMouseExtrasListeners.delete(listener);
    }

    function grapheneMouseExtrasInstallApi(bridge) {
    bridge.on(GRAPHENE_MOUSE_EXTRA_BUTTON_CHANNEL, grapheneMouseExtrasOnBridgeEvent);
    bridge.on(GRAPHENE_MOUSE_EXTRA_RESET_CHANNEL, function () {
        grapheneMouseExtrasResetState();
    });
    bridge.handle(GRAPHENE_MOUSE_EXTRA_STATE_REQUEST_CHANNEL, function () {
        return grapheneMouseExtrasSnapshot();
    });

    globalThis.grapheneMouseExtras = {
        __grapheneMouseExtrasInstalled: true,
        CHANNEL: GRAPHENE_MOUSE_EXTRA_BUTTON_CHANNEL,
        BUTTON_5: GRAPHENE_MOUSE_EXTRA_BUTTON_5,
        BUTTON_6: GRAPHENE_MOUSE_EXTRA_BUTTON_6,
        BUTTON_7: GRAPHENE_MOUSE_EXTRA_BUTTON_7,
        on: function (listener) {
            grapheneMouseExtrasAddListener(listener);
            return function () {
                grapheneMouseExtrasRemoveListener(listener);
            };
        },
        off: function (listener) {
            grapheneMouseExtrasRemoveListener(listener);
        },
        isExtraButton: function (button) {
            return grapheneMouseExtrasIsSupportedButton(Number(button));
        },
        isPressed: function (button) {
            const normalizedButton = Number(button);
            return grapheneMouseExtrasPressedButtons.has(normalizedButton);
        },
        snapshot: function () {
            return grapheneMouseExtrasSnapshot();
        }
    };
    }

    function grapheneMouseExtrasTryInstall() {
    if (globalThis.grapheneMouseExtras?.[GRAPHENE_MOUSE_EXTRAS_INSTALLED_FLAG]) {
        return;
    }

    const bridge = globalThis.grapheneBridge;
    if (!bridge || typeof bridge.on !== "function" || typeof bridge.handle !== "function") {
        if (typeof globalThis.setTimeout === "function") {
            globalThis.setTimeout(grapheneMouseExtrasTryInstall, 50);
        }
        return;
    }

    grapheneMouseExtrasInstallApi(bridge);
    }

    grapheneMouseExtrasTryInstall();
})();
