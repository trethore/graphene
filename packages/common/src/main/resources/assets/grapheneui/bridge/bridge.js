(function () {
	"use strict";

	const BRIDGE_NAME = "grapheneui";
	const PROTOCOL_VERSION = 1;
	const KIND_EVENT = "event";
	const KIND_REQUEST = "request";
	const KIND_RESPONSE = "response";
	const KIND_READY = "ready";
	const ERROR_NO_HANDLER = "handler_not_found";
	const ERROR_HANDLER_FAILURE = "js_handler_error";
	const ERROR_INVALID_RESPONSE = "invalid_response";
	const INSTALLED_FLAG = "__grapheneInstalled";
	const RECEIVE_FN_NAME = "__grapheneBridgeReceiveFromJava";
	const READY_RETRY_DELAY_MS = 50;

	if (globalThis.grapheneBridge?.[INSTALLED_FLAG]) {
		return;
	}

	/**
	 * @typedef {Object} GrapheneCefQueryRequest
	 * @property {string} request
	 * @property {(responseText: string) => void} onSuccess
	 * @property {(errorCode: number, errorMessage: string) => void} onFailure
	 */

	let nextRequestSequence = 0;
	const eventListenersByChannel = new Map();
	const requestHandlersByChannel = new Map();
	const readyState = createReadyState();

	function noop() {}

	function reportSuppressedError(context, error) {
		const consoleObject = globalThis.console;
		if (consoleObject && typeof consoleObject.debug === "function") {
			consoleObject.debug("[GrapheneBridge] " + context, error);
		}
	}

	function createReadyState() {
		/** @type {(value: void | PromiseLike<void>) => void} */
		let resolveReadyPromise = noop;
		const readyPromise = new Promise(function (resolve) {
			resolveReadyPromise = resolve;
		});

		return {
			ready: false,
			readyRequestInFlight: false,
			readyRetryScheduled: false,
			readyListeners: new Set(),
			readyPromise: readyPromise,
			resolveReadyPromise: resolveReadyPromise,
		};
	}

	function isBridgeMessage(message) {
		return Boolean(message) && message.bridge === BRIDGE_NAME;
	}

	function normalizePayload(payload) {
		return payload === undefined ? null : payload;
	}

	function createBaseMessage(kind) {
		return {
			bridge: BRIDGE_NAME,
			version: PROTOCOL_VERSION,
			kind: kind,
		};
	}

	function createResponseBase(message) {
		return {
			bridge: BRIDGE_NAME,
			version: PROTOCOL_VERSION,
			kind: KIND_RESPONSE,
			id: message.id,
			channel: message.channel,
		};
	}

	function createErrorResponse(message, code, errorMessage) {
		return Object.assign(createResponseBase(message), {
			ok: false,
			payload: null,
			error: {
				code: code,
				message: errorMessage,
			},
		});
	}

	function createSuccessResponse(message, payload) {
		return Object.assign(createResponseBase(message), {
			ok: true,
			payload: normalizePayload(payload),
		});
	}

	function nextRequestId() {
		nextRequestSequence += 1;
		return "js-" + Date.now() + "-" + nextRequestSequence;
	}

	function parseJsonOrNull(value) {
		try {
			return typeof value === "string" ? JSON.parse(value) : value;
		} catch (error) {
			reportSuppressedError("Failed to parse JSON payload", error);
			return null;
		}
	}

	function parseResponse(responseText) {
		if (!responseText) {
			return { ok: true, payload: null };
		}

		const parsed = parseJsonOrNull(responseText);
		if (parsed !== null) {
			return parsed;
		}

		return {
			ok: false,
			error: {
				code: ERROR_INVALID_RESPONSE,
				message: "Bridge returned invalid JSON",
			},
		};
	}

	function toError(response) {
		const defaultMessage = "Bridge request failed";
		const errorMessage = response?.error?.message ?? defaultMessage;
		const error = new Error(errorMessage);
		if (response?.error?.code) {
			error.code = response.error.code;
		}

		return error;
	}

	function resolveCefQuery() {
		const cefQuery = globalThis["cefQuery"];
		return typeof cefQuery === "function" ? cefQuery : null;
	}

	/**
	 * @param {Object} message
	 * @param {(responseText: string) => void} resolve
	 * @param {(error: Error) => void} reject
	 * @returns {GrapheneCefQueryRequest}
	 */
	function createCefQueryRequest(message, resolve, reject) {
		return {
			request: JSON.stringify(message),
			onSuccess: resolve,
			onFailure: function (_, errorMessage) {
				reject(new Error(errorMessage));
			},
		};
	}

	function sendToJava(message) {
		return new Promise(function (resolve, reject) {
			const cefQuery = resolveCefQuery();
			if (!cefQuery) {
				reject(new Error("cefQuery is unavailable"));
				return;
			}

			cefQuery(createCefQueryRequest(message, resolve, reject));
		});
	}

	function notifyReadyListeners() {
		readyState.readyListeners.forEach(function (listener) {
			try {
				listener();
			} catch (error) {
				reportSuppressedError("Ready listener failed", error);
			}
		});
		readyState.readyListeners.clear();
	}

	function markReady() {
		if (readyState.ready) {
			return;
		}

		readyState.ready = true;
		readyState.readyRetryScheduled = false;
		readyState.resolveReadyPromise();
		notifyReadyListeners();
	}

	function onReady(listener) {
		if (typeof listener !== "function") {
			throw new TypeError("listener must be a function");
		}

		if (readyState.ready) {
			if (typeof globalThis.queueMicrotask === "function") {
				globalThis.queueMicrotask(listener);
			} else if (typeof globalThis.setTimeout === "function") {
				globalThis.setTimeout(listener, 0);
			} else {
				listener();
			}

			return noop;
		}

		readyState.readyListeners.add(listener);
		return function () {
			readyState.readyListeners.delete(listener);
		};
	}

	function scheduleReadyRetry() {
		if (readyState.ready || readyState.readyRetryScheduled) {
			return;
		}

		if (typeof globalThis.setTimeout !== "function") {
			return;
		}

		readyState.readyRetryScheduled = true;
		globalThis.setTimeout(function () {
			readyState.readyRetryScheduled = false;
			sendReady();
		}, READY_RETRY_DELAY_MS);
	}

	function sendReady() {
		if (readyState.ready || readyState.readyRequestInFlight) {
			return;
		}

		readyState.readyRequestInFlight = true;
		sendToJava(createBaseMessage(KIND_READY))
			.then(function () {
				readyState.readyRequestInFlight = false;
				markReady();
			})
			.catch(function (error) {
				readyState.readyRequestInFlight = false;
				reportSuppressedError("Ready handshake failed", error);
				scheduleReadyRetry();
			});
	}

	function addChannelListener(channel, listener) {
		let listeners = eventListenersByChannel.get(channel);
		if (!listeners) {
			listeners = new Set();
			eventListenersByChannel.set(channel, listeners);
		}

		listeners.add(listener);
	}

	function removeChannelListener(channel, listener) {
		const listeners = eventListenersByChannel.get(channel);
		if (!listeners) {
			return;
		}

		listeners.delete(listener);
		if (listeners.size === 0) {
			eventListenersByChannel.delete(channel);
		}
	}

	function dispatchChannelEvent(message) {
		const listeners = eventListenersByChannel.get(message.channel);
		if (!listeners) {
			return;
		}

		listeners.forEach(function (listener) {
			try {
				listener(message.payload);
			} catch (error) {
				reportSuppressedError(
					"Event listener failed for channel '" + message.channel + "'",
					error,
				);
			}
		});
	}

	function handleRequest(message) {
		const requestHandler = requestHandlersByChannel.get(message.channel);
		if (!requestHandler) {
			sendToJava(
				createErrorResponse(
					message,
					ERROR_NO_HANDLER,
					"No JS bridge handler for channel '" + message.channel + "'",
				),
			).catch(noop);
			return;
		}

		Promise.resolve()
			.then(function () {
				return requestHandler(message.payload);
			})
			.then(function (responsePayload) {
				return sendToJava(createSuccessResponse(message, responsePayload));
			})
			.catch(function (error) {
				const messageText = error?.message ?? String(error);
				return sendToJava(
					createErrorResponse(message, ERROR_HANDLER_FAILURE, messageText),
				);
			})
			.catch(noop);
	}

	function receiveFromJava(messageJson) {
		const message = parseJsonOrNull(messageJson);
		if (!isBridgeMessage(message)) {
			return;
		}

		if (message.kind === KIND_EVENT) {
			dispatchChannelEvent(message);
			return;
		}

		if (message.kind === KIND_REQUEST) {
			handleRequest(message);
		}
	}

	function install() {
		globalThis[RECEIVE_FN_NAME] = receiveFromJava;

		globalThis.grapheneBridge = {
			[INSTALLED_FLAG]: true,
			on: function (channel, listener) {
				addChannelListener(channel, listener);
				return function () {
					removeChannelListener(channel, listener);
				};
			},
			off: removeChannelListener,
			handle: function (channel, handler) {
				requestHandlersByChannel.set(channel, handler);
				return function () {
					if (requestHandlersByChannel.get(channel) === handler) {
						requestHandlersByChannel.delete(channel);
					}
				};
			},
			isReady: function () {
				return readyState.ready;
			},
			onReady: onReady,
			ready: function () {
				return readyState.readyPromise;
			},
			emit: function (channel, payload) {
				return sendToJava(
					Object.assign(createBaseMessage(KIND_EVENT), {
						channel: channel,
						payload: normalizePayload(payload),
					}),
				);
			},
			request: function (channel, payload) {
				return sendToJava(
					Object.assign(createBaseMessage(KIND_REQUEST), {
						id: nextRequestId(),
						channel: channel,
						payload: normalizePayload(payload),
					}),
				).then(function (responseText) {
					const response = parseResponse(responseText);
					if (response.ok === false) {
						throw toError(response);
					}

					return response.payload;
				});
			},
		};
	}

	install();
	sendReady();
})();
