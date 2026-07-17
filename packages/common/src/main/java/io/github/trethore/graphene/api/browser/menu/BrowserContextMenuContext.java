package io.github.trethore.graphene.api.browser.menu;

import io.github.trethore.graphene.api.browser.BrowserSession;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record BrowserContextMenuContext(
    BrowserSession session,
    Position position,
    Set<TargetType> targetTypes,
    Document document,
    Target target,
    Media media,
    String selectionText,
    Editing editing) {
  public BrowserContextMenuContext {
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(position, "position");
    targetTypes = Set.copyOf(Objects.requireNonNull(targetTypes, "targetTypes"));
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(media, "media");
    Objects.requireNonNull(selectionText, "selectionText");
    Objects.requireNonNull(editing, "editing");
  }

  public record Position(int x, int y) {}

  public record Document(String pageUrl, String frameUrl, String frameCharset) {
    public Document {
      Objects.requireNonNull(pageUrl, "pageUrl");
      Objects.requireNonNull(frameUrl, "frameUrl");
      Objects.requireNonNull(frameCharset, "frameCharset");
    }
  }

  public record Target(String linkUrl, String sourceUrl, boolean imageContents) {
    public Target {
      Objects.requireNonNull(linkUrl, "linkUrl");
      Objects.requireNonNull(sourceUrl, "sourceUrl");
    }
  }

  public record Media(MediaType type, Set<MediaState> states) {
    public Media {
      Objects.requireNonNull(type, "type");
      states = Set.copyOf(Objects.requireNonNull(states, "states"));
    }
  }

  public record Editing(
      boolean spellCheckEnabled,
      Set<EditCapability> capabilities,
      String misspelledWord,
      List<String> dictionarySuggestions) {
    public Editing {
      capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities"));
      Objects.requireNonNull(misspelledWord, "misspelledWord");
      dictionarySuggestions =
          List.copyOf(Objects.requireNonNull(dictionarySuggestions, "dictionarySuggestions"));
    }
  }

  public enum TargetType {
    PAGE,
    FRAME,
    LINK,
    MEDIA,
    SELECTION,
    EDITABLE
  }

  public enum MediaType {
    NONE,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    PLUGIN
  }

  public enum MediaState {
    ERROR,
    PAUSED,
    MUTED,
    LOOP,
    CAN_SAVE,
    HAS_AUDIO,
    HAS_VIDEO,
    CONTROL_ROOT_ELEMENT,
    CAN_PRINT,
    CAN_ROTATE
  }

  public enum EditCapability {
    UNDO,
    REDO,
    CUT,
    COPY,
    PASTE,
    DELETE,
    SELECT_ALL,
    TRANSLATE
  }
}
