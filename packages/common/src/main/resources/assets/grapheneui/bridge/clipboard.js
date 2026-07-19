(function () {
	"use strict";

	const BRIDGE_NAME = "grapheneui";
	const BRIDGE_INSTALLED_PROPERTY = "__grapheneInstalled";
	const PROTOCOL_VERSION = 1;
	const DOCUMENT_INSTALLED_FLAG = "__grapheneClipboardDocumentInstalled";
	const WRITE_PATCHES_INSTALLED_FLAG =
		"__grapheneClipboardWritePatchesInstalled";
	const PASTE_FUNCTION = "__grapheneClipboardPasteFromHost";
	const WRITE_CHANNEL = "graphene:clipboard:write";

	function reportSuppressedError(context, error) {
		const consoleObject = globalThis.console;
		if (consoleObject && typeof consoleObject.debug === "function") {
			consoleObject.debug("[GrapheneClipboard] " + context, error);
		}
	}

	function inputSelection(target) {
		if (
			!(target instanceof HTMLInputElement) &&
			!(target instanceof HTMLTextAreaElement)
		) {
			return "";
		}

		const selectionStart = target.selectionStart;
		const selectionEnd = target.selectionEnd;
		if (
			selectionStart === null ||
			selectionEnd === null ||
			selectionStart === selectionEnd
		) {
			return "";
		}

		return target.value.substring(selectionStart, selectionEnd);
	}

	function selectedText(event) {
		const clipboardText = event.clipboardData?.getData("text/plain");
		if (clipboardText) {
			return clipboardText;
		}

		const inputText = inputSelection(event.target);
		if (inputText) {
			return inputText;
		}

		return globalThis.getSelection?.().toString() ?? "";
	}

	function selectedHtml(event) {
		const clipboardHtml = event.clipboardData?.getData("text/html");
		if (clipboardHtml) {
			return clipboardHtml;
		}

		const selection = globalThis.getSelection?.();
		if (!selection || selection.rangeCount === 0) {
			return "";
		}

		const container = document.createElement("div");
		for (let index = 0; index < selection.rangeCount; index += 1) {
			container.append(selection.getRangeAt(index).cloneContents());
		}
		return container.innerHTML;
	}

	function hasPayload(payload) {
		return Boolean(payload.text || payload.html || payload.png);
	}

	function emitDirect(payload) {
		return new Promise(function (resolve, reject) {
			const cefQuery = globalThis["cefQuery"];
			if (typeof cefQuery !== "function") {
				reject(new Error("cefQuery is unavailable"));
				return;
			}

			cefQuery({
				request: JSON.stringify({
					bridge: BRIDGE_NAME,
					version: PROTOCOL_VERSION,
					kind: "event",
					channel: WRITE_CHANNEL,
					payload: payload,
				}),
				onSuccess: resolve,
				onFailure: function (_, errorMessage) {
					reject(new Error(errorMessage));
				},
			});
		});
	}

	function emitPayload(payload) {
		if (!hasPayload(payload)) {
			return Promise.resolve();
		}
		return emitDirect(payload);
	}

	function bufferToBase64(buffer) {
		const bytes = new Uint8Array(buffer);
		const chunkSize = 0x8000;
		let binary = "";
		for (let offset = 0; offset < bytes.length; offset += chunkSize) {
			binary += String.fromCodePoint(
				...bytes.subarray(offset, offset + chunkSize),
			);
		}
		return globalThis.btoa(binary);
	}

	async function payloadFromItems(items) {
		const payload = { text: null, html: null, png: null };
		for (const item of items ?? []) {
			for (const type of item.types ?? []) {
				if (
					type !== "text/plain" &&
					type !== "text/html" &&
					type !== "image/png"
				) {
					continue;
				}

				const blob = await item.getType(type);
				if (type === "text/plain" && payload.text === null) {
					payload.text = await blob.text();
				} else if (type === "text/html" && payload.html === null) {
					payload.html = await blob.text();
				} else if (type === "image/png" && payload.png === null) {
					payload.png = bufferToBase64(await blob.arrayBuffer());
				}
			}
		}
		return payload;
	}

	function syncNativeRead() {
		const clipboard = globalThis.navigator?.clipboard;
		if (!clipboard || typeof clipboard.read !== "function") {
			return;
		}

		globalThis.setTimeout(function () {
			clipboard
				.read()
				.then(payloadFromItems)
				.then(function (payload) {
					return emitPayload(payload);
				})
				.catch(function (error) {
					reportSuppressedError(
						"Failed to read browser clipboard formats",
						error,
					);
				});
		}, 0);
	}

	function onCopy(event) {
		if (!event.isTrusted) {
			return;
		}

		const payload = {
			text: selectedText(event),
			html: selectedHtml(event),
			png: null,
		};
		emitPayload(payload).catch(function (error) {
			reportSuppressedError("Failed to write host clipboard", error);
		});
		if (payload.html.includes("<img")) {
			syncNativeRead();
		}
	}

	function deepActiveElement() {
		let activeElement = document.activeElement;
		while (activeElement?.shadowRoot?.activeElement) {
			activeElement = activeElement.shadowRoot.activeElement;
		}
		return activeElement ?? document.body;
	}

	function base64ToBytes(base64) {
		const binary = globalThis.atob(base64);
		const bytes = new Uint8Array(binary.length);
		for (let index = 0; index < binary.length; index += 1) {
			bytes[index] = binary.codePointAt(index) ?? 0;
		}
		return bytes;
	}

	function dispatchPaste(target, payload) {
		const clipboardData = new DataTransfer();
		if (payload.text) {
			clipboardData.setData("text/plain", payload.text);
		}
		if (payload.html) {
			clipboardData.setData("text/html", payload.html);
		}
		if (payload.png) {
			const file = new File([base64ToBytes(payload.png)], "clipboard.png", {
				type: "image/png",
			});
			clipboardData.items.add(file);
		}
		const pasteEvent = new ClipboardEvent("paste", {
			bubbles: true,
			cancelable: true,
			composed: true,
			clipboardData: clipboardData,
		});
		return target.dispatchEvent(pasteEvent);
	}

	function dispatchInput(target, type, text) {
		return target.dispatchEvent(
			new InputEvent(type, {
				bubbles: true,
				cancelable: type === "beforeinput",
				composed: true,
				data: text,
				inputType: "insertFromPaste",
			}),
		);
	}

	function insertIntoInput(target, text) {
		if (
			!(target instanceof HTMLInputElement) &&
			!(target instanceof HTMLTextAreaElement)
		) {
			return false;
		}

		if (!dispatchInput(target, "beforeinput", text)) {
			return true;
		}

		const selectionStart = target.selectionStart ?? target.value.length;
		const selectionEnd = target.selectionEnd ?? selectionStart;
		target.setRangeText(text, selectionStart, selectionEnd, "end");
		dispatchInput(target, "input", text);
		return true;
	}

	function createHtmlFragment(html) {
		const template = document.createElement("template");
		template.innerHTML = html;
		template.content
			.querySelectorAll("script, iframe, object, embed, meta")
			.forEach(function (element) {
				element.remove();
			});
		template.content.querySelectorAll("*").forEach(function (element) {
			for (const attribute of Array.from(element.attributes)) {
				const name = attribute.name.toLowerCase();
				const value = attribute.value.trim().toLowerCase();
				if (name.startsWith("on") || value.startsWith("javascript:")) {
					element.removeAttribute(attribute.name);
				}
			}
		});
		return template.content;
	}

	function insertRichContent(target, payload) {
		const editableTarget = target?.closest?.(
			"[contenteditable]:not([contenteditable='false'])",
		);
		if (!editableTarget) {
			return false;
		}

		editableTarget.focus();
		const selection = globalThis.getSelection?.();
		if (!selection) {
			return false;
		}

		let range = selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
		if (!range || !editableTarget.contains(range.commonAncestorContainer)) {
			range = document.createRange();
			range.selectNodeContents(editableTarget);
			range.collapse(false);
		}

		if (!dispatchInput(editableTarget, "beforeinput", payload.text ?? "")) {
			return true;
		}

		range.deleteContents();
		const fragment = document.createDocumentFragment();
		if (payload.html) {
			fragment.append(createHtmlFragment(payload.html));
		} else if (payload.png) {
			const image = document.createElement("img");
			image.alt = "";
			image.src = "data:image/png;base64," + payload.png;
			fragment.append(image);
		} else {
			fragment.append(document.createTextNode(payload.text ?? ""));
		}

		const lastNode = fragment.lastChild;
		range.insertNode(fragment);
		if (lastNode) {
			range.setStartAfter(lastNode);
			range.collapse(true);
			selection.removeAllRanges();
			selection.addRange(range);
		}
		dispatchInput(editableTarget, "input", payload.text ?? "");
		return true;
	}

	function onPaste(payload) {
		const normalizedPayload = {
			text: typeof payload?.text === "string" ? payload.text : "",
			html: typeof payload?.html === "string" ? payload.html : "",
			png: typeof payload?.png === "string" ? payload.png : "",
		};
		const target = deepActiveElement();
		if (!target || !dispatchPaste(target, normalizedPayload)) {
			return;
		}

		if (insertIntoInput(target, normalizedPayload.text)) {
			return;
		}

		insertRichContent(target, normalizedPayload);
	}

	function patchWrites() {
		const clipboard = globalThis.navigator?.clipboard;
		if (!clipboard || typeof clipboard.writeText !== "function") {
			return;
		}

		const nativeWriteText = clipboard.writeText.bind(clipboard);
		const synchronizedWriteText = function (text) {
			const normalizedText = String(text);
			return nativeWriteText(normalizedText).then(function () {
				return emitPayload({
					text: normalizedText,
					html: null,
					png: null,
				});
			});
		};

		try {
			Object.defineProperty(clipboard, "writeText", {
				configurable: true,
				value: synchronizedWriteText,
			});
		} catch (error) {
			reportSuppressedError(
				"Failed to synchronize navigator.clipboard.writeText",
				error,
			);
		}

		if (typeof clipboard.write !== "function") {
			return;
		}

		const nativeWrite = clipboard.write.bind(clipboard);
		const synchronizedWrite = function (items) {
			return nativeWrite(items)
				.then(function () {
					return payloadFromItems(items);
				})
				.then(function (payload) {
					return emitPayload(payload);
				});
		};

		try {
			Object.defineProperty(clipboard, "write", {
				configurable: true,
				value: synchronizedWrite,
			});
		} catch (error) {
			reportSuppressedError(
				"Failed to synchronize navigator.clipboard.write",
				error,
			);
		}
	}

	function installWritePatches() {
		globalThis[WRITE_PATCHES_INSTALLED_FLAG] = true;
		patchWrites();
	}

	globalThis[PASTE_FUNCTION] = onPaste;
	if (!globalThis[DOCUMENT_INSTALLED_FLAG]) {
		globalThis[DOCUMENT_INSTALLED_FLAG] = true;
		globalThis.addEventListener("copy", onCopy);
		globalThis.addEventListener("cut", onCopy);
	}

	const bridge = globalThis.grapheneBridge;
	if (
		!globalThis[WRITE_PATCHES_INSTALLED_FLAG] &&
		bridge?.[BRIDGE_INSTALLED_PROPERTY] === true
	) {
		installWritePatches();
	}
})();
