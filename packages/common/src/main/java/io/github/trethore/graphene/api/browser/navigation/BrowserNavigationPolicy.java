package io.github.trethore.graphene.api.browser.navigation;

import io.github.trethore.graphene.api.browser.BrowserSession;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface BrowserNavigationPolicy {
  /**
   * Decides how Graphene handles a requested navigation. This method is called synchronously on the
   * browser callback thread and must not block. Exceptions and {@code null} results cancel the
   * request.
   */
  Decision decide(Request request);

  /** Allows ordinary current-session navigation and cancels requests for new browsing contexts. */
  static BrowserNavigationPolicy defaultPolicy() {
    return request ->
        request.type() == Type.MAIN_FRAME_NAVIGATION
                || (request.type() == Type.OPEN_FROM_TAB
                    && request.disposition() == Disposition.CURRENT_TAB)
            ? Decision.SAME_SESSION
            : Decision.CANCEL;
  }

  enum Decision {
    /** Cancels the requested navigation. */
    CANCEL,
    /** Preserves or redirects the navigation into the originating session. */
    SAME_SESSION,
    /** Cancels the navigation and asks the platform to open the target URL externally. */
    EXTERNAL_BROWSER,
    /** Cancels Graphene's default handling after the policy has arranged any follow-up work. */
    CONSUMER_MANAGED
  }

  enum Type {
    MAIN_FRAME_NAVIGATION,
    POPUP,
    OPEN_FROM_TAB
  }

  enum Disposition {
    UNKNOWN,
    CURRENT_TAB,
    SINGLETON_TAB,
    NEW_FOREGROUND_TAB,
    NEW_BACKGROUND_TAB,
    NEW_POPUP,
    NEW_WINDOW,
    SAVE_TO_DISK,
    OFF_THE_RECORD,
    IGNORE_ACTION,
    SWITCH_TO_TAB,
    NEW_PICTURE_IN_PICTURE,
    NEW_SPLIT_VIEW
  }

  /** Immutable source-frame information captured while the navigation callback is valid. */
  record Frame(String identifier, String name, String url, boolean mainFrame) {
    public Frame {
      Objects.requireNonNull(identifier, "identifier");
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(url, "url");
    }
  }

  /**
   * Immutable navigation request. URL and frame-name values may be empty when the page did not
   * provide them.
   */
  record Request(
      BrowserSession session,
      Type type,
      String targetUrl,
      Optional<Frame> sourceFrame,
      String targetFrameName,
      Disposition disposition,
      boolean userGesture,
      boolean redirect) {
    public Request {
      Objects.requireNonNull(session, "session");
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(targetUrl, "targetUrl");
      Objects.requireNonNull(sourceFrame, "sourceFrame");
      Objects.requireNonNull(targetFrameName, "targetFrameName");
      Objects.requireNonNull(disposition, "disposition");
    }
  }
}
