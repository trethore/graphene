const GRAPHENE_BRIDGE_NAME = "graphene-ui";
const GRAPHENE_PROTOCOL_VERSION = 1;
const GRAPHENE_KIND_EVENT = "event";
const GRAPHENE_KIND_REQUEST = "request";
const GRAPHENE_KIND_RESPONSE = "response";
const GRAPHENE_KIND_READY = "ready";
const GRAPHENE_ERROR_NO_HANDLER = "handler_not_found";
const GRAPHENE_ERROR_HANDLER_FAILURE = "js_handler_error";
const GRAPHENE_ERROR_INVALID_RESPONSE = "invalid_response";
const GRAPHENE_INSTALLED_FLAG = "__grapheneInstalled";
const GRAPHENE_RECEIVE_FN_NAME = "__grapheneBridgeReceiveFromJava";
const GRAPHENE_POPUP_FALLBACK_INSTALLED_FLAG = "__graphenePopupFallbackInstalled";
const GRAPHENE_SHARED_STATE_NAME = "__grapheneBridgeSharedState";
const GRAPHENE_READY_RETRY_DELAY_MS = 50;

/**
 * @typedef {Object} GrapheneCefQueryRequest
 * @property {string} request
 * @property {(responseText: string) => void} onSuccess
 * @property {(errorCode: number, errorMessage: string) => void} onFailure
 */

let grapheneBridgeNextRequestSequence = 0;
const grapheneBridgeEventListenersByChannel = new Map();
const grapheneBridgeRequestHandlersByChannel = new Map();

const grapheneBridgeSharedState = grapheneBridgeGetSharedState();

function grapheneBridgeNoop() {
}

function grapheneBridgeReportSuppressedError(context, error) {
    const consoleObject = globalThis.console;
    if (consoleObject && typeof consoleObject.debug === "function") {
        consoleObject.debug("[GrapheneBridge] " + context, error);
    }
}

function grapheneBridgeGetSharedState() {
    let sharedState = globalThis[GRAPHENE_SHARED_STATE_NAME];
    if (grapheneBridgeIsValidSharedState(sharedState)) {
        return sharedState;
    }

    /** @type {(value: void | PromiseLike<void>) => void} */
    let resolveReadyPromise = grapheneBridgeNoop;
    const readyPromise = new Promise(function (resolve) {
        resolveReadyPromise = resolve;
    });

    sharedState = {
        ready: false,
        readyRequestInFlight: false,
        readyRetryScheduled: false,
        readyListeners: new Set(),
        readyPromise: readyPromise,
        resolveReadyPromise: resolveReadyPromise
    };
    globalThis[GRAPHENE_SHARED_STATE_NAME] = sharedState;
    return sharedState;
}

function grapheneBridgeIsValidSharedState(sharedState) {
    if (!sharedState || typeof sharedState !== "object") {
        return false;
    }

    return grapheneBridgeHasBoolean(sharedState, "ready")
        && grapheneBridgeHasBoolean(sharedState, "readyRequestInFlight")
        && grapheneBridgeHasBoolean(sharedState, "readyRetryScheduled")
        && grapheneBridgeHasListenerSet(sharedState.readyListeners)
        && grapheneBridgeIsPromiseLike(sharedState.readyPromise)
        && typeof sharedState.resolveReadyPromise === "function";
}

function grapheneBridgeHasBoolean(objectValue, key) {
    return Object.hasOwn(objectValue, key) && typeof objectValue[key] === "boolean";
}

function grapheneBridgeHasListenerSet(value) {
    return value
        && typeof value === "object"
        && typeof value.add === "function"
        && typeof value.delete === "function"
        && typeof value.forEach === "function"
        && typeof value.clear === "function";
}

function grapheneBridgeIsPromiseLike(value) {
    return value && typeof value === "object" && typeof value.then === "function";
}

function grapheneBridgeResolveCefQuery() {
    const cefQuery = globalThis["cefQuery"];
    return typeof cefQuery === "function" ? cefQuery : null;
}

function grapheneBridgeNotifyReadyListeners() {
    grapheneBridgeSharedState.readyListeners.forEach(function (listener) {
        try {
            listener();
        } catch (error) {
            grapheneBridgeReportSuppressedError("Ready listener failed", error);
        }
    });
    grapheneBridgeSharedState.readyListeners.clear();
}

function grapheneBridgeMarkReady() {
    if (grapheneBridgeSharedState.ready) {
        return;
    }

    grapheneBridgeSharedState.ready = true;
    grapheneBridgeSharedState.readyRetryScheduled = false;
    grapheneBridgeSharedState.resolveReadyPromise();
    grapheneBridgeNotifyReadyListeners();
}

function grapheneBridgeOnReady(listener) {
    if (typeof listener !== "function") {
        throw new TypeError("listener must be a function");
    }

    if (grapheneBridgeSharedState.ready) {
        if (typeof globalThis.queueMicrotask === "function") {
            globalThis.queueMicrotask(listener);
        } else if (typeof globalThis.setTimeout === "function") {
            globalThis.setTimeout(listener, 0);
        } else {
            listener();
        }

        return grapheneBridgeNoop;
    }

    grapheneBridgeSharedState.readyListeners.add(listener);
    return function () {
        grapheneBridgeSharedState.readyListeners.delete(listener);
    };
}

function grapheneBridgeScheduleReadyRetry() {
    if (grapheneBridgeSharedState.ready || grapheneBridgeSharedState.readyRetryScheduled) {
        return;
    }

    if (typeof globalThis.setTimeout !== "function") {
        return;
    }

    grapheneBridgeSharedState.readyRetryScheduled = true;
    globalThis.setTimeout(function () {
        grapheneBridgeSharedState.readyRetryScheduled = false;
        grapheneBridgeSendReady();
    }, GRAPHENE_READY_RETRY_DELAY_MS);
}

function grapheneBridgeIsMessage(message) {
    return Boolean(message) && message.bridge === GRAPHENE_BRIDGE_NAME;
}

function grapheneBridgeNormalizePayload(payload) {
    return payload === undefined ? null : payload;
}

function grapheneBridgeCreateBaseMessage(kind) {
    return {
        bridge: GRAPHENE_BRIDGE_NAME,
        version: GRAPHENE_PROTOCOL_VERSION,
        kind: kind
    };
}

function grapheneBridgeCreateResponseBase(message) {
    return {
        bridge: GRAPHENE_BRIDGE_NAME,
        version: GRAPHENE_PROTOCOL_VERSION,
        kind: GRAPHENE_KIND_RESPONSE,
        id: message.id,
        channel: message.channel
    };
}

function grapheneBridgeCreateErrorResponse(message, code, errorMessage) {
    return Object.assign(grapheneBridgeCreateResponseBase(message), {
        ok: false,
        payload: null,
        error: {
            code: code,
            message: errorMessage
        }
    });
}

function grapheneBridgeCreateSuccessResponse(message, payload) {
    return Object.assign(grapheneBridgeCreateResponseBase(message), {
        ok: true,
        payload: grapheneBridgeNormalizePayload(payload)
    });
}

function grapheneBridgeNextRequestId() {
    grapheneBridgeNextRequestSequence += 1;
    return "js-" + Date.now() + "-" + grapheneBridgeNextRequestSequence;
}

function grapheneBridgeParseJsonOrNull(value) {
    try {
        return typeof value === "string" ? JSON.parse(value) : value;
    } catch (error) {
        grapheneBridgeReportSuppressedError("Failed to parse JSON payload", error);
        return null;
    }
}

function grapheneBridgeParseResponse(responseText) {
    if (!responseText) {
        return {ok: true, payload: null};
    }

    const parsed = grapheneBridgeParseJsonOrNull(responseText);
    if (parsed !== null) {
        return parsed;
    }

    return {
        ok: false,
        error: {
            code: GRAPHENE_ERROR_INVALID_RESPONSE,
            message: "Bridge returned invalid JSON"
        }
    };
}

function grapheneBridgeToError(response) {
    const defaultMessage = "Bridge request failed";
    const errorMessage = response?.error?.message ?? defaultMessage;
    const error = new Error(errorMessage);
    if (response?.error?.code) {
        error.code = response.error.code;
    }

    return error;
}

/**
 * @param {Object} message
 * @param {(responseText: string) => void} resolve
 * @param {(error: Error) => void} reject
 * @returns {GrapheneCefQueryRequest}
 */
function grapheneBridgeCreateCefQueryRequest(message, resolve, reject) {
    return {
        request: JSON.stringify(message),
        onSuccess: resolve,
        onFailure: function (_, errorMessage) {
            reject(new Error(errorMessage));
        }
    };
}

function grapheneBridgeSendToJava(message) {
    return new Promise(function (resolve, reject) {
        const cefQuery = grapheneBridgeResolveCefQuery();
        if (!cefQuery) {
            reject(new Error("cefQuery is unavailable"));
            return;
        }

        cefQuery(grapheneBridgeCreateCefQueryRequest(message, resolve, reject));
    });
}

function grapheneBridgeSendReady() {
    if (grapheneBridgeSharedState.ready || grapheneBridgeSharedState.readyRequestInFlight) {
        return;
    }

    grapheneBridgeSharedState.readyRequestInFlight = true;
    grapheneBridgeSendToJava(grapheneBridgeCreateBaseMessage(GRAPHENE_KIND_READY))
        .then(function () {
            grapheneBridgeSharedState.readyRequestInFlight = false;
            grapheneBridgeMarkReady();
        })
        .catch(function (error) {
            grapheneBridgeSharedState.readyRequestInFlight = false;
            grapheneBridgeReportSuppressedError("Ready handshake failed", error);
            grapheneBridgeScheduleReadyRetry();
        });
}

function grapheneBridgeAddEventListener(channel, listener) {
    let listeners = grapheneBridgeEventListenersByChannel.get(channel);
    if (!listeners) {
        listeners = new Set();
        grapheneBridgeEventListenersByChannel.set(channel, listeners);
    }

    listeners.add(listener);
}

function grapheneBridgeRemoveEventListener(channel, listener) {
    const listeners = grapheneBridgeEventListenersByChannel.get(channel);
    if (!listeners) {
        return;
    }

    listeners.delete(listener);
    if (listeners.size === 0) {
        grapheneBridgeEventListenersByChannel.delete(channel);
    }
}

function grapheneBridgeDispatchEvent(message) {
    const listeners = grapheneBridgeEventListenersByChannel.get(message.channel);
    if (!listeners) {
        return;
    }

    listeners.forEach(function (listener) {
        try {
            listener(message.payload);
        } catch (error) {
            grapheneBridgeReportSuppressedError("Event listener failed for channel '" + message.channel + "'", error);
        }
    });
}

function grapheneBridgeHandleRequest(message) {
    const requestHandler = grapheneBridgeRequestHandlersByChannel.get(message.channel);
    if (!requestHandler) {
        grapheneBridgeSendToJava(grapheneBridgeCreateErrorResponse(
            message,
            GRAPHENE_ERROR_NO_HANDLER,
            "No JS bridge handler for channel '" + message.channel + "'"
        )).catch(grapheneBridgeNoop);
        return;
    }

    Promise.resolve()
        .then(function () {
            return requestHandler(message.payload);
        })
        .then(function (responsePayload) {
            return grapheneBridgeSendToJava(grapheneBridgeCreateSuccessResponse(message, responsePayload));
        })
        .catch(function (error) {
            const messageText = error?.message ?? String(error);
            return grapheneBridgeSendToJava(grapheneBridgeCreateErrorResponse(message, GRAPHENE_ERROR_HANDLER_FAILURE, messageText));
        })
        .catch(grapheneBridgeNoop);
}

function grapheneBridgeReceiveFromJava(messageJson) {
    const message = grapheneBridgeParseJsonOrNull(messageJson);
    if (!grapheneBridgeIsMessage(message)) {
        return;
    }

    if (message.kind === GRAPHENE_KIND_EVENT) {
        grapheneBridgeDispatchEvent(message);
        return;
    }

    if (message.kind === GRAPHENE_KIND_REQUEST) {
        grapheneBridgeHandleRequest(message);
    }
}

function grapheneBridgeInstallApi() {
    globalThis[GRAPHENE_RECEIVE_FN_NAME] = grapheneBridgeReceiveFromJava;

    const grapheneBridgeApi = {
        __grapheneInstalled: true,
        on: function (channel, listener) {
            grapheneBridgeAddEventListener(channel, listener);
            return function () {
                grapheneBridgeApi.off(channel, listener);
            };
        },
        off: grapheneBridgeOff,
        handle: function (channel, handler) {
            grapheneBridgeRequestHandlersByChannel.set(channel, handler);
            return function () {
                if (grapheneBridgeRequestHandlersByChannel.get(channel) === handler) {
                    grapheneBridgeRequestHandlersByChannel.delete(channel);
                }
            };
        },
        isReady: function () {
            return grapheneBridgeSharedState.ready;
        },
        onReady: grapheneBridgeOnReady,
        ready: function () {
            return grapheneBridgeSharedState.readyPromise;
        },
        emit: function (channel, payload) {
            return grapheneBridgeSendToJava(Object.assign(grapheneBridgeCreateBaseMessage(GRAPHENE_KIND_EVENT), {
                channel: channel,
                payload: grapheneBridgeNormalizePayload(payload)
            }));
        },
        request: function (channel, payload) {
            return grapheneBridgeSendToJava(Object.assign(grapheneBridgeCreateBaseMessage(GRAPHENE_KIND_REQUEST), {
                id: grapheneBridgeNextRequestId(),
                channel: channel,
                payload: grapheneBridgeNormalizePayload(payload)
            })).then(function (responseText) {
                const response = grapheneBridgeParseResponse(responseText);
                if (response.ok === false) {
                    throw grapheneBridgeToError(response);
                }

                return response.payload;
            });
        }
    };

    globalThis.grapheneBridge = grapheneBridgeApi;
}

function grapheneBridgeOff(channel, listener) {
    grapheneBridgeRemoveEventListener(channel, listener);
}

function grapheneBridgeFindAnchorFromClick(event) {
    const target = event?.target;
    if (target && typeof target.closest === "function") {
        const directMatch = target.closest("a[href]");
        if (directMatch) {
            return directMatch;
        }
    }

    if (typeof event?.composedPath !== "function") {
        return null;
    }

    const path = event.composedPath();
    for (const node of path) {
        if (!node) {
            continue;
        }

        if (typeof node.closest === "function") {
            const pathMatch = node.closest("a[href]");
            if (pathMatch) {
                return pathMatch;
            }
        }
    }

    return null;
}

function grapheneBridgeInstallPopupFallback() {
    if (globalThis[GRAPHENE_POPUP_FALLBACK_INSTALLED_FLAG]) {
        return;
    }

    globalThis[GRAPHENE_POPUP_FALLBACK_INSTALLED_FLAG] = true;

    document.addEventListener("click", function (event) {
        const anchor = grapheneBridgeFindAnchorFromClick(event);
        if (!anchor) {
            return;
        }

        const target = String(anchor.getAttribute("target") || "").toLowerCase();
        if (target !== "_blank") {
            return;
        }

        const href = anchor.href;
        if (!href) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();
        globalThis.location?.assign?.(href);
    }, true);

    const originalOpen = typeof globalThis.open === "function"
        ? globalThis.open.bind(globalThis)
        : null;

    globalThis.open = function (url, target, features) {
        const hasUrl = typeof url === "string" && url.length > 0;
        const normalizedTarget = typeof target === "string" ? target.toLowerCase() : "";
        if (hasUrl && (normalizedTarget === "_blank" || normalizedTarget === "")) {
            globalThis.location?.assign?.(url);
            return globalThis;
        }

        if (originalOpen) {
            return originalOpen(url, target, features);
        }

        return null;
    };
}

if (!globalThis.grapheneBridge?.[GRAPHENE_INSTALLED_FLAG]) {
    grapheneBridgeInstallApi();
}

grapheneBridgeInstallPopupFallback();
grapheneBridgeSendReady();
