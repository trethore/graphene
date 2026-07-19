package io.github.trethore.graphene.internal.bridge;

public interface BridgeQueryCallback {
  void success(String response);

  void failure(int errorCode, String errorMessage);
}
