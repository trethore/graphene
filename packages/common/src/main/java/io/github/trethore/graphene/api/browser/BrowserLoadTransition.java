package io.github.trethore.graphene.api.browser;

/** Describes the action that initiated a browser frame load. */
public enum BrowserLoadTransition {
  LINK,
  EXPLICIT,
  AUTOMATIC_SUBFRAME,
  USER_INITIATED_SUBFRAME,
  FORM_SUBMISSION,
  RELOAD,
  UNKNOWN
}
