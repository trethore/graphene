package io.github.trethore.graphene.api.browser;

import io.github.trethore.graphene.api.browser.bridge.BrowserBridgePolicy;
import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.api.browser.dialog.BrowserJsDialogPresenter;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadPolicy;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPolicy;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import io.github.trethore.graphene.api.browser.navigation.BrowserNavigationPolicy;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable options controlling browser behavior, policies, and presentation hooks. Defaults use a
 * transparent 60 FPS browser with JavaScript enabled, expose the bridge only to Graphene-owned
 * documents, cancel downloads, allow ordinary same-session navigation, and disable context menus.
 */
@SuppressWarnings("unused")
public final class BrowserOptions {
  private static final int DEFAULT_FRAME_RATE = 60;
  private static final int DEFAULT_BACKGROUND_COLOR = 0xFFFFFF;
  private static final BrowserOptions DEFAULT = builder().build();

  private final int maximumFrameRate;
  private final boolean transparent;
  private final int backgroundColor;
  private final boolean javascriptEnabled;
  private final BrowserBridgePolicy bridgePolicy;
  private final BrowserNavigationPolicy navigationPolicy;
  private final BrowserDownloadPolicy downloadPolicy;
  private final BrowserContextMenuPolicy contextMenuPolicy;
  private final BrowserContextMenuPresenter contextMenuPresenter;
  private final BrowserFileDialogPresenter fileDialogPresenter;
  private final BrowserJsDialogPresenter jsDialogPresenter;

  private BrowserOptions(Builder builder) {
    this.maximumFrameRate = requireFrameRate(builder.maximumFrameRate);
    this.transparent = builder.transparent;
    this.backgroundColor = requireBackgroundColor(builder.backgroundColor);
    this.javascriptEnabled = builder.javascriptEnabled;
    this.bridgePolicy = Objects.requireNonNull(builder.bridgePolicy, "bridgePolicy");
    this.navigationPolicy = Objects.requireNonNull(builder.navigationPolicy, "navigationPolicy");
    this.downloadPolicy = Objects.requireNonNull(builder.downloadPolicy, "downloadPolicy");
    this.contextMenuPolicy = Objects.requireNonNull(builder.contextMenuPolicy, "contextMenuPolicy");
    this.contextMenuPresenter = builder.contextMenuPresenter;
    this.fileDialogPresenter = builder.fileDialogPresenter;
    this.jsDialogPresenter = builder.jsDialogPresenter;
  }

  public static BrowserOptions defaults() {
    return DEFAULT;
  }

  public static Builder builder() {
    return new Builder();
  }

  public int maximumFrameRate() {
    return maximumFrameRate;
  }

  public boolean transparent() {
    return transparent;
  }

  public int backgroundColor() {
    return backgroundColor;
  }

  public boolean javascriptEnabled() {
    return javascriptEnabled;
  }

  public BrowserBridgePolicy bridgePolicy() {
    return bridgePolicy;
  }

  public BrowserNavigationPolicy navigationPolicy() {
    return navigationPolicy;
  }

  public BrowserDownloadPolicy downloadPolicy() {
    return downloadPolicy;
  }

  public BrowserContextMenuPolicy contextMenuPolicy() {
    return contextMenuPolicy;
  }

  public Optional<BrowserContextMenuPresenter> contextMenuPresenter() {
    return Optional.ofNullable(contextMenuPresenter);
  }

  public Optional<BrowserFileDialogPresenter> fileDialogPresenter() {
    return Optional.ofNullable(fileDialogPresenter);
  }

  public Optional<BrowserJsDialogPresenter> jsDialogPresenter() {
    return Optional.ofNullable(jsDialogPresenter);
  }

  private static int requireFrameRate(int frameRate) {
    if (frameRate < 1 || frameRate > 60) {
      throw new IllegalArgumentException("maximumFrameRate must be between 1 and 60");
    }
    return frameRate;
  }

  private static int requireBackgroundColor(int backgroundColor) {
    if (backgroundColor < 0 || backgroundColor > 0xFFFFFF) {
      throw new IllegalArgumentException("backgroundColor must be a 24-bit RGB value");
    }
    return backgroundColor;
  }

  /** Builds immutable browser options. */
  public static final class Builder {
    private int maximumFrameRate = DEFAULT_FRAME_RATE;
    private boolean transparent = true;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private boolean javascriptEnabled = true;
    private BrowserBridgePolicy bridgePolicy = BrowserBridgePolicy.defaultPolicy();
    private BrowserNavigationPolicy navigationPolicy = BrowserNavigationPolicy.defaultPolicy();
    private BrowserDownloadPolicy downloadPolicy = BrowserDownloadPolicy.defaultPolicy();
    private BrowserContextMenuPolicy contextMenuPolicy = BrowserContextMenuPolicy.disabled();
    private BrowserContextMenuPresenter contextMenuPresenter;
    private BrowserFileDialogPresenter fileDialogPresenter;
    private BrowserJsDialogPresenter jsDialogPresenter;

    private Builder() {}

    /** Sets the maximum off-screen frame rate from {@code 1} through {@code 60}. */
    public Builder maximumFrameRate(int maximumFrameRate) {
      this.maximumFrameRate = requireFrameRate(maximumFrameRate);
      return this;
    }

    public Builder transparent(boolean transparent) {
      this.transparent = transparent;
      return this;
    }

    /** Sets the opaque background color as a 24-bit {@code 0xRRGGBB} value. */
    public Builder backgroundColor(int backgroundColor) {
      this.backgroundColor = requireBackgroundColor(backgroundColor);
      return this;
    }

    public Builder javascriptEnabled(boolean javascriptEnabled) {
      this.javascriptEnabled = javascriptEnabled;
      return this;
    }

    public Builder bridgePolicy(BrowserBridgePolicy bridgePolicy) {
      this.bridgePolicy = Objects.requireNonNull(bridgePolicy, "bridgePolicy");
      return this;
    }

    public Builder navigationPolicy(BrowserNavigationPolicy navigationPolicy) {
      this.navigationPolicy = Objects.requireNonNull(navigationPolicy, "navigationPolicy");
      return this;
    }

    public Builder downloadPolicy(BrowserDownloadPolicy downloadPolicy) {
      this.downloadPolicy = Objects.requireNonNull(downloadPolicy, "downloadPolicy");
      return this;
    }

    public Builder contextMenuPolicy(BrowserContextMenuPolicy contextMenuPolicy) {
      this.contextMenuPolicy = Objects.requireNonNull(contextMenuPolicy, "contextMenuPolicy");
      return this;
    }

    public Builder contextMenuPresenter(BrowserContextMenuPresenter contextMenuPresenter) {
      this.contextMenuPresenter =
          Objects.requireNonNull(contextMenuPresenter, "contextMenuPresenter");
      return this;
    }

    public Builder fileDialogPresenter(BrowserFileDialogPresenter fileDialogPresenter) {
      this.fileDialogPresenter = Objects.requireNonNull(fileDialogPresenter, "fileDialogPresenter");
      return this;
    }

    public Builder jsDialogPresenter(BrowserJsDialogPresenter jsDialogPresenter) {
      this.jsDialogPresenter = Objects.requireNonNull(jsDialogPresenter, "jsDialogPresenter");
      return this;
    }

    public BrowserOptions build() {
      return new BrowserOptions(this);
    }
  }
}
