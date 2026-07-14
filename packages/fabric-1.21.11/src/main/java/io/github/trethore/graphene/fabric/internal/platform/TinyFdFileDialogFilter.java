package io.github.trethore.graphene.fabric.internal.platform;

import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

record TinyFdFileDialogFilter(List<String> patterns, String description) {
  private static final String DEFAULT_DESCRIPTION = "Supported files";

  TinyFdFileDialogFilter {
    patterns = List.copyOf(Objects.requireNonNull(patterns, "patterns"));
    description = Objects.requireNonNull(description, "description");
  }

  static TinyFdFileDialogFilter from(List<BrowserFileDialogPresenter.Filter> filters) {
    Set<String> patterns = new LinkedHashSet<>();
    Set<String> descriptions = new LinkedHashSet<>();
    for (BrowserFileDialogPresenter.Filter filter : Objects.requireNonNull(filters, "filters")) {
      if (addPatterns(patterns, filter)) {
        String description = resolveDescription(filter);
        if (!description.isBlank()) {
          descriptions.add(description);
        }
      }
    }
    if (patterns.isEmpty()) {
      return new TinyFdFileDialogFilter(List.of(), "");
    }
    String description =
        descriptions.size() == 1 ? descriptions.iterator().next() : DEFAULT_DESCRIPTION;
    return new TinyFdFileDialogFilter(List.copyOf(patterns), description);
  }

  private static boolean addPatterns(
      Set<String> patterns, BrowserFileDialogPresenter.Filter filter) {
    String extensionList = filter.extensions();
    if (extensionList.isBlank()) {
      extensionList = resolveExtensions(filter.pattern());
    }
    boolean hasValidPattern = false;
    for (String extension : extensionList.split(";")) {
      String pattern = normalizePattern(extension);
      if (pattern != null) {
        patterns.add(pattern);
        hasValidPattern = true;
      }
    }
    return hasValidPattern;
  }

  private static String resolveExtensions(String pattern) {
    int descriptionDelimiter = pattern.indexOf('|');
    return descriptionDelimiter < 0 ? pattern : pattern.substring(descriptionDelimiter + 1);
  }

  private static String resolveDescription(BrowserFileDialogPresenter.Filter filter) {
    if (!filter.description().isBlank()) {
      return filter.description().trim();
    }
    int descriptionDelimiter = filter.pattern().indexOf('|');
    return descriptionDelimiter < 0
        ? ""
        : filter.pattern().substring(0, descriptionDelimiter).trim();
  }

  private static String normalizePattern(String extension) {
    String normalizedExtension = extension.trim();
    if (normalizedExtension.isEmpty() || normalizedExtension.indexOf('/') >= 0) {
      return null;
    }
    if (normalizedExtension.indexOf('*') >= 0) {
      return normalizedExtension;
    }
    return normalizedExtension.startsWith(".")
        ? "*" + normalizedExtension
        : "*." + normalizedExtension;
  }
}
