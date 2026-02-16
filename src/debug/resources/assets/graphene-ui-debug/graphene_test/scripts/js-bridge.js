(function () {
    const CHANNEL_EVENT = "debug:event";
    const CHANNEL_ECHO = "debug:echo";
    const CHANNEL_SUM = "debug:sum";
    const CHANNEL_DEVTOOLS_STATUS = "debug:devtools-status";
    const CHANNEL_JAVA_TO_JS_EVENT = "debug:bridge:java-event";
    const CHANNEL_JAVA_TO_JS_REQUEST = "debug:bridge:java-request";
    const CHANNEL_JAVA_TO_JS_TRIGGER = "debug:bridge:trigger-java-to-js";
    let bridge = null;

    function bridgeStatusElement() {
        return document.getElementById("bridge-status");
    }

    function responseLogElement() {
        return document.getElementById("response-log");
    }

    function setBridgeStatus(text) {
        bridgeStatusElement().textContent = text;
    }

    function appendLogLine(label, value) {
        const log = responseLogElement();
        const rendered = typeof value === "string" ? value : JSON.stringify(value, null, 2);
        log.textContent = "[" + new Date().toISOString() + "] " + label + "\n" + rendered + "\n\n" + log.textContent;
    }

    function requireBridge() {
        if (!bridge) {
            appendLogLine("Bridge", "Bridge is not ready yet.");
            return null;
        }

        return bridge;
    }

    function onDevToolsStatus(payload) {
        appendLogLine("Java -> JS event debug:devtools-status", payload);
    }

    function onJavaToJsEvent(payload) {
        appendLogLine("Java -> JS event debug:bridge:java-event", payload);
    }

    function onJavaToJsRequest(payload) {
        const responsePayload = {
            ok: true,
            kind: "js-handler-response",
            receivedAt: new Date().toISOString(),
            received: payload
        };
        appendLogLine("Java -> JS request debug:bridge:java-request", payload);
        appendLogLine("JS -> Java response debug:bridge:java-request", responsePayload);
        return responsePayload;
    }

    function connectBridgeWhenAvailable() {
        const candidate = globalThis.grapheneBridge;
        if (!candidate || typeof candidate.emit !== "function" || typeof candidate.request !== "function") {
            setTimeout(connectBridgeWhenAvailable, 50);
            return;
        }

        bridge = candidate;
        setBridgeStatus("Bridge status: connected");
        bridge.on(CHANNEL_DEVTOOLS_STATUS, onDevToolsStatus);
        bridge.on(CHANNEL_JAVA_TO_JS_EVENT, onJavaToJsEvent);
        bridge.handle(CHANNEL_JAVA_TO_JS_REQUEST, onJavaToJsRequest);
        appendLogLine("Bridge", "Connected and listeners registered.");
    }

    function sendEventToJava() {
        const currentBridge = requireBridge();
        if (!currentBridge) {
            return;
        }

        const payload = {
            text: document.getElementById("event-text").value,
            sentAt: new Date().toISOString(),
            page: "js-bridge"
        };

        currentBridge.emit(CHANNEL_EVENT, payload)
            .then(function () {
                appendLogLine("JS -> Java event debug:event", payload);
            })
            .catch(function (error) {
                appendLogLine("JS -> Java event error", error?.message || String(error));
            });
    }

    function sendEchoRequest() {
        const currentBridge = requireBridge();
        if (!currentBridge) {
            return;
        }

        const payload = {
            text: document.getElementById("echo-text").value,
            sentAt: new Date().toISOString(),
            page: "js-bridge"
        };

        currentBridge.request(CHANNEL_ECHO, payload)
            .then(function (responsePayload) {
                appendLogLine("JS -> Java request debug:echo", responsePayload);
            })
            .catch(function (error) {
                appendLogLine("JS -> Java request error", error?.message || String(error));
            });
    }

    function sendSumRequest() {
        const currentBridge = requireBridge();
        if (!currentBridge) {
            return;
        }

        const payload = {
            a: Number(document.getElementById("sum-a").value),
            b: Number(document.getElementById("sum-b").value)
        };

        currentBridge.request(CHANNEL_SUM, payload)
            .then(function (responsePayload) {
                appendLogLine("JS -> Java request debug:sum", responsePayload);
            })
            .catch(function (error) {
                appendLogLine("JS -> Java request error", error?.message || String(error));
            });
    }

    function triggerJavaToJsRoundTrip() {
        const currentBridge = requireBridge();
        if (!currentBridge) {
            return;
        }

        const payload = {
            probe: document.getElementById("java-probe").value,
            sentAt: new Date().toISOString(),
            page: "js-bridge"
        };

        currentBridge.request(CHANNEL_JAVA_TO_JS_TRIGGER, payload)
            .then(function (responsePayload) {
                appendLogLine("JS -> Java request debug:bridge:trigger-java-to-js", responsePayload);
            })
            .catch(function (error) {
                appendLogLine("Round trip error", error?.message || String(error));
            });
    }

    function installHandlers() {
        document.getElementById("send-event").addEventListener("click", sendEventToJava);
        document.getElementById("send-echo").addEventListener("click", sendEchoRequest);
        document.getElementById("send-sum").addEventListener("click", sendSumRequest);
        document.getElementById("run-java-to-js").addEventListener("click", triggerJavaToJsRoundTrip);
    }

    installHandlers();
    connectBridgeWhenAvailable();
})();
