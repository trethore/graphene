(function () {
	"use strict";

	const INSTALLED_FLAG = "__graphenePopupFallbackInstalled";
	if (globalThis[INSTALLED_FLAG]) {
		return;
	}

	function findAnchor(event) {
		const target = event?.target;
		if (target && typeof target.closest === "function") {
			const anchor = target.closest("a[href]");
			if (anchor) {
				return anchor;
			}
		}

		if (typeof event?.composedPath !== "function") {
			return null;
		}

		for (const node of event.composedPath()) {
			if (!node || typeof node.closest !== "function") {
				continue;
			}

			const anchor = node.closest("a[href]");
			if (anchor) {
				return anchor;
			}
		}

		return null;
	}

	function handleBlankTargetClick(event) {
		const anchor = findAnchor(event);
		if (!anchor) {
			return;
		}

		const target = String(anchor.getAttribute("target") || "").toLowerCase();
		if (target !== "_blank" || !anchor.href) {
			return;
		}

		event.preventDefault();
		event.stopPropagation();
		globalThis.location?.assign?.(anchor.href);
	}

	function patchWindowOpen() {
		const nativeOpen =
			typeof globalThis.open === "function"
				? globalThis.open.bind(globalThis)
				: null;

		globalThis.open = function (url, target, features) {
			const hasUrl = typeof url === "string" && url.length > 0;
			const normalizedTarget =
				typeof target === "string" ? target.toLowerCase() : "";
			if (
				hasUrl &&
				(normalizedTarget === "_blank" || normalizedTarget === "")
			) {
				globalThis.location?.assign?.(url);
				return globalThis;
			}

			return nativeOpen ? nativeOpen(url, target, features) : null;
		};
	}

	globalThis[INSTALLED_FLAG] = true;
	document.addEventListener("click", handleBlankTargetClick, true);
	patchWindowOpen();
})();
