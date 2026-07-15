package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.download.BrowserDownload;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadControl;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadId;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadState;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

record GrapheneCefDownloadSnapshot(
    Metadata metadata,
    BrowserDownloadState state,
    Path target,
    Progress progress,
    BrowserDownloadControl control)
    implements BrowserDownload {
  @Override
  public BrowserDownloadId id() {
    return metadata.id();
  }

  @Override
  public String url() {
    return metadata.url();
  }

  @Override
  public String suggestedFileName() {
    return metadata.suggestedFileName();
  }

  @Override
  public String mimeType() {
    return metadata.mimeType();
  }

  @Override
  public String contentDisposition() {
    return metadata.contentDisposition();
  }

  @Override
  public Optional<Path> targetPath() {
    return Optional.ofNullable(target);
  }

  @Override
  public long receivedBytes() {
    return progress.receivedBytes();
  }

  @Override
  public OptionalLong totalBytes() {
    long value = progress.totalBytes();
    return value < 0 ? OptionalLong.empty() : OptionalLong.of(value);
  }

  @Override
  public long currentSpeed() {
    return progress.currentSpeed();
  }

  @Override
  public OptionalInt percentComplete() {
    int value = progress.percentComplete();
    return value < 0 || value > 100 ? OptionalInt.empty() : OptionalInt.of(value);
  }

  @Override
  public Optional<Instant> startedAt() {
    return Optional.ofNullable(progress.startedAt());
  }

  @Override
  public Optional<Instant> endedAt() {
    return Optional.ofNullable(progress.endedAt());
  }

  record Metadata(
      BrowserDownloadId id,
      String url,
      String suggestedFileName,
      String mimeType,
      String contentDisposition) {}

  record Progress(
      long receivedBytes,
      long totalBytes,
      long currentSpeed,
      int percentComplete,
      Instant startedAt,
      Instant endedAt) {}
}
