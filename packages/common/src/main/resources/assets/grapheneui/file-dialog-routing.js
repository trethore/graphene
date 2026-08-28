(function () {
	"use strict";

	const WRAPPED_FLAG = "__grapheneFileDialogRoutingWrapped";
	const ARM_CHANNEL = "graphene:file-dialog:arm-directory";
	const BRIDGE_NAME = "grapheneui";
	const PROTOCOL_VERSION = 1;
	const nativeShowDirectoryPicker = globalThis.showDirectoryPicker;
	if (
		typeof nativeShowDirectoryPicker !== "function" ||
		nativeShowDirectoryPicker[WRAPPED_FLAG]
	) {
		return;
	}

	const apply = Reflect.apply;
	let nextRequestSequence = 0;

	function notAllowed(message) {
		return new DOMException(message, "NotAllowedError");
	}

	function armDirectoryPicker() {
		const cefQuery = globalThis.cefQuery;
		if (typeof cefQuery !== "function") {
			return Promise.reject(
				notAllowed("Graphene directory-picker routing is unavailable"),
			);
		}

		nextRequestSequence += 1;
		const request = JSON.stringify({
			bridge: BRIDGE_NAME,
			version: PROTOCOL_VERSION,
			kind: "request",
			id: "directory-picker-" + Date.now() + "-" + nextRequestSequence,
			channel: ARM_CHANNEL,
			payload: null,
		});
		return new Promise(function (resolve, reject) {
			cefQuery({
				request: request,
				onSuccess: function (responseText) {
					try {
						const response = JSON.parse(responseText);
						if (response?.ok === true) {
							resolve();
							return;
						}
						reject(
							notAllowed(
								response?.error?.message ??
									"Graphene rejected directory-picker routing",
							),
						);
					} catch (error) {
						const reason = error instanceof Error ? ": " + error.message : "";
						reject(
							notAllowed(
								"Graphene returned an invalid routing response" + reason,
							),
						);
					}
				},
				onFailure: function (_errorCode, errorMessage) {
					reject(
						notAllowed(
							errorMessage || "Graphene directory-picker routing failed",
						),
					);
				},
			});
		});
	}

	function showDirectoryPicker(options) {
		const args = arguments.length === 0 ? [] : [options];
		const invoke = () => apply(nativeShowDirectoryPicker, this, args);
		return armDirectoryPicker().then(invoke);
	}

	Object.defineProperty(showDirectoryPicker, WRAPPED_FLAG, {
		value: true,
	});
	const descriptor = Object.getOwnPropertyDescriptor(
		globalThis,
		"showDirectoryPicker",
	);
	Object.defineProperty(globalThis, "showDirectoryPicker", {
		configurable: descriptor?.configurable ?? true,
		enumerable: descriptor?.enumerable ?? true,
		writable: descriptor?.writable ?? true,
		value: showDirectoryPicker,
	});
})();
