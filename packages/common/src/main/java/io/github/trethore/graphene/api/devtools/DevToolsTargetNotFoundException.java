package io.github.trethore.graphene.api.devtools;

import java.util.Objects;

/** Thrown when no remote page target matches a browser session. */
public final class DevToolsTargetNotFoundException extends IllegalStateException {
  private final String sessionUrl;
  private final String sessionTitle;

  public DevToolsTargetNotFoundException(String sessionUrl, String sessionTitle) {
    super(message(sessionUrl, sessionTitle));
    this.sessionUrl = Objects.requireNonNull(sessionUrl, "sessionUrl");
    this.sessionTitle = Objects.requireNonNull(sessionTitle, "sessionTitle");
  }

  public String sessionUrl() {
    return sessionUrl;
  }

  public String sessionTitle() {
    return sessionTitle;
  }

  private static String message(String sessionUrl, String sessionTitle) {
    return "No remote DevTools page target matches session URL "
        + Objects.requireNonNull(sessionUrl, "sessionUrl")
        + " and title "
        + Objects.requireNonNull(sessionTitle, "sessionTitle");
  }
}
