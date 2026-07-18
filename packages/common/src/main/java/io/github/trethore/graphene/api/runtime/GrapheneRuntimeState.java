package io.github.trethore.graphene.api.runtime;

/** Lifecycle state of the process-wide Graphene runtime. */
public enum GrapheneRuntimeState {
  NEW,
  STARTING,
  RUNNING,
  STOPPING,
  STOPPED,
  FAILED
}
