package io.github.trethore.graphene.api.browser.menu;

import io.github.trethore.graphene.api.browser.BrowserSession;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable browser and document state captured for a context-menu request. */
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

  /** Browser viewport position at which the context menu was requested. */
  public record Position(int x, int y) {}

  /** Document and frame metadata for the context-menu request. */
  public record Document(String pageUrl, String frameUrl, String frameCharset) {
    public Document {
      Objects.requireNonNull(pageUrl, "pageUrl");
      Objects.requireNonNull(frameUrl, "frameUrl");
      Objects.requireNonNull(frameCharset, "frameCharset");
    }
  }

  /** Link or resource targeted by the context-menu request. */
  public record Target(String linkUrl, String sourceUrl, boolean imageContents) {
    public Target {
      Objects.requireNonNull(linkUrl, "linkUrl");
      Objects.requireNonNull(sourceUrl, "sourceUrl");
    }
  }

  /** Media type and state targeted by the context-menu request. */
  public record Media(MediaType type, Set<MediaState> states) {
    public Media {
      Objects.requireNonNull(type, "type");
      states = Set.copyOf(Objects.requireNonNull(states, "states"));
    }
  }

  /** Editing state and available actions at the requested position. */
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

  /** Kind of content targeted by the context-menu request. */
  public enum TargetType {
    PAGE,
    FRAME,
    LINK,
    MEDIA,
    SELECTION,
    EDITABLE
  }

  /** Kind of media targeted by the context-menu request. */
  public enum MediaType {
    NONE,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    PLUGIN
  }

  /** State or capability reported for targeted media. */
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

  /** Editing operation available at the requested position. */
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
