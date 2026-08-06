package com.devonfw.tools.ide.truststore;

import java.nio.file.Path;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Test helper to start a WireMock HTTPS server that presents a self-signed {@code CN=localhost} certificate (with matching {@code localhost} SAN so that
 * hostname verification of the JDK {@link java.net.http.HttpClient} passes). The certificate is not part of the JRE {@code cacerts} and therefore simulates an
 * untrusted (e.g. VPN-intercepted) endpoint.
 */
public final class HttpsTestServer {

  private static final String KEYSTORE_RESOURCE = "/truststore/localhost.p12";

  private static final String KEYSTORE_PASSWORD = "changeit";

  private HttpsTestServer() {
    // utility class
  }

  /**
   * @return a freshly started {@link WireMockServer} listening on a dynamic HTTPS port. The caller is responsible for {@link WireMockServer#stop() stopping} it.
   */
  public static WireMockServer start() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options()
        .httpDisabled(true)
        .dynamicHttpsPort()
        .keystorePath(keystorePath())
        .keystorePassword(KEYSTORE_PASSWORD)
        .keyManagerPassword(KEYSTORE_PASSWORD)
        .keystoreType("PKCS12"));
    server.start();
    return server;
  }

  private static String keystorePath() {
    try {
      return Path.of(HttpsTestServer.class.getResource(KEYSTORE_RESOURCE).toURI()).toAbsolutePath().toString();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve test keystore resource: " + KEYSTORE_RESOURCE, e);
    }
  }
}
