package io.github.trethore.graphene.api.browser.dialog;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Presents browser file dialogs through a consumer-provided user interface. */
@FunctionalInterface
public interface BrowserFileDialogPresenter {
  /**
   * Presents the request asynchronously. An empty selected-path list cancels the dialog; failed or
   * {@code null} completion is also treated as cancellation.
   */
  CompletionStage<List<Path>> show(Request request);

  /** Kind of file selection requested by browser content. */
  enum Mode {
    OPEN_FILE,
    OPEN_MULTIPLE_FILES,
    OPEN_FOLDER,
    SAVE_FILE
  }

  /** Immutable details of a browser file-dialog request. */
  record Request(Mode mode, String title, String defaultFilePath, List<Filter> filters) {
    public Request {
      Objects.requireNonNull(mode, "mode");
      Objects.requireNonNull(title, "title");
      Objects.requireNonNull(defaultFilePath, "defaultFilePath");
      filters = List.copyOf(Objects.requireNonNull(filters, "filters"));
    }
  }

  /** A browser-provided file-type filter. */
  record Filter(String pattern, String extensions, String description) {
    public Filter {
      Objects.requireNonNull(pattern, "pattern");
      Objects.requireNonNull(extensions, "extensions");
      Objects.requireNonNull(description, "description");
    }
  }
}
