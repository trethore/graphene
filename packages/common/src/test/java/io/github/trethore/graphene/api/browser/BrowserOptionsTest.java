package io.github.trethore.graphene.api.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.api.browser.dialog.BrowserJsDialogPresenter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class BrowserOptionsTest {
  @Test
  void validatesMaximumFrameRate() {
    assertThrows(IllegalArgumentException.class, () -> optionsWithMaximumFrameRate(0));
    assertThrows(IllegalArgumentException.class, () -> optionsWithMaximumFrameRate(61));
    assertEquals(30, BrowserOptions.builder().maximumFrameRate(30).build().maximumFrameRate());
  }

  @Test
  void configuresDialogPresenters() {
    BrowserFileDialogPresenter filePresenter =
        request -> CompletableFuture.completedFuture(List.of(Path.of("selected.txt")));
    BrowserJsDialogPresenter jsPresenter =
        request -> CompletableFuture.completedFuture(BrowserJsDialogPresenter.Result.accept());

    BrowserOptions options =
        BrowserOptions.builder()
            .fileDialogPresenter(filePresenter)
            .jsDialogPresenter(jsPresenter)
            .build();

    assertSame(filePresenter, options.fileDialogPresenter().orElseThrow());
    assertSame(jsPresenter, options.jsDialogPresenter().orElseThrow());
    assertTrue(BrowserOptions.defaults().fileDialogPresenter().isEmpty());
    assertTrue(BrowserOptions.defaults().jsDialogPresenter().isEmpty());
  }

  private static BrowserOptions optionsWithMaximumFrameRate(int maximumFrameRate) {
    return BrowserOptions.builder().maximumFrameRate(maximumFrameRate).build();
  }
}
