package io.github.trethore.graphene.api.browser.find;

import java.util.Objects;

/** A non-empty page-text search and its case-matching preference. */
public record BrowserFindQuery(String text, boolean matchCase) {
  public BrowserFindQuery {
    Objects.requireNonNull(text, "text");
    if (text.isEmpty()) {
      throw new IllegalArgumentException("text must not be empty");
    }
  }
}
