package io.github.trethore.graphene.fabric.internal.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TinyFdFileDialogFilterTest {
  @Test
  void flattensExpandedExtensionsAndDescriptions() {
    TinyFdFileDialogFilter filter =
        TinyFdFileDialogFilter.from(
            List.of(
                new BrowserFileDialogPresenter.Filter("text/plain", ".txt;.text", "Text files"),
                new BrowserFileDialogPresenter.Filter("image/*", ".png;.jpg;.png", "Image files")));

    assertEquals(List.of("*.txt", "*.text", "*.png", "*.jpg"), filter.patterns());
    assertEquals("Supported files", filter.description());
  }

  @Test
  void usesCombinedFilterPatternAsFallback() {
    TinyFdFileDialogFilter filter =
        TinyFdFileDialogFilter.from(
            List.of(new BrowserFileDialogPresenter.Filter("Image files|.png;*.gif", "", "")));

    assertEquals(List.of("*.png", "*.gif"), filter.patterns());
    assertEquals("Image files", filter.description());
  }

  @Test
  void ignoresMimeTypesWithoutExpandedExtensions() {
    TinyFdFileDialogFilter filter =
        TinyFdFileDialogFilter.from(
            List.of(new BrowserFileDialogPresenter.Filter("image/*", "", "Images")));

    assertEquals(List.of(), filter.patterns());
    assertEquals("", filter.description());
  }

  @Test
  void preservesDescriptionFromDuplicatePattern() {
    TinyFdFileDialogFilter filter =
        TinyFdFileDialogFilter.from(
            List.of(
                new BrowserFileDialogPresenter.Filter(".png", "", ""),
                new BrowserFileDialogPresenter.Filter(".png", "", "PNG files")));

    assertEquals(List.of("*.png"), filter.patterns());
    assertEquals("PNG files", filter.description());
  }

  @Test
  void normalizesDirectExtensionFilters() {
    TinyFdFileDialogFilter filter =
        TinyFdFileDialogFilter.from(
            List.of(
                new BrowserFileDialogPresenter.Filter(".log", "", "Logs"),
                new BrowserFileDialogPresenter.Filter("json", "", "JSON"),
                new BrowserFileDialogPresenter.Filter("*.*", "", "All files")));

    assertEquals(List.of("*.log", "*.json", "*.*"), filter.patterns());
    assertEquals("Supported files", filter.description());
  }
}
