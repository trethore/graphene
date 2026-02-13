const GRAPHENE_MOUSE_INSTALLED_FLAG = "__grapheneMouseInstalled";
const GRAPHENE_MOUSE_CHANNEL = "graphene:mouse:button";
const GRAPHENE_MOUSE_STATE_REQUEST_CHANNEL = "graphene:mouse:state";
const GRAPHENE_MOUSE_BUTTON_4 = 3;
const GRAPHENE_MOUSE_BUTTON_5 = 4;
const GRAPHENE_MOUSE_BUTTON_6 = 5;
const GRAPHENE_MOUSE_BUTTON_7 = 6;
const GRAPHENE_MOUSE_BUTTON_8 = 7;

const grapheneMouseListeners = new Set();
const grapheneMousePressedButtons = new Set();
let grapheneMouseEventCount = 0;
let grapheneMouseLastEvent = null;

function grapheneMouseReportSuppressedError(context, error) {
    const consoleObject = globalThis.console;
    if (consoleObject && typeof consoleObject.debug === "function") {
        consoleObject.debug("[GrapheneMouse] " + context, error);
    }
}

function grapheneMouseIsSupportedButton(button) {
    return button >= GRAPHENE_MOUSE_BUTTON_4 && button <= GRAPHENE_MOUSE_BUTTON_8;
}

function grapheneMouseCreateEvent(button, pressed) {
    return {
        button: button,
        pressed: pressed,
        released: !pressed
    };
}

function grapheneMouseSnapshot() {
    return {
        eventCount: grapheneMouseEventCount,
        lastEvent: grapheneMouseLastEvent,
        pressedButtons: Array.from(grapheneMousePressedButtons).sort(function (left, right) {
            return left - right;
        })
    };
}

function grapheneMouseDispatch(eventPayload) {
    grapheneMouseListeners.forEach(function (listener) {
        try {
            listener(eventPayload);
        } catch (error) {
            grapheneMouseReportSuppressedError("Mouse listener failed", error);
        }
    });
}

function grapheneMouseOnBridgeEvent(payload) {
    const button = Number(payload?.button);
    if (!grapheneMouseIsSupportedButton(button)) {
        return;
    }

    const pressed = Boolean(payload?.pressed);
    if (pressed) {
        grapheneMousePressedButtons.add(button);
    } else {
        grapheneMousePressedButtons.delete(button);
    }

    grapheneMouseEventCount += 1;
    grapheneMouseLastEvent = grapheneMouseCreateEvent(button, pressed);
    grapheneMouseDispatch(grapheneMouseLastEvent);
}

function grapheneMouseAddListener(listener) {
    grapheneMouseListeners.add(listener);
}

function grapheneMouseRemoveListener(listener) {
    grapheneMouseListeners.delete(listener);
}

function grapheneMouseInstallApi(bridge) {
    bridge.on(GRAPHENE_MOUSE_CHANNEL, grapheneMouseOnBridgeEvent);
    bridge.handle(GRAPHENE_MOUSE_STATE_REQUEST_CHANNEL, function () {
        return grapheneMouseSnapshot();
    });

    globalThis.grapheneMouse = {
        __grapheneMouseInstalled: true,
        CHANNEL: GRAPHENE_MOUSE_CHANNEL,
        BUTTON_4: GRAPHENE_MOUSE_BUTTON_4,
        BUTTON_5: GRAPHENE_MOUSE_BUTTON_5,
        BUTTON_6: GRAPHENE_MOUSE_BUTTON_6,
        BUTTON_7: GRAPHENE_MOUSE_BUTTON_7,
        BUTTON_8: GRAPHENE_MOUSE_BUTTON_8,
        on: function (listener) {
            grapheneMouseAddListener(listener);
            return function () {
                grapheneMouseRemoveListener(listener);
            };
        },
        off: function (listener) {
            grapheneMouseRemoveListener(listener);
        },
        isSideButton: function (button) {
            return grapheneMouseIsSupportedButton(Number(button));
        },
        isPressed: function (button) {
            const normalizedButton = Number(button);
            return grapheneMousePressedButtons.has(normalizedButton);
        },
        snapshot: function () {
            return grapheneMouseSnapshot();
        }
    };
}

function grapheneMouseTryInstall() {
    if (globalThis.grapheneMouse?.[GRAPHENE_MOUSE_INSTALLED_FLAG]) {
        return;
    }

    const bridge = globalThis.grapheneBridge;
    if (!bridge || typeof bridge.on !== "function" || typeof bridge.handle !== "function") {
        if (typeof globalThis.setTimeout === "function") {
            globalThis.setTimeout(grapheneMouseTryInstall, 50);
        }
        return;
    }

    grapheneMouseInstallApi(bridge);
}

grapheneMouseTryInstall();
