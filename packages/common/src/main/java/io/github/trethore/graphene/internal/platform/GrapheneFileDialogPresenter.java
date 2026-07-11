package io.github.trethore.graphene.internal.platform;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface GrapheneFileDialogPresenter {
  CompletionStage<List<Path>> show(boolean foldersOnly, boolean multiple);
}
