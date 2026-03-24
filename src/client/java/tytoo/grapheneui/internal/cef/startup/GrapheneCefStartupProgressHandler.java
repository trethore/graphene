package tytoo.grapheneui.internal.cef.startup;

import io.github.trethore.jcefgithub.EnumProgress;
import io.github.trethore.jcefgithub.IProgressHandler;

import java.util.Objects;

public final class GrapheneCefStartupProgressHandler implements IProgressHandler {
    private final GrapheneNativeDownloadState downloadState;
    private final Runnable downloadStartedAction;
    private boolean downloadStarted;

    public GrapheneCefStartupProgressHandler(GrapheneNativeDownloadState downloadState, Runnable downloadStartedAction) {
        this.downloadState = Objects.requireNonNull(downloadState, "downloadState");
        this.downloadStartedAction = Objects.requireNonNull(downloadStartedAction, "downloadStartedAction");
    }

    @Override
    public void handleProgress(EnumProgress state, float percent) {
        if (state == EnumProgress.DOWNLOADING) {
            downloadState.beginDownload(percent);
            if (!downloadStarted) {
                downloadStarted = true;
                downloadStartedAction.run();
            }
            return;
        }

        if (!downloadStarted) {
            return;
        }

        if (state == EnumProgress.EXTRACTING
                || state == EnumProgress.INSTALL
                || state == EnumProgress.INITIALIZING
                || state == EnumProgress.INITIALIZED) {
            downloadState.markPostDownloadWork();
        }
    }
}
