package tytoo.grapheneui.internal.cef.alert;

@FunctionalInterface
interface GrapheneJsDialogCompletionHandler {
    void complete(GrapheneJsDialogScreen screen, boolean accepted, String value);
}
