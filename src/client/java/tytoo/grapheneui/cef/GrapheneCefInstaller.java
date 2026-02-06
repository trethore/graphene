package tytoo.grapheneui.cef;

import me.tytoo.jcefgithub.CefAppBuilder;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GrapheneCefInstaller {
    private static final File INSTALLATION_DIR = new File("./jcef");
    private static final List<String> MIRRORS = List.of(
            "https://github.com/trethore/jcefgithub/releases/download/{mvn_version}/jcef-natives-{platform}-{tag}.jar"
    );

    private GrapheneCefInstaller() {
    }

    public static CefAppBuilder createBuilder() {
        CefAppBuilder cefAppBuilder = new CefAppBuilder();
        cefAppBuilder.setInstallDir(INSTALLATION_DIR);
        cefAppBuilder.setMirrors(MIRRORS);
        cefAppBuilder.addJcefArgs("--remote-allow-origins=*");

        try {
            Path cacheDirectory = Files.createDirectories(INSTALLATION_DIR.toPath().resolve("cache"));
            cefAppBuilder.getCefSettings().cache_path = cacheDirectory.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create Graphene CEF cache directory", exception);
        }

        cefAppBuilder.getCefSettings().windowless_rendering_enabled = true;
        cefAppBuilder.getCefSettings().remote_debugging_port = findRandomPort();
        cefAppBuilder.getCefSettings().user_agent_product = "Graphene";

        return cefAppBuilder;
    }

    private static int findRandomPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException _) {
            return 9222;
        }
    }
}
