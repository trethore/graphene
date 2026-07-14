package io.github.trethore.graphene.api.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  void defaultsToTransparentWhiteWithJavaScriptEnabled() {
    BrowserOptions options = BrowserOptions.defaults();

    assertTrue(options.transparent());
    assertEquals(0xFFFFFF, options.backgroundColor());
    assertTrue(options.javascriptEnabled());
  }

  @Test
  void validatesMaximumFrameRate() {
    BrowserOptions.Builder builder = BrowserOptions.builder();

    assertThrows(IllegalArgumentException.class, () -> builder.maximumFrameRate(0));
    assertThrows(IllegalArgumentException.class, () -> builder.maximumFrameRate(61));
    assertEquals(30, builder.maximumFrameRate(30).build().maximumFrameRate());
  }

  @Test
  void validatesRgbBackgroundColor() {
    BrowserOptions options =
        BrowserOptions.builder().transparent(false).backgroundColor(0x123456).build();

    assertFalse(options.transparent());
    assertEquals(0x123456, options.backgroundColor());
    BrowserOptions.Builder builder = BrowserOptions.builder();
    assertThrows(IllegalArgumentException.class, () -> builder.backgroundColor(-1));
    assertThrows(IllegalArgumentException.class, () -> builder.backgroundColor(0x1000000));
    assertEquals(0xFFFFFF, builder.build().backgroundColor());
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
}
