package io.github.trethore.graphene.api.browser.download;

import io.github.trethore.graphene.api.browser.BrowserSession;
import java.nio.file.Path;
import java.util.Objects;
import java.util.OptionalLong;

@FunctionalInterface
public interface BrowserDownloadPolicy {
  /**
   * Decides how Graphene handles a requested download. This method is called synchronously on the
   * browser callback thread and must not block. Exceptions and {@code null} results cancel the
   * download.
   */
  Decision decide(Request request);

  /** Cancels all downloads unless the consumer explicitly installs another policy. */
  static BrowserDownloadPolicy defaultPolicy() {
    return request -> Decision.cancel();
  }

  sealed interface Decision permits Cancel, SaveTo, ShowSaveDialog {
    static Decision cancel() {
      return Cancel.INSTANCE;
    }

    static Decision saveTo(Path targetPath) {
      return new SaveTo(targetPath);
    }

    static Decision showSaveDialog() {
      return ShowSaveDialog.INSTANCE;
    }
  }

  /** Rejects the download before Graphene selects a target path. */
  enum Cancel implements Decision {
    INSTANCE
  }

  /** Saves directly to an explicit path without creating missing parent directories. */
  record SaveTo(Path targetPath) implements Decision {
    public SaveTo {
      Path validatedTarget = Objects.requireNonNull(targetPath, "targetPath");
      if (validatedTarget.toString().isBlank() || validatedTarget.getFileName() == null) {
        throw new IllegalArgumentException("targetPath must identify a file");
      }
      targetPath = validatedTarget.toAbsolutePath().normalize();
    }
  }

  /** Delegates target selection and user cancellation to CEF's native Save As dialog. */
  enum ShowSaveDialog implements Decision {
    INSTANCE
  }

  /**
   * Immutable download request metadata. {@code suggestedFileName} is a sanitized single file name
   * without path separators.
   */
  record Request(
      BrowserSession session,
      BrowserDownloadId id,
      String url,
      String suggestedFileName,
      String mimeType,
      String contentDisposition,
      OptionalLong totalBytes) {
    public Request {
      Objects.requireNonNull(session, "session");
      Objects.requireNonNull(id, "id");
      url = Objects.requireNonNullElse(url, "");
      suggestedFileName = Objects.requireNonNullElse(suggestedFileName, "");
      mimeType = Objects.requireNonNullElse(mimeType, "");
      contentDisposition = Objects.requireNonNullElse(contentDisposition, "");
      Objects.requireNonNull(totalBytes, "totalBytes");
      if (totalBytes.isPresent() && totalBytes.getAsLong() < 0) {
        throw new IllegalArgumentException("totalBytes must not be negative");
      }
    }
  }
}
