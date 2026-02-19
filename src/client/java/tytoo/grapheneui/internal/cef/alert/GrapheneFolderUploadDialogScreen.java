package tytoo.grapheneui.internal.cef.alert;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

final class GrapheneFolderUploadDialogScreen extends Screen {
    private static final int DIALOG_WIDTH = 340;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int VERTICAL_TEXT_START = 64;
    private static final int TEXT_LINE_HEIGHT = 9;
    private static final int CONTENT_HORIZONTAL_PADDING = 40;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int BUTTON_SPACING = 8;
    private static final int BOTTOM_PADDING = 28;
    private static final Component DEFAULT_TITLE = Component.literal("Folder Upload");
    private static final Component UPLOAD_LABEL = Component.literal("Upload");
    private static final Component CANCEL_LABEL = Component.literal("Cancel");

    private final GrapheneFolderUploadDialogRequest request;
    private final Screen returnScreen;
    private final GrapheneFolderUploadDialogCompletionHandler completionHandler;
    private final Vector<String> selectedPaths;
    private final Path folderPath;
    private final long fileCount;
    private MultiLineLabel messageLabel = MultiLineLabel.EMPTY;
    private boolean completed;

    GrapheneFolderUploadDialogScreen(
            GrapheneFolderUploadDialogRequest request,
            Screen returnScreen,
            GrapheneFolderUploadDialogCompletionHandler completionHandler,
            Vector<String> selectedPaths,
            Path folderPath,
            long fileCount
    ) {
        super(resolveTitle(Objects.requireNonNull(request, "request").title()));
        this.request = request;
        this.returnScreen = returnScreen;
        this.completionHandler = Objects.requireNonNull(completionHandler, "completionHandler");
        this.selectedPaths = Objects.requireNonNull(selectedPaths, "selectedPaths");
        this.folderPath = Objects.requireNonNull(folderPath, "folderPath");
        this.fileCount = fileCount;
    }

    private static Component resolveTitle(String titleText) {
        if (titleText == null || titleText.isBlank()) {
            return DEFAULT_TITLE;
        }

        return Component.literal(titleText);
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(DIALOG_WIDTH, this.width - CONTENT_HORIZONTAL_PADDING);
        this.messageLabel = MultiLineLabel.create(this.font, Component.literal(buildMessageText()), contentWidth);

        int textHeight = this.messageLabel.getLineCount() * TEXT_LINE_HEIGHT;
        int buttonsY = Math.min(VERTICAL_TEXT_START + textHeight + 12, this.height - BOTTOM_PADDING);
        int totalButtonsWidth = BUTTON_WIDTH * 2 + BUTTON_SPACING;
        int leftX = (this.width - totalButtonsWidth) / 2;
        int rightX = leftX + BUTTON_WIDTH + BUTTON_SPACING;

        this.addRenderableWidget(
                Button.builder(CANCEL_LABEL, ignored -> complete(false))
                        .bounds(leftX, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(UPLOAD_LABEL, ignored -> complete(true))
                        .bounds(rightX, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
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
            complete(true);
            return true;
        }

        if (keyEvent.key() == 256) {
            complete(false);
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        complete(false);
    }

    @Override
    public void removed() {
        if (!completed) {
            complete(false);
        }

        super.removed();
    }

    GrapheneFolderUploadDialogRequest request() {
        return request;
    }

    Screen returnScreen() {
        return returnScreen;
    }

    Vector<String> selectedPaths() {
        return selectedPaths;
    }

    private void complete(boolean accepted) {
        if (completed) {
            return;
        }

        completed = true;
        completionHandler.complete(this, accepted);
    }

    private String buildMessageText() {
        String folderName = folderPath.getFileName() == null ? folderPath.toString() : folderPath.getFileName().toString();
        String cautionText = "Only do this if you trust the site.";
        if (fileCount < 0) {
            return "Upload files to this site?\n"
                    + "This will upload all files from \""
                    + folderName
                    + "\". "
                    + cautionText;
        }

        NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);
        String fileLabel = fileCount == 1 ? "file" : "files";
        return "Upload "
                + numberFormat.format(fileCount)
                + " "
                + fileLabel
                + " to this site?\n"
                + "This will upload all files from \""
                + folderName
                + "\". "
                + cautionText;
    }
}
