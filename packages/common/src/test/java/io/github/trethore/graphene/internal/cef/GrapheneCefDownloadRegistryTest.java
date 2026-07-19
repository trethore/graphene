package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.download.BrowserDownload;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadPolicy;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadState;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrapheneCefDownloadRegistryTest {
  @TempDir Path temporaryDirectory;

  @Test
  void cancelsDownloadsByDefault() {
    GrapheneCefDownloadRegistry registry =
        new GrapheneCefDownloadRegistry(session(), BrowserDownloadPolicy.defaultPolicy());
    List<BrowserDownload> events = new ArrayList<>();
    registry.subscribe(events::add);
    TestBeforeDownloadCallback callback = new TestBeforeDownloadCallback();

    boolean handled = registry.onBeforeDownload(new TestDownloadItem(1), "archive.zip", callback);

    assertFalse(handled);
    assertFalse(callback.continued);
    assertEquals(
        List.of(BrowserDownloadState.REQUESTED, BrowserDownloadState.CANCELED),
        events.stream().map(BrowserDownload::state).toList());
    assertTrue(registry.activeDownloads().isEmpty());
  }

  @Test
  void cancelsWhenThePolicyFails() {
    GrapheneCefDownloadRegistry registry =
        new GrapheneCefDownloadRegistry(
            session(),
            request -> {
              throw new IllegalStateException("policy failure");
            });
    List<BrowserDownload> events = new ArrayList<>();
    registry.subscribe(events::add);
    TestBeforeDownloadCallback callback = new TestBeforeDownloadCallback();

    boolean handled = registry.onBeforeDownload(new TestDownloadItem(2), "archive.zip", callback);

    assertFalse(handled);
    assertFalse(callback.continued);
    assertEquals(BrowserDownloadState.CANCELED, events.getLast().state());
  }

  @Test
  void savesToAnExplicitPathAndExposesCancellation() {
    Path targetPath = temporaryDirectory.resolve("archive.zip");
    GrapheneCefDownloadRegistry registry =
        new GrapheneCefDownloadRegistry(
            session(), request -> BrowserDownloadPolicy.Decision.saveTo(targetPath));
    List<BrowserDownload> events = new ArrayList<>();
    registry.subscribe(events::add);
    TestDownloadItem item = new TestDownloadItem(3);
    TestBeforeDownloadCallback beforeCallback = new TestBeforeDownloadCallback();

    assertTrue(registry.onBeforeDownload(item, "archive.zip", beforeCallback));
    assertEquals(targetPath.toAbsolutePath().normalize().toString(), beforeCallback.path);
    assertFalse(beforeCallback.showDialog);
    assertEquals(
        targetPath.toAbsolutePath().normalize(),
        registry.activeDownloads().getFirst().targetPath().orElseThrow());

    item.inProgress = true;
    item.receivedBytes = 25;
    item.totalBytes = 100;
    item.percentComplete = 25;
    TestDownloadItemCallback updateCallback = new TestDownloadItemCallback();
    registry.onDownloadUpdated(item, updateCallback);

    BrowserDownload progress = events.getLast();
    assertEquals(BrowserDownloadState.IN_PROGRESS, progress.state());
    assertTrue(progress.control().cancel());
    assertFalse(progress.control().cancel());
    assertTrue(updateCallback.canceled);

    item.inProgress = false;
    item.canceled = true;
    registry.onDownloadUpdated(item, updateCallback);

    assertEquals(BrowserDownloadState.CANCELED, events.getLast().state());
    assertTrue(registry.activeDownloads().isEmpty());
  }

  @Test
  void delegatesPromptingToTheCefSaveDialog() {
    GrapheneCefDownloadRegistry registry =
        new GrapheneCefDownloadRegistry(
            session(), request -> BrowserDownloadPolicy.Decision.showSaveDialog());
    TestBeforeDownloadCallback callback = new TestBeforeDownloadCallback();

    assertTrue(registry.onBeforeDownload(new TestDownloadItem(4), "archive.zip", callback));

    assertTrue(callback.continued);
    assertEquals("", callback.path);
    assertTrue(callback.showDialog);
  }

  @Test
  void exposesASafeSuggestedFileName() {
    List<String> suggestedNames = new ArrayList<>();
    GrapheneCefDownloadRegistry registry =
        new GrapheneCefDownloadRegistry(
            session(),
            request -> {
              suggestedNames.add(request.suggestedFileName());
              return BrowserDownloadPolicy.Decision.cancel();
            });

    registry.onBeforeDownload(
        new TestDownloadItem(5), "../bad/name.txt", new TestBeforeDownloadCallback());

    assertEquals(List.of(".._bad_name.txt"), suggestedNames);
  }

  @Test
  void reportsUnknownTerminalOutcomesAsFailed() {
    GrapheneCefDownloadRegistry registry =
        new GrapheneCefDownloadRegistry(
            session(), request -> BrowserDownloadPolicy.Decision.showSaveDialog());
    List<BrowserDownload> events = new ArrayList<>();
    registry.subscribe(events::add);
    TestDownloadItem item = new TestDownloadItem(6);
    registry.onBeforeDownload(item, "archive.zip", new TestBeforeDownloadCallback());

    registry.onDownloadUpdated(item, new TestDownloadItemCallback());

    assertEquals(BrowserDownloadState.FAILED, events.getLast().state());
  }

  @Test
  void reportsCompletedDownloadsAndTheirFinalPath() {
    Path targetPath = temporaryDirectory.resolve("archive.zip");
    GrapheneCefDownloadRegistry registry =
        new GrapheneCefDownloadRegistry(
            session(), request -> BrowserDownloadPolicy.Decision.showSaveDialog());
    List<BrowserDownload> events = new ArrayList<>();
    registry.subscribe(events::add);
    TestDownloadItem item = new TestDownloadItem(7);
    registry.onBeforeDownload(item, "archive.zip", new TestBeforeDownloadCallback());
    item.complete = true;
    item.totalBytes = 100;
    item.receivedBytes = 100;
    item.percentComplete = 100;
    item.fullPath = targetPath.toString();

    registry.onDownloadUpdated(item, new TestDownloadItemCallback());

    BrowserDownload completed = events.getLast();
    assertEquals(BrowserDownloadState.COMPLETED, completed.state());
    assertEquals(targetPath.toAbsolutePath().normalize(), completed.targetPath().orElseThrow());
    assertTrue(registry.activeDownloads().isEmpty());
  }

  @Test
  void closingTheRegistryCancelsActiveDownloads() {
    GrapheneCefDownloadRegistry registry =
        new GrapheneCefDownloadRegistry(
            session(), request -> BrowserDownloadPolicy.Decision.showSaveDialog());
    TestDownloadItem item = new TestDownloadItem(8);
    registry.onBeforeDownload(item, "archive.zip", new TestBeforeDownloadCallback());
    item.inProgress = true;
    TestDownloadItemCallback callback = new TestDownloadItemCallback();
    registry.onDownloadUpdated(item, callback);
    BrowserDownload activeDownload = registry.activeDownloads().getFirst();

    registry.close();

    assertTrue(callback.canceled);
    assertFalse(activeDownload.control().cancel());
    assertTrue(registry.activeDownloads().isEmpty());
  }

  private static BrowserSession session() {
    return (BrowserSession)
        Proxy.newProxyInstance(
            BrowserSession.class.getClassLoader(),
            new Class<?>[] {BrowserSession.class},
            (proxy, method, arguments) -> {
              Class<?> returnType = method.getReturnType();
              if (returnType == boolean.class) {
                return false;
              }
              if (returnType == int.class) {
                return 0;
              }
              return null;
            });
  }

  private static final class TestBeforeDownloadCallback implements CefBeforeDownloadCallback {
    private boolean continued;
    private String path;
    private boolean showDialog;

    @Override
    public void Continue(String path, boolean showDialog) {
      continued = true;
      this.path = path;
      this.showDialog = showDialog;
    }
  }

  private static final class TestDownloadItemCallback implements CefDownloadItemCallback {
    private boolean canceled;

    @Override
    public void cancel() {
      canceled = true;
    }

    @Override
    public void pause() {
      throw new UnsupportedOperationException("Pause is not part of the V1 download API");
    }

    @Override
    public void resume() {
      throw new UnsupportedOperationException("Resume is not part of the V1 download API");
    }
  }

  private static final class TestDownloadItem implements CefDownloadItem {
    private final int id;
    private boolean inProgress;
    private boolean complete;
    private boolean canceled;
    private int percentComplete = -1;
    private long totalBytes = -1;
    private long receivedBytes;
    private String fullPath = "";

    private TestDownloadItem(int id) {
      this.id = id;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public boolean isInProgress() {
      return inProgress;
    }

    @Override
    public boolean isComplete() {
      return complete;
    }

    @Override
    public boolean isCanceled() {
      return canceled;
    }

    @Override
    public long getCurrentSpeed() {
      return 0;
    }

    @Override
    public int getPercentComplete() {
      return percentComplete;
    }

    @Override
    public long getTotalBytes() {
      return totalBytes;
    }

    @Override
    public long getReceivedBytes() {
      return receivedBytes;
    }

    @Override
    public Date getStartTime() {
      return null;
    }

    @Override
    public Date getEndTime() {
      return null;
    }

    @Override
    public String getFullPath() {
      return fullPath;
    }

    @Override
    public int getId() {
      return id;
    }

    @Override
    public String getURL() {
      return "https://example.invalid/archive.zip";
    }

    @Override
    public String getSuggestedFileName() {
      return "archive.zip";
    }

    @Override
    public String getContentDisposition() {
      return "attachment";
    }

    @Override
    public String getMimeType() {
      return "application/zip";
    }
  }
}
