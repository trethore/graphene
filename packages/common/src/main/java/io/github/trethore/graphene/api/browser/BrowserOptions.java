package io.github.trethore.graphene.api.browser;

import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.api.browser.dialog.BrowserJsDialogPresenter;
import io.github.trethore.graphene.api.browser.navigation.BrowserNavigationPolicy;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public final class BrowserOptions {
  private static final int DEFAULT_FRAME_RATE = 60;
  private static final int DEFAULT_BACKGROUND_COLOR = 0xFFFFFF;
  private static final BrowserOptions DEFAULT = builder().build();

  private final int maximumFrameRate;
  private final boolean transparent;
  private final int backgroundColor;
  private final boolean javascriptEnabled;
  private final BrowserNavigationPolicy navigationPolicy;
  private final BrowserFileDialogPresenter fileDialogPresenter;
  private final BrowserJsDialogPresenter jsDialogPresenter;

  private BrowserOptions(Builder builder) {
    this.maximumFrameRate = requireFrameRate(builder.maximumFrameRate);
    this.transparent = builder.transparent;
    this.backgroundColor = requireBackgroundColor(builder.backgroundColor);
    this.javascriptEnabled = builder.javascriptEnabled;
    this.navigationPolicy = Objects.requireNonNull(builder.navigationPolicy, "navigationPolicy");
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

  public BrowserNavigationPolicy navigationPolicy() {
    return navigationPolicy;
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

  public static final class Builder {
    private int maximumFrameRate = DEFAULT_FRAME_RATE;
    private boolean transparent = true;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private boolean javascriptEnabled = true;
    private BrowserNavigationPolicy navigationPolicy = BrowserNavigationPolicy.defaultPolicy();
    private BrowserFileDialogPresenter fileDialogPresenter;
    private BrowserJsDialogPresenter jsDialogPresenter;

    private Builder() {}

    public Builder maximumFrameRate(int maximumFrameRate) {
      this.maximumFrameRate = requireFrameRate(maximumFrameRate);
      return this;
    }

    public Builder transparent(boolean transparent) {
      this.transparent = transparent;
      return this;
    }

    public Builder backgroundColor(int backgroundColor) {
      this.backgroundColor = requireBackgroundColor(backgroundColor);
      return this;
    }

    public Builder javascriptEnabled(boolean javascriptEnabled) {
      this.javascriptEnabled = javascriptEnabled;
      return this;
    }

    public Builder navigationPolicy(BrowserNavigationPolicy navigationPolicy) {
      this.navigationPolicy = Objects.requireNonNull(navigationPolicy, "navigationPolicy");
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
