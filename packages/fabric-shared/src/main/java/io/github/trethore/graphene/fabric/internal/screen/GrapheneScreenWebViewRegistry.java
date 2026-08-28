package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import java.util.ArrayList;
import java.util.List;

final class GrapheneScreenWebViewRegistry {
    private final List<GrapheneWebViewWidget> widgets = new ArrayList<>();
    private boolean autoClose = true;

    List<GrapheneWebViewWidget> widgets() {
        return widgets;
    }

    void add(GrapheneWebViewWidget widget) {
        widgets.add(widget);
    }

    void remove(GrapheneWebViewWidget widget) {
        widgets.remove(widget);
    }

    boolean autoClose() {
        return autoClose;
    }

    void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }

    void closeAll() {
        if (!autoClose) {
            return;
        }
        List<GrapheneWebViewWidget> widgetsToClose = new ArrayList<>(widgets);
        widgetsToClose.forEach(GrapheneWebViewWidget::close);
        widgets.clear();
    }

    void resizeAll() {
        widgets.forEach(GrapheneWebViewWidget::handleScreenResize);
    }
}
