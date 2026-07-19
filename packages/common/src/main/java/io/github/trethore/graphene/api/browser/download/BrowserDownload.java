package io.github.trethore.graphene.api.browser.download;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** Immutable browser-download state captured at a point in time. */
@SuppressWarnings("unused")
public interface BrowserDownload {
  BrowserDownloadId id();

  BrowserDownloadState state();

  String url();

  String suggestedFileName();

  String mimeType();

  String contentDisposition();

  Optional<Path> targetPath();

  long receivedBytes();

  /** Returns the expected total byte count when reported by the server. */
  OptionalLong totalBytes();

  /** Returns the current transfer speed in bytes per second. */
  long currentSpeed();

  /** Returns completion from {@code 0} through {@code 100} when the total size is known. */
  OptionalInt percentComplete();

  Optional<Instant> startedAt();

  Optional<Instant> endedAt();

  BrowserDownloadControl control();
}
