package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuAction;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuContext;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;

final class GrapheneCefContextMenuMapper {
  private GrapheneCefContextMenuMapper() {}

  @SuppressWarnings("java:S1149")
  static BrowserContextMenuContext context(BrowserSession session, CefContextMenuParams params) {
    Vector<String> suggestions = new Vector<>();
    params.getDictionarySuggestions(suggestions);
    return new BrowserContextMenuContext(
        session,
        new BrowserContextMenuContext.Position(params.getXCoord(), params.getYCoord()),
        targetTypes(params.getTypeFlags()),
        new BrowserContextMenuContext.Document(
            string(params.getPageUrl()),
            string(params.getFrameUrl()),
            string(params.getFrameCharset())),
        new BrowserContextMenuContext.Target(
            string(params.getLinkUrl()), string(params.getSourceUrl()), params.hasImageContents()),
        new BrowserContextMenuContext.Media(
            mediaType(params.getMediaType()), mediaStates(params.getMediaStateFlags())),
        string(params.getSelectionText()),
        new BrowserContextMenuContext.Editing(
            params.isSpellCheckEnabled(),
            editCapabilities(params.getEditStateFlags()),
            string(params.getMisspelledWord()),
            suggestions.stream().map(GrapheneCefContextMenuMapper::string).toList()));
  }

  static BrowserContextMenuAction action(int commandId) {
    return switch (commandId) {
      case CefMenuModel.MenuId.MENU_ID_BACK -> BrowserContextMenuAction.BACK;
      case CefMenuModel.MenuId.MENU_ID_FORWARD -> BrowserContextMenuAction.FORWARD;
      case CefMenuModel.MenuId.MENU_ID_RELOAD -> BrowserContextMenuAction.RELOAD;
      case CefMenuModel.MenuId.MENU_ID_RELOAD_NOCACHE ->
          BrowserContextMenuAction.RELOAD_WITHOUT_CACHE;
      case CefMenuModel.MenuId.MENU_ID_STOPLOAD -> BrowserContextMenuAction.STOP_LOADING;
      case CefMenuModel.MenuId.MENU_ID_UNDO -> BrowserContextMenuAction.UNDO;
      case CefMenuModel.MenuId.MENU_ID_REDO -> BrowserContextMenuAction.REDO;
      case CefMenuModel.MenuId.MENU_ID_CUT -> BrowserContextMenuAction.CUT;
      case CefMenuModel.MenuId.MENU_ID_COPY -> BrowserContextMenuAction.COPY;
      case CefMenuModel.MenuId.MENU_ID_PASTE -> BrowserContextMenuAction.PASTE;
      case CefMenuModel.MenuId.MENU_ID_DELETE -> BrowserContextMenuAction.DELETE;
      case CefMenuModel.MenuId.MENU_ID_SELECT_ALL -> BrowserContextMenuAction.SELECT_ALL;
      case CefMenuModel.MenuId.MENU_ID_FIND -> BrowserContextMenuAction.FIND;
      case CefMenuModel.MenuId.MENU_ID_PRINT -> BrowserContextMenuAction.PRINT;
      case CefMenuModel.MenuId.MENU_ID_VIEW_SOURCE -> BrowserContextMenuAction.VIEW_SOURCE;
      default ->
          commandId >= CefMenuModel.MenuId.MENU_ID_SPELLCHECK_SUGGESTION_0
                  && commandId <= CefMenuModel.MenuId.MENU_ID_SPELLCHECK_SUGGESTION_LAST
              ? BrowserContextMenuAction.SPELLING_SUGGESTION
              : BrowserContextMenuAction.OTHER;
    };
  }

  static String label(String value) {
    String label = string(value);
    StringBuilder normalized = new StringBuilder(label.length());
    boolean pendingMnemonic = false;
    for (int index = 0; index < label.length(); index++) {
      char character = label.charAt(index);
      if (character == '&') {
        if (pendingMnemonic) {
          normalized.append(character);
          pendingMnemonic = false;
        } else {
          pendingMnemonic = true;
        }
      } else {
        normalized.append(character);
        pendingMnemonic = false;
      }
    }
    return normalized.toString();
  }

  private static Set<BrowserContextMenuContext.TargetType> targetTypes(int flags) {
    EnumSet<BrowserContextMenuContext.TargetType> types =
        EnumSet.noneOf(BrowserContextMenuContext.TargetType.class);
    addFlag(
        types,
        flags,
        CefContextMenuParams.TypeFlags.CM_TYPEFLAG_PAGE,
        BrowserContextMenuContext.TargetType.PAGE);
    addFlag(
        types,
        flags,
        CefContextMenuParams.TypeFlags.CM_TYPEFLAG_FRAME,
        BrowserContextMenuContext.TargetType.FRAME);
    addFlag(
        types,
        flags,
        CefContextMenuParams.TypeFlags.CM_TYPEFLAG_LINK,
        BrowserContextMenuContext.TargetType.LINK);
    addFlag(
        types,
        flags,
        CefContextMenuParams.TypeFlags.CM_TYPEFLAG_MEDIA,
        BrowserContextMenuContext.TargetType.MEDIA);
    addFlag(
        types,
        flags,
        CefContextMenuParams.TypeFlags.CM_TYPEFLAG_SELECTION,
        BrowserContextMenuContext.TargetType.SELECTION);
    addFlag(
        types,
        flags,
        CefContextMenuParams.TypeFlags.CM_TYPEFLAG_EDITABLE,
        BrowserContextMenuContext.TargetType.EDITABLE);
    return Set.copyOf(types);
  }

  private static Set<BrowserContextMenuContext.MediaState> mediaStates(int flags) {
    EnumSet<BrowserContextMenuContext.MediaState> states =
        EnumSet.noneOf(BrowserContextMenuContext.MediaState.class);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_ERROR,
        BrowserContextMenuContext.MediaState.ERROR);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_PAUSED,
        BrowserContextMenuContext.MediaState.PAUSED);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_MUTED,
        BrowserContextMenuContext.MediaState.MUTED);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_LOOP,
        BrowserContextMenuContext.MediaState.LOOP);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_CAN_SAVE,
        BrowserContextMenuContext.MediaState.CAN_SAVE);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_HAS_AUDIO,
        BrowserContextMenuContext.MediaState.HAS_AUDIO);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_HAS_VIDEO,
        BrowserContextMenuContext.MediaState.HAS_VIDEO);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_CONTROL_ROOT_ELEMENT,
        BrowserContextMenuContext.MediaState.CONTROL_ROOT_ELEMENT);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_CAN_PRINT,
        BrowserContextMenuContext.MediaState.CAN_PRINT);
    addFlag(
        states,
        flags,
        CefContextMenuParams.MediaStateFlags.CM_MEDIAFLAG_CAN_ROTATE,
        BrowserContextMenuContext.MediaState.CAN_ROTATE);
    return Set.copyOf(states);
  }

  private static Set<BrowserContextMenuContext.EditCapability> editCapabilities(int flags) {
    EnumSet<BrowserContextMenuContext.EditCapability> capabilities =
        EnumSet.noneOf(BrowserContextMenuContext.EditCapability.class);
    addFlag(
        capabilities,
        flags,
        CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_UNDO,
        BrowserContextMenuContext.EditCapability.UNDO);
    addFlag(
        capabilities,
        flags,
        CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_REDO,
        BrowserContextMenuContext.EditCapability.REDO);
    addFlag(
        capabilities,
        flags,
        CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_CUT,
        BrowserContextMenuContext.EditCapability.CUT);
    addFlag(
        capabilities,
        flags,
        CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_COPY,
        BrowserContextMenuContext.EditCapability.COPY);
    addFlag(
        capabilities,
        flags,
        CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_PASTE,
        BrowserContextMenuContext.EditCapability.PASTE);
    addFlag(
        capabilities,
        flags,
        CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_DELETE,
        BrowserContextMenuContext.EditCapability.DELETE);
    addFlag(
        capabilities,
        flags,
        CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_SELECT_ALL,
        BrowserContextMenuContext.EditCapability.SELECT_ALL);
    addFlag(
        capabilities,
        flags,
        CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_TRANSLATE,
        BrowserContextMenuContext.EditCapability.TRANSLATE);
    return Set.copyOf(capabilities);
  }

  private static BrowserContextMenuContext.MediaType mediaType(
      CefContextMenuParams.MediaType mediaType) {
    if (mediaType == null) {
      return BrowserContextMenuContext.MediaType.NONE;
    }
    return switch (mediaType) {
      case CM_MEDIATYPE_NONE -> BrowserContextMenuContext.MediaType.NONE;
      case CM_MEDIATYPE_IMAGE -> BrowserContextMenuContext.MediaType.IMAGE;
      case CM_MEDIATYPE_VIDEO -> BrowserContextMenuContext.MediaType.VIDEO;
      case CM_MEDIATYPE_AUDIO -> BrowserContextMenuContext.MediaType.AUDIO;
      case CM_MEDIATYPE_FILE -> BrowserContextMenuContext.MediaType.FILE;
      case CM_MEDIATYPE_PLUGIN -> BrowserContextMenuContext.MediaType.PLUGIN;
    };
  }

  private static <T> void addFlag(Set<T> values, int flags, int flag, T value) {
    if ((flags & flag) != 0) {
      values.add(value);
    }
  }

  private static String string(String value) {
    return Objects.requireNonNullElse(value, "");
  }
}
