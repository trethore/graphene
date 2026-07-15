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

  OptionalLong totalBytes();

  long currentSpeed();

  OptionalInt percentComplete();

  Optional<Instant> startedAt();

  Optional<Instant> endedAt();

  BrowserDownloadControl control();
}
