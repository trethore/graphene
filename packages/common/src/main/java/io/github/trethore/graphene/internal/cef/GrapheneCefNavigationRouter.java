package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.navigation.BrowserNavigationPolicy;
import io.github.trethore.graphene.internal.platform.GrapheneExternalBrowser;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import java.util.Optional;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefWindowOpenDisposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneCefNavigationRouter {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefNavigationRouter.class);

  private final GrapheneTaskExecutor mainThreadExecutor;
  private final GrapheneExternalBrowser externalBrowser;

  GrapheneCefNavigationRouter(
      GrapheneTaskExecutor mainThreadExecutor, GrapheneExternalBrowser externalBrowser) {
    this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    this.externalBrowser = Objects.requireNonNull(externalBrowser, "externalBrowser");
  }

  boolean onMainFrameNavigation(
      CefBrowser browser, CefFrame frame, String targetUrl, boolean userGesture, boolean redirect) {
    if (!(browser instanceof BrowserSession session) || frame == null || !frame.isMain()) {
      return false;
    }
    BrowserNavigationPolicy.Request request =
        request(
            session,
            BrowserNavigationPolicy.Type.MAIN_FRAME_NAVIGATION,
            targetUrl,
            frame,
            "",
            BrowserNavigationPolicy.Disposition.CURRENT_TAB,
            userGesture,
            redirect);
    return apply(session, request, true);
  }

  void onPopup(
      CefBrowser browser,
      CefFrame frame,
      String targetUrl,
      String targetFrameName,
      CefWindowOpenDisposition targetDisposition,
      boolean userGesture) {
    if (!(browser instanceof BrowserSession session)) {
      return;
    }
    BrowserNavigationPolicy.Request request =
        request(
            session,
            BrowserNavigationPolicy.Type.POPUP,
            targetUrl,
            frame,
            targetFrameName,
            disposition(targetDisposition),
            userGesture,
            false);
    applyPopup(session, request);
  }

  boolean onOpenFromTab(
      CefBrowser browser,
      CefFrame frame,
      String targetUrl,
      CefWindowOpenDisposition targetDisposition,
      boolean userGesture) {
    if (!(browser instanceof BrowserSession session)) {
      return false;
    }
    BrowserNavigationPolicy.Request request =
        request(
            session,
            BrowserNavigationPolicy.Type.OPEN_FROM_TAB,
            targetUrl,
            frame,
            "",
            disposition(targetDisposition),
            userGesture,
            false);
    boolean preserveOriginalNavigation = targetDisposition == CefWindowOpenDisposition.CURRENT_TAB;
    return apply(session, request, preserveOriginalNavigation);
  }

  private boolean apply(
      BrowserSession session,
      BrowserNavigationPolicy.Request request,
      boolean preserveOriginalNavigation) {
    BrowserNavigationPolicy.Decision decision = decide(session, request);
    return switch (decision) {
      case CANCEL, CONSUMER_MANAGED -> true;
      case SAME_SESSION -> {
        if (preserveOriginalNavigation) {
          yield false;
        }
        navigate(session, request.targetUrl());
        yield true;
      }
      case EXTERNAL_BROWSER -> {
        openExternally(request.targetUrl());
        yield true;
      }
    };
  }

  private void applyPopup(BrowserSession session, BrowserNavigationPolicy.Request request) {
    BrowserNavigationPolicy.Decision decision = decide(session, request);
    if (decision == BrowserNavigationPolicy.Decision.SAME_SESSION) {
      navigate(session, request.targetUrl());
    } else if (decision == BrowserNavigationPolicy.Decision.EXTERNAL_BROWSER) {
      openExternally(request.targetUrl());
    }
  }

  private static BrowserNavigationPolicy.Decision decide(
      BrowserSession session, BrowserNavigationPolicy.Request request) {
    try {
      BrowserNavigationPolicy.Decision decision =
          session.options().navigationPolicy().decide(request);
      if (decision != null) {
        return decision;
      }
      LOGGER.warn("Navigation policy returned null for {}", request.targetUrl());
    } catch (RuntimeException exception) {
      LOGGER.warn("Navigation policy failed for {}", request.targetUrl(), exception);
    }
    return BrowserNavigationPolicy.Decision.CANCEL;
  }

  private void navigate(BrowserSession session, String targetUrl) {
    if (targetUrl.isBlank()) {
      return;
    }
    schedule(
        "navigate session to",
        targetUrl,
        () -> {
          if (!session.isClosed()) {
            session.navigate(targetUrl);
          }
        });
  }

  private void openExternally(String targetUrl) {
    if (targetUrl.isBlank()) {
      return;
    }
    schedule("open external browser for", targetUrl, () -> externalBrowser.open(targetUrl));
  }

  private void schedule(String operation, String targetUrl, Runnable action) {
    try {
      mainThreadExecutor.execute(
          () -> {
            try {
              action.run();
            } catch (RuntimeException exception) {
              LOGGER.warn("Failed to {} {}", operation, targetUrl, exception);
            }
          });
    } catch (RuntimeException exception) {
      LOGGER.warn("Failed to schedule {} {}", operation, targetUrl, exception);
    }
  }

  private static BrowserNavigationPolicy.Request request(
      BrowserSession session,
      BrowserNavigationPolicy.Type type,
      String targetUrl,
      CefFrame sourceFrame,
      String targetFrameName,
      BrowserNavigationPolicy.Disposition disposition,
      boolean userGesture,
      boolean redirect) {
    return new BrowserNavigationPolicy.Request(
        session,
        type,
        value(targetUrl),
        frame(sourceFrame),
        value(targetFrameName),
        disposition,
        userGesture,
        redirect);
  }

  private static Optional<BrowserNavigationPolicy.Frame> frame(CefFrame frame) {
    if (frame == null) {
      return Optional.empty();
    }
    return Optional.of(
        new BrowserNavigationPolicy.Frame(
            value(frame.getIdentifier()),
            value(frame.getName()),
            value(frame.getURL()),
            frame.isMain()));
  }

  static BrowserNavigationPolicy.Disposition disposition(CefWindowOpenDisposition disposition) {
    if (disposition == null) {
      return BrowserNavigationPolicy.Disposition.UNKNOWN;
    }
    return switch (disposition) {
      case UNKNOWN -> BrowserNavigationPolicy.Disposition.UNKNOWN;
      case CURRENT_TAB -> BrowserNavigationPolicy.Disposition.CURRENT_TAB;
      case SINGLETON_TAB -> BrowserNavigationPolicy.Disposition.SINGLETON_TAB;
      case NEW_FOREGROUND_TAB -> BrowserNavigationPolicy.Disposition.NEW_FOREGROUND_TAB;
      case NEW_BACKGROUND_TAB -> BrowserNavigationPolicy.Disposition.NEW_BACKGROUND_TAB;
      case NEW_POPUP -> BrowserNavigationPolicy.Disposition.NEW_POPUP;
      case NEW_WINDOW -> BrowserNavigationPolicy.Disposition.NEW_WINDOW;
      case SAVE_TO_DISK -> BrowserNavigationPolicy.Disposition.SAVE_TO_DISK;
      case OFF_THE_RECORD -> BrowserNavigationPolicy.Disposition.OFF_THE_RECORD;
      case IGNORE_ACTION -> BrowserNavigationPolicy.Disposition.IGNORE_ACTION;
      case SWITCH_TO_TAB -> BrowserNavigationPolicy.Disposition.SWITCH_TO_TAB;
      case NEW_PICTURE_IN_PICTURE -> BrowserNavigationPolicy.Disposition.NEW_PICTURE_IN_PICTURE;
      case NEW_SPLIT_VIEW -> BrowserNavigationPolicy.Disposition.NEW_SPLIT_VIEW;
    };
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }
}
