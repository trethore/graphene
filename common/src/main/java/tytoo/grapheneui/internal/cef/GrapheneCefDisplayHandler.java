package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import tytoo.grapheneui.internal.core.GrapheneMainThreadExecutor;
import tytoo.grapheneui.internal.event.GrapheneTitleEventBus;

import java.util.Objects;

final class GrapheneCefDisplayHandler extends CefDisplayHandlerAdapter {
    private final GrapheneTitleEventBus titleEventBus;
    private final GrapheneMainThreadExecutor mainThreadExecutor;

    GrapheneCefDisplayHandler(GrapheneTitleEventBus titleEventBus) {
        this(titleEventBus, GrapheneMainThreadExecutor.DIRECT);
    }

    GrapheneCefDisplayHandler(GrapheneTitleEventBus titleEventBus, GrapheneMainThreadExecutor mainThreadExecutor) {
        this.titleEventBus = Objects.requireNonNull(titleEventBus, "titleEventBus");
        this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    }

    @Override
    public void onTitleChange(CefBrowser browser, String title) {
        if (!(browser instanceof GrapheneTitleTarget titleTarget)) {
            return;
        }

        mainThreadExecutor.run(() -> {
            if (!titleTarget.updateTitle(title)) {
                return;
            }

            titleEventBus.onTitleChange(titleTarget.browser(), titleTarget.currentTitle());
        });
    }
}
