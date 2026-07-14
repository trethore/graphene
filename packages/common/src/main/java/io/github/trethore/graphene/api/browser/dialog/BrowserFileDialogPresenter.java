package io.github.trethore.graphene.api.browser.dialog;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface BrowserFileDialogPresenter {
  CompletionStage<List<Path>> show(Request request);

  enum Mode {
    OPEN_FILE,
    OPEN_MULTIPLE_FILES,
    OPEN_FOLDER,
    SAVE_FILE
  }

  record Request(Mode mode, String title, String defaultFilePath, List<Filter> filters) {
    public Request {
      Objects.requireNonNull(mode, "mode");
      Objects.requireNonNull(title, "title");
      Objects.requireNonNull(defaultFilePath, "defaultFilePath");
      filters = List.copyOf(Objects.requireNonNull(filters, "filters"));
    }
  }

  record Filter(String pattern, String extensions, String description) {
    public Filter {
      Objects.requireNonNull(pattern, "pattern");
      Objects.requireNonNull(extensions, "extensions");
      Objects.requireNonNull(description, "description");
    }
  }
}
