package tytoo.grapheneui.internal.bridge;

public interface GrapheneBridgeBrowser {
    int getIdentifier();

    String currentUrl();

    boolean hasDocument();

    void executeScript(String script, String url);
}
