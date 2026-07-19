package io.github.trethore.graphene.api.browser;

/** Stable, backend-independent categories for browser frame load failures. */
public enum BrowserLoadFailureReason {
  CANCELED,
  INVALID_REQUEST,
  NOT_FOUND,
  TIMED_OUT,
  ACCESS_DENIED,
  BLOCKED,
  OFFLINE,
  NAME_RESOLUTION_FAILED,
  CONNECTION_FAILED,
  PROXY_FAILED,
  TLS_FAILED,
  CERTIFICATE_FAILED,
  PROTOCOL_FAILED,
  FILE_FAILED,
  CACHE_FAILED,
  RESOURCE_EXHAUSTED,
  UNKNOWN
}
