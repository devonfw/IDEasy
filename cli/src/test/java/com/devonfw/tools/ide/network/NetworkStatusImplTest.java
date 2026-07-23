package com.devonfw.tools.ide.network;

import java.util.concurrent.Callable;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link NetworkStatusImpl}.
 */
class NetworkStatusImplTest extends AbstractIdeContextTest {

  /**
   * Verifies that a TLS certificate trust error is detected and reported with an actionable hint even when the {@link SSLHandshakeException} is wrapped in a
   * {@link RuntimeException} - which is how the downloader propagates it and which previously slipped through as an "unexpected" error without any hint.
   */
  @Test
  void testInvokeNetworkTaskDetectsWrappedTlsTrustIssueAndReportsFailingUrl() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    NetworkStatusImpl networkStatus = new NetworkStatusImpl(context);
    String uri = "https://aka.ms/vs/17/release/vs_BuildTools.exe";
    Callable<Void> failingTask = () -> {
      SSLHandshakeException tlsError = new SSLHandshakeException(
          "PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target");
      // mimics how HttpDownloader wraps the handshake error in a RuntimeException
      throw new IllegalStateException("Failed to stream response body from url: " + uri, tlsError);
    };

    assertThatThrownBy(() -> networkStatus.invokeNetworkTask(failingTask, uri))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TLS certificate trust error")
        .hasMessageContaining(uri);

    assertThat(context).logAtWarning().hasMessageContaining("ide fix-vpn-tls-problem " + uri);
  }

  /**
   * Verifies that {@link NetworkStatusImpl#isTlsTrustIssue(Throwable)} detects the PKIX indicator anywhere in the cause chain.
   */
  @Test
  void testIsTlsTrustIssueDetectsPkixInCauseChain() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    NetworkStatusImpl networkStatus = new NetworkStatusImpl(context);

    Throwable wrapped = new IllegalStateException("outer",
        new IllegalStateException("middle", new SSLHandshakeException("PKIX path building failed: unable to find valid certification path")));

    assertThat(networkStatus.isTlsTrustIssue(wrapped)).isTrue();
    assertThat(networkStatus.isTlsTrustIssue(new IllegalStateException("some unrelated network glitch"))).isFalse();
  }
}
