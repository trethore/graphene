(function () {
	"use strict";

	const WRAPPED_FLAG = "__grapheneFileDialogRoutingWrapped";
	const ARM_CHANNEL = "graphene.internal.file-dialog.arm-directory";
	const nativeShowDirectoryPicker = globalThis.showDirectoryPicker;
	if (
		typeof nativeShowDirectoryPicker !== "function" ||
		nativeShowDirectoryPicker[WRAPPED_FLAG]
	) {
		return;
	}

	const apply = Reflect.apply;

	function showDirectoryPicker(options) {
		const args = arguments.length === 0 ? [] : [options];
		const invoke = () => apply(nativeShowDirectoryPicker, this, args);
		const bridge = globalThis.grapheneBridge;
		if (!bridge || typeof bridge.request !== "function") {
			return invoke();
		}

		return bridge.request(ARM_CHANNEL, null).then(invoke, invoke);
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
