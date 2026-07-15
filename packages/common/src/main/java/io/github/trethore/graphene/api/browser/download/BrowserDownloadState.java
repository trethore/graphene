package io.github.trethore.graphene.api.browser.download;

public enum BrowserDownloadState {
  REQUESTED,
  IN_PROGRESS,
  COMPLETED,
  CANCELED,
  /** Includes interruptions and backend failures when JCEF provides no detailed reason. */
  FAILED
}
