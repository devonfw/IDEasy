package com.devonfw.tools.ide.truststore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.util.TruststoreUtil;

/**
 * Test of {@link TruststoreUtil}.
 */
class TruststoreUtilTest {

  private static final String PASSWORD = "changeit";

  private static final String TEST_CERT_RESOURCE = "/truststore/test-cert.pem";

  @TempDir
  Path tempDir;

  @Test
  void testParseTlsEndpointFromHttpsUrl() {

    TruststoreUtil.TlsEndpoint endpoint = TruststoreUtil.parseTlsEndpoint("https://github.com/tools/path");

    assertThat(endpoint.host()).isEqualTo("github.com");
    assertThat(endpoint.port()).isEqualTo(443);
  }

  @Test
  void testParseTlsEndpointFromHostAndPort() {

    TruststoreUtil.TlsEndpoint endpoint = TruststoreUtil.parseTlsEndpoint("my-host.local:8443");

    assertThat(endpoint.host()).isEqualTo("my-host.local");
    assertThat(endpoint.port()).isEqualTo(8443);
  }

  @Test
  void testParseTlsEndpointRejectsHttp() {

    assertThatThrownBy(() -> TruststoreUtil.parseTlsEndpoint("http://github.com")).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Only HTTPS URLs are supported");
  }

  @Test
  void testIsTruststorePresent() {

    Path path = this.tempDir.resolve("truststore.p12");
    assertThat(TruststoreUtil.isTruststorePresent(path)).isFalse();

    writeEmptyTruststore(path);

    assertThat(TruststoreUtil.isTruststorePresent(path)).isTrue();
  }

  @Test
  void testCopyTruststore() throws Exception {

    X509Certificate certificate = loadCertificateFromResource();
    KeyStore source = KeyStore.getInstance("PKCS12");
    source.load(null, PASSWORD.toCharArray());
    source.setCertificateEntry("source-cert", certificate);

    KeyStore target = KeyStore.getInstance("PKCS12");
    target.load(null, PASSWORD.toCharArray());

    TruststoreUtil.copyTruststore(source, target);

    assertThat(target.getCertificate("source-cert")).isNotNull();
  }

  @Test
  void testCreateOrUpdateTruststoreAddsCertificateOnlyOnce() throws Exception {

    Path truststorePath = this.tempDir.resolve("custom-existing.p12");
    writeEmptyTruststore(truststorePath);

    X509Certificate certificate = loadCertificateFromResource();

    TruststoreUtil.createOrUpdateTruststore(truststorePath, certificate, "custom");
    int countAfterFirstAdd = countCertificateOccurrences(truststorePath, certificate);

    TruststoreUtil.createOrUpdateTruststore(truststorePath, certificate, "custom");
    int countAfterSecondAdd = countCertificateOccurrences(truststorePath, certificate);

    assertThat(countAfterFirstAdd).isEqualTo(1);
    assertThat(countAfterSecondAdd).isEqualTo(1);
  }

  @Test
  void testCreateOrUpdateTruststoreCreatesFileIfMissing() throws Exception {

    Path truststorePath = this.tempDir.resolve("nested").resolve("custom-new.p12");

    TruststoreUtil.createOrUpdateTruststore(truststorePath, loadCertificateFromResource(), "custom");

    assertThat(truststorePath).exists();
    KeyStore truststore = loadTruststore(truststorePath);
    assertThat(truststore.size()).isGreaterThan(0);
  }

  @Test
  void testDescribeCertificateContainsExpectedSections() throws Exception {

    X509Certificate certificate = loadCertificateFromResource();

    String description = TruststoreUtil.describeCertificate(certificate);

    assertThat(description).contains("Subject:");
    assertThat(description).contains("Issuer :");
    assertThat(description).contains("Serial :");
    assertThat(description).contains("SigAlg :");
  }

  @Test
  void testFetchServerCertificateValidatesInput() {

    assertThatThrownBy(() -> TruststoreUtil.fetchServerCertificate(null, 443)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> TruststoreUtil.fetchServerCertificate(" ", 443)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("host must not be blank");
    assertThatThrownBy(() -> TruststoreUtil.fetchServerCertificate("github.com", 0)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("port must be between 1 and 65535");
  }

  @Test
  void testLoadCertificateFromResource() throws Exception {

    X509Certificate certificate = loadCertificateFromResource();

    assertThat(certificate.getSubjectX500Principal().getName()).contains("CN=IDEasy Test Cert");
  }

  private static X509Certificate loadCertificateFromResource() throws Exception {

    try (InputStream in = TruststoreUtilTest.class.getResourceAsStream(TEST_CERT_RESOURCE)) {
      assertThat(in).as("Test certificate resource must exist: " + TEST_CERT_RESOURCE).isNotNull();
      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      return (X509Certificate) certificateFactory.generateCertificate(in);
    }
  }

  private static void writeEmptyTruststore(Path path) {

    try {
      Files.createDirectories(path.getParent());
      KeyStore truststore = KeyStore.getInstance("PKCS12");
      truststore.load(null, PASSWORD.toCharArray());
      try (OutputStream out = Files.newOutputStream(path)) {
        truststore.store(out, PASSWORD.toCharArray());
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize empty truststore for test: " + path, e);
    }
  }

  private static KeyStore loadTruststore(Path truststorePath) throws Exception {

    KeyStore truststore = KeyStore.getInstance("PKCS12");
    try (InputStream in = Files.newInputStream(truststorePath)) {
      truststore.load(in, PASSWORD.toCharArray());
    }
    return truststore;
  }

  private static int countCertificateOccurrences(Path truststorePath, X509Certificate certificate) throws Exception {

    KeyStore truststore = loadTruststore(truststorePath);
    Enumeration<String> aliases = truststore.aliases();
    int count = 0;
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      if (truststore.getCertificate(alias) instanceof X509Certificate existing && Arrays.equals(existing.getEncoded(), certificate.getEncoded())) {
        count++;
      }
    }
    return count;
  }

}




