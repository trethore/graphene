package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.internal.bridge.BridgeQueryCallback;
import java.util.Objects;
import org.cef.callback.CefQueryCallback;

final class GrapheneCefQueryCallbackAdapter implements BridgeQueryCallback {
  private final CefQueryCallback callback;

  GrapheneCefQueryCallbackAdapter(CefQueryCallback callback) {
    this.callback = Objects.requireNonNull(callback, "callback");
  }

  @Override
  public void success(String response) {
    callback.success(response);
  }

  @Override
  public void failure(int errorCode, String errorMessage) {
    callback.failure(errorCode, errorMessage);
  }
}
