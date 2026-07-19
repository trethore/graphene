package io.github.trethore.graphene.fabric.internal.input;

public final class GrapheneClickCounter {
  private static final long MULTI_CLICK_INTERVAL_MILLIS = 250;

  private int button = -1;
  private int count;
  private long timestamp;

  public int registerClick(int button, boolean consecutive, long timestamp) {
    if (consecutive
        && this.button == button
        && timestamp - this.timestamp < MULTI_CLICK_INTERVAL_MILLIS) {
      count = count == Integer.MAX_VALUE ? count : count + 1;
    } else {
      count = 1;
    }
    this.button = button;
    this.timestamp = timestamp;
    return count;
  }

  public int current(int button) {
    return this.button == button ? count : 1;
  }
}
