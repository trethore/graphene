(function () {
    const RUN_TESTS_CHANNEL = "debug:tests:run";
    let bridge = null;

    function statusElement() {
        return document.getElementById("status");
    }

    function summaryElement() {
        return document.getElementById("run-summary");
    }

    function resultsBodyElement() {
        return document.getElementById("results-body");
    }

    function runButtonElement() {
        return document.getElementById("run-tests");
    }

    function renderNoResults(message) {
        resultsBodyElement().innerHTML = "<tr><td colspan=\"4\">" + message + "</td></tr>";
    }

    function renderSummary(report) {
        if (!report) {
            summaryElement().textContent = "No test report received.";
            return;
        }

        const outcome = report.ok ? "PASS" : "FAIL";
        const total = Number(report.totalCount || 0);
        const passed = Number(report.passedCount || 0);
        const failed = Number(report.failedCount || 0);
        const durationMs = Number(report.durationMs || 0);
        summaryElement().textContent =
            "Run result: " + outcome + "\n" +
            "Passed: " + passed + " / " + total + "\n" +
            "Failed: " + failed + "\n" +
            "Duration: " + durationMs + " ms";
    }

    function renderResults(report) {
        const results = Array.isArray(report?.results) ? report.results : [];
        if (results.length === 0) {
            if (report?.error) {
                const details = (report.error?.type || "Error") + ": " + (report.error?.message || "Unknown error");
                renderNoResults("Test runner error: " + details);
                return;
            }

            renderNoResults("No test cases were executed.");
            return;
        }

        const rowsHtml = results.map(renderResultRow).join("");

        resultsBodyElement().innerHTML = rowsHtml;
    }

    function renderResultRow(result) {
        const passed = Boolean(result?.passed);
        const statusLabel = passed ? "PASS" : "FAIL";
        const statusClass = passed ? "pass" : "fail";
        const durationMs = Number(result?.durationMs ?? 0);
        const testName = String(result?.name ?? "unknown");
        const detailText = !passed && result?.error
            ? ((result.error?.type || "Error") + ": " + (result.error?.message || "Unknown error"))
            : "-";

        return "<tr>" +
            "<td>" + testName + "</td>" +
            "<td class=\"" + statusClass + "\">" + statusLabel + "</td>" +
            "<td>" + durationMs + "</td>" +
            "<td>" + detailText + "</td>" +
            "</tr>";
    }

    function parseReport(payload) {
        if (!payload) {
            return null;
        }

        if (typeof payload === "string") {
            try {
                return JSON.parse(payload);
            } catch (error) {
                globalThis.console?.error?.("[Graphene Debug] Failed to parse automated test report", error);
                return null;
            }
        }

        return payload;
    }

    function setRunningState(running) {
        runButtonElement().disabled = running;
        runButtonElement().textContent = running ? "Running..." : "Run Tests";
    }

    function connectBridgeWhenAvailable() {
        const candidate = globalThis.grapheneBridge;
        if (!candidate || typeof candidate.request !== "function") {
            setTimeout(connectBridgeWhenAvailable, 50);
            return;
        }

        bridge = candidate;
        statusElement().textContent = "Bridge status: connected";
    }

    function runTests() {
        if (!bridge) {
            statusElement().textContent = "Bridge status: not ready";
            return;
        }

        setRunningState(true);
        statusElement().textContent = "Bridge status: running tests...";
        bridge.request(RUN_TESTS_CHANNEL, {
            requestedAt: new Date().toISOString()
        }).then(onRunSuccess)
            .catch(onRunFailure)
            .finally(onRunFinished);
    }

    function onRunSuccess(payload) {
        const report = parseReport(payload);
        renderSummary(report);
        renderResults(report);
        statusElement().textContent = "Bridge status: connected";
    }

    function onRunFailure(error) {
        renderNoResults("Request failed: " + (error?.message || String(error)));
        summaryElement().textContent = "Run failed before report was produced.";
        statusElement().textContent = "Bridge status: connected";
    }

    function onRunFinished() {
        setRunningState(false);
    }

    runButtonElement().addEventListener("click", runTests);
    connectBridgeWhenAvailable();
})();
