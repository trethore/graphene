package tytoo.grapheneui.api.runtime;

@SuppressWarnings("unused")
public interface GrapheneHttpServer {
    boolean isRunning();

    String host();

    int port();

    String baseUrl();
}
