package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserLoadFailureReason;
import io.github.trethore.graphene.api.browser.BrowserLoadTransition;
import java.util.Map;
import java.util.Set;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.Test;

class GrapheneCefLoadEventMapperTest {
  @Test
  void mapsEveryTransitionType() {
    Map<CefRequest.TransitionType, BrowserLoadTransition> expected =
        Map.of(
            CefRequest.TransitionType.TT_LINK,
            BrowserLoadTransition.LINK,
            CefRequest.TransitionType.TT_EXPLICIT,
            BrowserLoadTransition.EXPLICIT,
            CefRequest.TransitionType.TT_AUTO_SUBFRAME,
            BrowserLoadTransition.AUTOMATIC_SUBFRAME,
            CefRequest.TransitionType.TT_MANUAL_SUBFRAME,
            BrowserLoadTransition.USER_INITIATED_SUBFRAME,
            CefRequest.TransitionType.TT_FORM_SUBMIT,
            BrowserLoadTransition.FORM_SUBMISSION,
            CefRequest.TransitionType.TT_RELOAD,
            BrowserLoadTransition.RELOAD);

    for (CefRequest.TransitionType transitionType : CefRequest.TransitionType.values()) {
      assertEquals(
          expected.get(transitionType), GrapheneCefLoadEventMapper.transition(transitionType));
    }
    assertEquals(BrowserLoadTransition.UNKNOWN, GrapheneCefLoadEventMapper.transition(null));
  }

  @Test
  void mapsRepresentativeFailureReasons() {
    assertReason(CefLoadHandler.ErrorCode.ERR_ABORTED, BrowserLoadFailureReason.CANCELED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_INVALID_URL, BrowserLoadFailureReason.INVALID_REQUEST);
    assertReason(CefLoadHandler.ErrorCode.ERR_FILE_NOT_FOUND, BrowserLoadFailureReason.NOT_FOUND);
    assertReason(CefLoadHandler.ErrorCode.ERR_TIMED_OUT, BrowserLoadFailureReason.TIMED_OUT);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_ACCESS_DENIED, BrowserLoadFailureReason.ACCESS_DENIED);
    assertReason(CefLoadHandler.ErrorCode.ERR_BLOCKED_BY_CLIENT, BrowserLoadFailureReason.BLOCKED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_INTERNET_DISCONNECTED, BrowserLoadFailureReason.OFFLINE);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_NAME_NOT_RESOLVED,
        BrowserLoadFailureReason.NAME_RESOLUTION_FAILED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_CONNECTION_REFUSED,
        BrowserLoadFailureReason.CONNECTION_FAILED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_PROXY_CONNECTION_FAILED,
        BrowserLoadFailureReason.PROXY_FAILED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_SSL_PROTOCOL_ERROR, BrowserLoadFailureReason.TLS_FAILED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_CERT_DATE_INVALID,
        BrowserLoadFailureReason.CERTIFICATE_FAILED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_HTTP2_PROTOCOL_ERROR,
        BrowserLoadFailureReason.PROTOCOL_FAILED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_UPLOAD_FILE_CHANGED, BrowserLoadFailureReason.FILE_FAILED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_CACHE_READ_FAILURE, BrowserLoadFailureReason.CACHE_FAILED);
    assertReason(
        CefLoadHandler.ErrorCode.ERR_OUT_OF_MEMORY, BrowserLoadFailureReason.RESOURCE_EXHAUSTED);
    assertReason(CefLoadHandler.ErrorCode.ERR_FAILED, BrowserLoadFailureReason.UNKNOWN);
    assertEquals(BrowserLoadFailureReason.UNKNOWN, GrapheneCefLoadEventMapper.failureReason(null));
  }

  @Test
  void mapsEveryFailureCodeOrExplicitlyFallsBackToUnknown() {
    Set<CefLoadHandler.ErrorCode> genericFailures =
        Set.of(
            CefLoadHandler.ErrorCode.ERR_NONE,
            CefLoadHandler.ErrorCode.ERR_IO_PENDING,
            CefLoadHandler.ErrorCode.ERR_FAILED,
            CefLoadHandler.ErrorCode.ERR_UNEXPECTED,
            CefLoadHandler.ErrorCode.ERR_NOT_IMPLEMENTED,
            CefLoadHandler.ErrorCode.ERR_READ_IF_READY_NOT_IMPLEMENTED);
    for (CefLoadHandler.ErrorCode errorCode : CefLoadHandler.ErrorCode.values()) {
      BrowserLoadFailureReason reason = GrapheneCefLoadEventMapper.failureReason(errorCode);
      assertNotNull(reason);
      assertEquals(genericFailures.contains(errorCode), reason == BrowserLoadFailureReason.UNKNOWN);
    }
  }

  @Test
  void preservesOptionalBackendDiagnostics() {
    assertEquals(
        CefLoadHandler.ErrorCode.ERR_FAILED.getCode(),
        GrapheneCefLoadEventMapper.diagnosticCode(CefLoadHandler.ErrorCode.ERR_FAILED)
            .orElseThrow());
    assertFalse(GrapheneCefLoadEventMapper.diagnosticCode(null).isPresent());
    assertEquals(200, GrapheneCefLoadEventMapper.httpStatus(200).orElseThrow());
    assertFalse(GrapheneCefLoadEventMapper.httpStatus(0).isPresent());
    assertFalse(GrapheneCefLoadEventMapper.httpStatus(-1).isPresent());
    assertTrue(GrapheneCefLoadEventMapper.httpStatus(599).isPresent());
  }

  private static void assertReason(
      CefLoadHandler.ErrorCode errorCode, BrowserLoadFailureReason expected) {
    assertEquals(expected, GrapheneCefLoadEventMapper.failureReason(errorCode));
  }
}
