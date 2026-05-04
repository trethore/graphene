package tytoo.grapheneui.internal.browser.input;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;

import java.util.Objects;

final class GrapheneWebViewKeyboardController {
    private final GrapheneBrowser browser;

    GrapheneWebViewKeyboardController(GrapheneBrowser browser) {
        this.browser = Objects.requireNonNull(browser, "browser");
    }

    void keyPressed(KeyEvent keyEvent) {
        browser.keyPressed(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers());
    }

    void keyReleased(KeyEvent keyEvent) {
        browser.keyReleased(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers());
    }

    void characterTyped(CharacterEvent characterEvent) {
        browser.textInput(new String(Character.toChars(characterEvent.codepoint())));
    }

    void reset() {
        browser.resetKeyboardState();
    }
}
