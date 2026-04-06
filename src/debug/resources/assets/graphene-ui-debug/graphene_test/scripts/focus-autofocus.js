const input = document.getElementById("autofocus-input");
const activeElementLabel = document.getElementById("active-element");
const lastKeyLabel = document.getElementById("last-key");
const inputLengthLabel = document.getElementById("input-length");

function describeActiveElement() {
    const activeElement = document.activeElement;
    if (!activeElement) {
        return "none";
    }

    const idSuffix = activeElement.id ? "#" + activeElement.id : "";
    return activeElement.tagName.toLowerCase() + idSuffix;
}

function updateStatus() {
    activeElementLabel.textContent = describeActiveElement();
    inputLengthLabel.textContent = String(input.value.length);
}

function focusInput() {
    input.focus();
    input.select();
    updateStatus();
}

window.addEventListener("load", focusInput);
document.addEventListener("focusin", updateStatus);
document.addEventListener("keydown", event => {
    lastKeyLabel.textContent = event.key;
    updateStatus();
});
input.addEventListener("input", updateStatus);

updateStatus();
