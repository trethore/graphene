const typingArea = document.getElementById("typing-area");
const activeElementLabel = document.getElementById("active-element");
const characterCountLabel = document.getElementById("character-count");
const eventCountLabel = document.getElementById("event-count");
const lastEventLabel = document.getElementById("last-event");
const environmentElement = document.getElementById("environment");
const eventLogBody = document.getElementById("event-log");
const clearButton = document.getElementById("clear-log");
const copyButton = document.getElementById("copy-report");
const copyStatus = document.getElementById("copy-status");

const records = [];
let nextSequence = 1;

function valueOrEmpty(value) {
	return value === undefined || value === null ? "" : String(value);
}

function quoted(value) {
	return value === undefined || value === null
		? ""
		: JSON.stringify(String(value));
}

function describeElement(element) {
	if (!element || !element.tagName) {
		return "none";
	}

	const id = element.id ? "#" + element.id : "";
	return element.tagName.toLowerCase() + id;
}

function modifierNames(event) {
	const modifiers = [];
	const directModifiers = [
		["ctrlKey", "Ctrl"],
		["altKey", "Alt"],
		["shiftKey", "Shift"],
		["metaKey", "Meta"],
	];
	const modifierStates = [
		"AltGraph",
		"CapsLock",
		"NumLock",
		"ScrollLock",
		"Fn",
		"FnLock",
		"Symbol",
		"SymbolLock",
	];

	directModifiers.forEach(([property, name]) => {
		if (event[property]) {
			modifiers.push(name);
		}
	});
	if (typeof event.getModifierState === "function") {
		modifierStates.forEach((name) => {
			if (event.getModifierState(name)) {
				modifiers.push(name);
			}
		});
	}
	return modifiers;
}

function eventFlags(event) {
	const flags = modifierNames(event);
	if (event.repeat) {
		flags.push("repeat");
	}
	if (event.isComposing) {
		flags.push("composing");
	}
	if (event.defaultPrevented) {
		flags.push("prevented");
	}
	if (event.cancelable) {
		flags.push("cancelable");
	}
	return flags.join("+") || "-";
}

function createRecord(event) {
	const isInputEvent = event.type === "beforeinput" || event.type === "input";
	return {
		sequence: nextSequence++,
		type: event.type,
		keyOrData:
			isInputEvent || event.type.startsWith("composition")
				? quoted(event.data)
				: quoted(event.key),
		codeOrInputType: isInputEvent
			? valueOrEmpty(event.inputType)
			: valueOrEmpty(event.code),
		location: valueOrEmpty(event.location),
		which: valueOrEmpty(event.which),
		keyCode: valueOrEmpty(event.keyCode),
		charCode: valueOrEmpty(event.charCode),
		flags: eventFlags(event),
		target: describeElement(event.target),
		activeElement: describeElement(document.activeElement),
		timeStamp: Number(event.timeStamp).toFixed(3),
		text: typingArea.value,
	};
}

function appendCell(row, value) {
	const cell = document.createElement("td");
	cell.textContent = value;
	row.appendChild(cell);
}

function appendRecord(record) {
	const row = document.createElement("tr");
	[
		record.sequence,
		record.type,
		record.keyOrData,
		record.codeOrInputType,
		record.location,
		record.which,
		record.keyCode,
		record.charCode,
		record.flags,
		record.target,
	].forEach((value) => appendCell(row, value));
	eventLogBody.appendChild(row);
	row.scrollIntoView({ block: "nearest" });
}

function updateStatus(lastRecord) {
	activeElementLabel.textContent = describeElement(document.activeElement);
	characterCountLabel.textContent = String(typingArea.value.length);
	eventCountLabel.textContent = String(records.length);
	lastEventLabel.textContent = lastRecord ? lastRecord.type : "none";
}

function recordEvent(event) {
	const record = createRecord(event);
	records.push(record);
	appendRecord(record);
	updateStatus(record);
}

function environmentReport() {
	const lines = [
		"userAgent: " + navigator.userAgent,
		"platform: " + valueOrEmpty(navigator.platform),
		"language: " + valueOrEmpty(navigator.language),
		"languages: " + JSON.stringify(navigator.languages || []),
		"hardwareConcurrency: " + valueOrEmpty(navigator.hardwareConcurrency),
		"devicePixelRatio: " + valueOrEmpty(window.devicePixelRatio),
		"screen: " + screen.width + "x" + screen.height,
		"origin: " + location.origin,
		"secureContext: " + String(window.isSecureContext),
		"clipboardApi: " + String(Boolean(navigator.clipboard)),
		"page URL: " + location.href,
	];
	return lines.join("\n");
}

function tabSeparatedRecord(record) {
	return [
		record.sequence,
		record.type,
		record.keyOrData,
		record.codeOrInputType,
		record.location,
		record.which,
		record.keyCode,
		record.charCode,
		record.flags,
		record.target,
		record.activeElement,
		record.timeStamp,
		JSON.stringify(record.text),
	].join("\t");
}

function buildReport() {
	const header = [
		"#",
		"type",
		"key/data",
		"code/inputType",
		"location",
		"which",
		"keyCode",
		"charCode",
		"modifiers/flags",
		"target",
		"activeElement",
		"timeStamp",
		"textAfterEvent",
	].join("\t");
	return [
		"Graphene keyboard diagnostics",
		"Generated: " + new Date().toISOString(),
		"",
		environmentReport(),
		"",
		"Final text: " + JSON.stringify(typingArea.value),
		"",
		header,
		...records.map(tabSeparatedRecord),
	].join("\n");
}

function copyWithDocumentCommand(report) {
	const temporaryArea = document.createElement("textarea");
	temporaryArea.value = report;
	temporaryArea.setAttribute("readonly", "");
	temporaryArea.style.position = "fixed";
	temporaryArea.style.left = "-10000px";
	temporaryArea.style.top = "0";
	document.body.appendChild(temporaryArea);
	temporaryArea.focus();
	temporaryArea.select();
	temporaryArea.setSelectionRange(0, temporaryArea.value.length);

	let copied = false;
	try {
		copied = document.execCommand("copy");
	} finally {
		temporaryArea.remove();
		typingArea.focus();
	}
	return copied;
}

function copyReport() {
	const report = buildReport();
	if (
		navigator.clipboard &&
		typeof navigator.clipboard.writeText === "function"
	) {
		navigator.clipboard.writeText(report).then(
			() => (copyStatus.textContent = "Report copied using the Clipboard API."),
			(error) => copyWithFallback(report, error),
		);
		return;
	}

	copyWithFallback(report, new Error("Clipboard API is unavailable"));
}

function copyWithFallback(report, clipboardError) {
	if (copyWithDocumentCommand(report)) {
		copyStatus.textContent = "Report copied to clipboard.";
	} else {
		copyStatus.textContent =
			"Copy failed: " + (clipboardError?.message || String(clipboardError));
	}
}

function clearLog() {
	records.length = 0;
	nextSequence = 1;
	eventLogBody.replaceChildren();
	copyStatus.textContent = "";
	updateStatus(null);
	typingArea.focus();
}

[
	"keydown",
	"keypress",
	"keyup",
	"beforeinput",
	"input",
	"compositionstart",
	"compositionupdate",
	"compositionend",
].forEach((type) => document.addEventListener(type, recordEvent, true));

document.addEventListener("focusin", () => updateStatus(records.at(-1)));
document.addEventListener("focusout", () => updateStatus(records.at(-1)));
typingArea.addEventListener("select", () => updateStatus(records.at(-1)));
clearButton.addEventListener("click", clearLog);
copyButton.addEventListener("click", copyReport);

environmentElement.textContent = environmentReport();
updateStatus(null);
