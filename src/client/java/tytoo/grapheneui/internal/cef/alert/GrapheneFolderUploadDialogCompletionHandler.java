package tytoo.grapheneui.internal.cef.alert;

@FunctionalInterface
interface GrapheneFolderUploadDialogCompletionHandler {
    void complete(GrapheneFolderUploadDialogScreen screen, boolean accepted);
}
