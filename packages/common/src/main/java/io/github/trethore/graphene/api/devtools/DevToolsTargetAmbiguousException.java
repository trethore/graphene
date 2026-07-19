package io.github.trethore.graphene.api.devtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Thrown when multiple remote page targets match a browser session. */
public final class DevToolsTargetAmbiguousException extends IllegalStateException {
  private final String sessionUrl;
  private final String sessionTitle;
  private final ArrayList<DevToolsPageTarget> candidates;

  public DevToolsTargetAmbiguousException(
      String sessionUrl, String sessionTitle, List<DevToolsPageTarget> candidates) {
    super(message(sessionUrl, sessionTitle, candidates));
    this.sessionUrl = Objects.requireNonNull(sessionUrl, "sessionUrl");
    this.sessionTitle = Objects.requireNonNull(sessionTitle, "sessionTitle");
    this.candidates = new ArrayList<>(Objects.requireNonNull(candidates, "candidates"));
    if (this.candidates.size() < 2) {
      throw new IllegalArgumentException("candidates must contain at least two targets");
    }
  }

  public String sessionUrl() {
    return sessionUrl;
  }

  public String sessionTitle() {
    return sessionTitle;
  }

  public List<DevToolsPageTarget> candidates() {
    return Collections.unmodifiableList(candidates);
  }

  private static String message(
      String sessionUrl, String sessionTitle, List<DevToolsPageTarget> candidates) {
    return Objects.requireNonNull(candidates, "candidates").size()
        + " remote DevTools page targets match session URL "
        + Objects.requireNonNull(sessionUrl, "sessionUrl")
        + " and title "
        + Objects.requireNonNull(sessionTitle, "sessionTitle");
  }
}
