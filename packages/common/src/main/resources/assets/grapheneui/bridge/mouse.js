(function () {
	"use strict";

	const INSTALLED_FLAG = "__grapheneMouseInstalled";
	const EXTRA_BUTTON_CHANNEL = "graphene:mouse:button";
	const STATE_REQUEST_CHANNEL = "graphene:mouse:state";
	const BUTTON_6 = 5;
	const BUTTON_7 = 6;
	const BUTTON_8 = 7;

	const listeners = new Set();
	const pressedButtons = new Set();
	let eventCount = 0;
	let lastEvent = null;

	function reportSuppressedError(context, error) {
		const consoleObject = globalThis.console;
		if (consoleObject && typeof consoleObject.debug === "function") {
			consoleObject.debug("[GrapheneMouse] " + context, error);
		}
	}

	function isSupportedButton(button) {
		return button >= BUTTON_6 && button <= BUTTON_8;
	}

	function createEvent(button, pressed) {
		return {
			button: button,
			pressed: pressed,
			released: !pressed,
		};
	}

	function buttonsMask() {
		let buttons = 0;
		pressedButtons.forEach(function (button) {
			buttons |= 1 << button;
		});
		return buttons;
	}

	function dispatchDomEvent(button, pressed, x, y) {
		const target = document.elementFromPoint(x, y) ?? document;
		const type = pressed ? "mousedown" : "mouseup";
		const options = {
			bubbles: true,
			cancelable: true,
			composed: true,
			view: globalThis,
			button: button,
			buttons: buttonsMask(),
			clientX: x,
			clientY: y,
		};

		if (typeof globalThis.PointerEvent === "function") {
			target.dispatchEvent(
				new PointerEvent(pressed ? "pointerdown" : "pointerup", {
					...options,
					pointerId: 1,
					pointerType: "mouse",
					isPrimary: true,
				}),
			);
		}
		target.dispatchEvent(new MouseEvent(type, options));
	}

	function snapshot() {
		return {
			eventCount: eventCount,
			lastEvent: lastEvent,
			pressedButtons: Array.from(pressedButtons).sort(function (left, right) {
				return left - right;
			}),
		};
	}

	function dispatch(eventPayload) {
		listeners.forEach(function (listener) {
			try {
				listener(eventPayload);
			} catch (error) {
				reportSuppressedError("Mouse listener failed", error);
			}
		});
	}

	function onBridgeEvent(payload) {
		const button = Number(payload?.button);
		if (!isSupportedButton(button)) {
			return;
		}

		const pressed = Boolean(payload?.pressed);
		if (pressed) {
			pressedButtons.add(button);
		} else {
			pressedButtons.delete(button);
		}

		const x = Number(payload?.x);
		const y = Number(payload?.y);
		if (Number.isFinite(x) && Number.isFinite(y)) {
			dispatchDomEvent(button, pressed, x, y);
		}

		eventCount += 1;
		lastEvent = createEvent(button, pressed);
		dispatch(lastEvent);
	}

	function addListener(listener) {
		listeners.add(listener);
	}

	function removeListener(listener) {
		listeners.delete(listener);
	}

	function install(bridge) {
		bridge.on(EXTRA_BUTTON_CHANNEL, onBridgeEvent);
		bridge.handle(STATE_REQUEST_CHANNEL, function () {
			return snapshot();
		});

		globalThis.grapheneMouse = {
			[INSTALLED_FLAG]: true,
			CHANNEL: EXTRA_BUTTON_CHANNEL,
			BUTTON_6: BUTTON_6,
			BUTTON_7: BUTTON_7,
			BUTTON_8: BUTTON_8,
			on: function (listener) {
				addListener(listener);
				return function () {
					removeListener(listener);
				};
			},
			off: function (listener) {
				removeListener(listener);
			},
			isSideButton: function (button) {
				return isSupportedButton(Number(button));
			},
			isPressed: function (button) {
				const normalizedButton = Number(button);
				return pressedButtons.has(normalizedButton);
			},
			snapshot: snapshot,
		};
	}

	if (globalThis.grapheneMouse?.[INSTALLED_FLAG]) {
		return;
	}

	const bridge = globalThis.grapheneBridge;
	if (
		bridge &&
		typeof bridge.on === "function" &&
		typeof bridge.handle === "function"
	) {
		install(bridge);
	}
})();
