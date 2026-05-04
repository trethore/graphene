(function () {
    const preventSideButtonsCheckbox = document.getElementById("prevent-side-buttons");
    const statusElement = document.getElementById("status");
    const handledEventTypes = ["mousedown", "mouseup", "auxclick"];

    function isSideButton(event) {
        return event.button === 3 || event.button === 4;
    }

    function updateStatus() {
        statusElement.textContent = preventSideButtonsCheckbox.checked
            ? "Prevention is enabled. Press mouse back or forward to verify that navigation is blocked."
            : "Prevention is disabled. Mouse back and forward keep their default behavior.";
    }

    function handleMouseEvent(event) {
        if (!preventSideButtonsCheckbox.checked || !isSideButton(event) || !event.cancelable) {
            return;
        }

        event.preventDefault();
        statusElement.textContent = "preventDefault() called on " + event.type + " for button " + event.button + ".";
    }

    function installListeners() {
        handledEventTypes.forEach(function (eventType) {
            window.addEventListener(eventType, handleMouseEvent, true);
        });

        preventSideButtonsCheckbox.addEventListener("change", updateStatus);
    }

    installListeners();
    updateStatus();
})();
