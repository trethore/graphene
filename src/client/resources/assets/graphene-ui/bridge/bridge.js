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

let grapheneBridgeNextRequestSequence = 0;
const grapheneBridgeEventListenersByChannel = new Map();
const grapheneBridgeRequestHandlersByChannel = new Map();

function grapheneBridgeNoop() {
}

function grapheneBridgeReportSuppressedError(context, error) {
    if (typeof globalThis.console?.debug === "function") {
        globalThis.console.debug("[GrapheneBridge] " + context, error);
    }
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

function grapheneBridgeSendToJava(message) {
    return new Promise(function (resolve, reject) {
        if (typeof globalThis.cefQuery !== "function") {
            reject(new Error("cefQuery is unavailable"));
            return;
        }

        globalThis.cefQuery({
            request: JSON.stringify(message),
            onSuccess: resolve,
            onFailure: function (_, errorMessage) {
                reject(new Error(errorMessage));
            }
        });
    });
}

function grapheneBridgeSendReady() {
    grapheneBridgeSendToJava(grapheneBridgeCreateBaseMessage(GRAPHENE_KIND_READY)).catch(grapheneBridgeNoop);
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

    globalThis.grapheneBridge = {
        __grapheneInstalled: true,
        on: function (channel, listener) {
            grapheneBridgeAddEventListener(channel, listener);
            return function () {
                grapheneBridgeRemoveEventListener(channel, listener);
            };
        },
        off: function (channel, listener) {
            grapheneBridgeRemoveEventListener(channel, listener);
        },
        handle: function (channel, handler) {
            grapheneBridgeRequestHandlersByChannel.set(channel, handler);
            return function () {
                if (grapheneBridgeRequestHandlersByChannel.get(channel) === handler) {
                    grapheneBridgeRequestHandlersByChannel.delete(channel);
                }
            };
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
}

if (!globalThis.grapheneBridge?.[GRAPHENE_INSTALLED_FLAG]) {
    grapheneBridgeInstallApi();
}

grapheneBridgeSendReady();
