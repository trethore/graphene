package tytoo.grapheneui.cef.alert;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.cef.handler.CefJSDialogHandler;
import org.jspecify.annotations.NonNull;

final class GrapheneJsDialogScreen extends Screen {
    private static final int DIALOG_WIDTH = 340;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int VERTICAL_TEXT_START = 64;
    private static final int TEXT_LINE_HEIGHT = 9;
    private static final int CONTENT_HORIZONTAL_PADDING = 40;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int MESSAGE_TO_INPUT_SPACING = 12;
    private static final int INPUT_TO_BUTTON_SPACING = 12;
    private static final int BOTTOM_PADDING = 28;
    private static final String EMPTY_VALUE = "";
    private static final Component ALERT_TITLE = Component.literal("JavaScript Alert");
    private static final Component CONFIRM_TITLE = Component.literal("JavaScript Confirm");
    private static final Component PROMPT_TITLE = Component.literal("JavaScript Prompt");
    private static final Component DIALOG_TITLE = Component.literal("JavaScript Dialog");

    private final GrapheneJsDialogRequest request;
    private final Screen returnScreen;
    private final GrapheneJsDialogCompletionHandler completionHandler;
    private MultiLineLabel messageLabel = MultiLineLabel.EMPTY;
    private EditBox promptInput;
    private boolean completed;

    GrapheneJsDialogScreen(
            GrapheneJsDialogRequest request,
            Screen returnScreen,
            GrapheneJsDialogCompletionHandler completionHandler
    ) {
        super(titleFor(request.dialogType()));
        this.request = request;
        this.returnScreen = returnScreen;
        this.completionHandler = completionHandler;
    }

    private static Component titleFor(CefJSDialogHandler.JSDialogType dialogType) {
        if (dialogType == CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT) {
            return ALERT_TITLE;
        }

        if (dialogType == CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_CONFIRM) {
            return CONFIRM_TITLE;
        }

        if (dialogType == CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_PROMPT) {
            return PROMPT_TITLE;
        }

        return DIALOG_TITLE;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(DIALOG_WIDTH, this.width - CONTENT_HORIZONTAL_PADDING);
        int contentX = (this.width - contentWidth) / 2;
        this.messageLabel = MultiLineLabel.create(this.font, Component.literal(request.messageText()), contentWidth);

        int contentY = VERTICAL_TEXT_START + (this.messageLabel.getLineCount() * TEXT_LINE_HEIGHT) + MESSAGE_TO_INPUT_SPACING;
        if (isPromptDialog()) {
            promptInput = new EditBox(this.font, contentX, contentY, contentWidth, BUTTON_HEIGHT, Component.empty());
            promptInput.setValue(request.defaultPromptText());
            promptInput.setMaxLength(Integer.MAX_VALUE);
            this.addRenderableWidget(promptInput);
            this.setInitialFocus(promptInput);
            contentY += BUTTON_HEIGHT + INPUT_TO_BUTTON_SPACING;
        }

        int buttonsY = Math.min(contentY, this.height - BOTTOM_PADDING);
        addButtons(buttonsY);
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 40, TITLE_COLOR);
        ActiveTextCollector activeTextCollector = guiGraphics.textRenderer();
        this.messageLabel.visitLines(TextAlignment.CENTER, this.width / 2, VERTICAL_TEXT_START, TEXT_LINE_HEIGHT, activeTextCollector);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (keyEvent.isConfirmation()) {
            complete(true, valueForAcceptance());
            return true;
        }

        if (keyEvent.key() == 256 && shouldCloseOnEsc()) {
            complete(defaultAcceptedResult(), defaultValueResult());
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !isAlertDialog();
    }

    @Override
    public void onClose() {
        complete(defaultAcceptedResult(), defaultValueResult());
    }

    @Override
    public void removed() {
        if (!completed) {
            complete(defaultAcceptedResult(), defaultValueResult());
        }

        super.removed();
    }

    GrapheneJsDialogRequest request() {
        return request;
    }

    Screen returnScreen() {
        return returnScreen;
    }

    private void addButtons(int y) {
        if (isAlertDialog()) {
            int x = (this.width - BUTTON_WIDTH) / 2;
            this.addRenderableWidget(
                    Button.builder(CommonComponents.GUI_OK, _ -> complete(true, EMPTY_VALUE))
                            .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                            .build()
            );
            return;
        }

        int spacing = 8;
        int totalWidth = BUTTON_WIDTH * 2 + spacing;
        int leftX = (this.width - totalWidth) / 2;
        int rightX = leftX + BUTTON_WIDTH + spacing;
        Component confirmLabel = isPromptDialog() ? CommonComponents.GUI_OK : CommonComponents.GUI_YES;

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, _ -> complete(false, defaultValueResult()))
                        .bounds(leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(confirmLabel, _ -> complete(true, valueForAcceptance()))
                        .bounds(rightX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
    }

    private void complete(boolean accepted, String value) {
        if (completed) {
            return;
        }

        completed = true;
        completionHandler.complete(this, accepted, value);
    }

    private boolean isAlertDialog() {
        return request.dialogType() == CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT;
    }

    private boolean isPromptDialog() {
        return request.dialogType() == CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_PROMPT;
    }

    private boolean defaultAcceptedResult() {
        return isAlertDialog();
    }

    private String defaultValueResult() {
        return EMPTY_VALUE;
    }

    private String valueForAcceptance() {
        if (!isPromptDialog()) {
            return EMPTY_VALUE;
        }

        return promptInput == null ? request.defaultPromptText() : promptInput.getValue();
    }
}
