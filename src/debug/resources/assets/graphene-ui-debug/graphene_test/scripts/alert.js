(function () {
    function showAlert() {
        alert("Hello from Graphene");
    }

    function showConfirm() {
        confirm("Confirm from Graphene");
    }

    function showPrompt() {
        prompt("Prompt from Graphene", "default value");
    }

    document.getElementById("show-alert").addEventListener("click", showAlert);
    document.getElementById("show-confirm").addEventListener("click", showConfirm);
    document.getElementById("show-prompt").addEventListener("click", showPrompt);
})();
