package tytoo.grapheneui.internal.browser.title;

public final class GrapheneBrowserTitleState {
    private volatile String currentTitle = "";

    public String currentTitle() {
        return currentTitle;
    }

    public boolean updateTitle(String title) {
        String normalizedTitle = title == null ? "" : title;
        if (currentTitle.equals(normalizedTitle)) {
            return false;
        }

        currentTitle = normalizedTitle;
        return true;
    }
}
