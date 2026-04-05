package com.devonfw.tools.ide.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Utility methods for truststore handling and TLS certificate capture.
 */
public final class TruststoreUtil {

  /**
   * Parsed TLS endpoint with host and port.
   *
   * @param host the server host.
   * @param port the server port.
   */
  public record TlsEndpoint(String host, int port) {

  }

  private static final String TRUSTSTORE_PASSWORD = "changeit";

  /**
   * Default password for the JRE cacerts truststore
   */
  public static final char[] DEFAULT_CACERTS_PASSWORD = TRUSTSTORE_PASSWORD.toCharArray();

  /**
   * Password for the custom truststore
   */
  public static final char[] CUSTOM_TRUSTSTORE_PASSWORD = TRUSTSTORE_PASSWORD.toCharArray();

  /**
   * Default prefix for aliases of certificates added to the truststore.
   */
  private static final String DEFAULT_ALIAS_PREFIX = "custom";

  private static final int DEFAULT_TIMEOUT_MILLIS = 10_000;

  private static final String TLS_PROTOCOL = "TLS";

  private TruststoreUtil() {
    // utility class
  }

  /**
   * Checks if a truststore file exists at the specified path.
   *
   * @param path the path to the truststore file.
   * @return {@code true} if a truststore file exists at the specified path, {@code false} otherwise.
   */
  public static boolean isTruststorePresent(Path path) {
    return (path != null) && Files.exists(path);
  }

  /**
   * Loads the default Java truststore from the JRE cacerts file.
   *
   * @return the default Java truststore loaded from the JRE cacerts file.
   * @throws Exception if an error occurs while loading the default truststore.
   */
  public static KeyStore getDefaultTruststore() throws Exception {
    String javaHome = System.getProperty("java.home");
    Path cacertsPath = Path.of(javaHome, "lib", "security", "cacerts");
    if (!Files.exists(cacertsPath)) {
      throw new IllegalStateException("Default cacerts not found: " + cacertsPath);
    }

    KeyStore cacerts = KeyStore.getInstance(KeyStore.getDefaultType());
    try (InputStream in = Files.newInputStream(cacertsPath)) {
      cacerts.load(in, DEFAULT_CACERTS_PASSWORD);
    }
    return cacerts;
  }

  /**
   * Copies all certificate entries from the source truststore to the target truststore. Key entries are not copied, but if a key entry is encountered, its
   * first certificate in the chain is copied as a certificate entry.
   *
   * @param source the source truststore to copy from.
   * @param target the target truststore to copy to.
   * @throws Exception if an error occurs while copying the truststore.
   */
  public static void copyTruststore(KeyStore source, KeyStore target) throws Exception {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");

    Enumeration<String> aliases = source.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      if (source.isCertificateEntry(alias)) {
        Certificate cert = source.getCertificate(alias);
        if (cert != null) {
          target.setCertificateEntry(alias, cert);
        }
      } else if (source.isKeyEntry(alias)) {
        Certificate[] chain = source.getCertificateChain(alias);
        if ((chain != null) && (chain.length > 0)) {
          target.setCertificateEntry(alias, chain[0]);
        }
      }
    }
  }

  /**
   * Creates a new truststore at the specified path or updates an existing one by adding the given certificate if it is not already present. If the truststore
   * does not
   *
   * @param customTruststorePath the path to the custom truststore file to create or update.
   * @param certificate the certificate to add to the truststore if not already present.
   * @param aliasPrefix the prefix to use for the alias of the new certificate (e.g. "custom"). If {@code null} or blank, a default prefix is used.
   * @throws Exception if an error occurs while creating or updating the truststore.
   */
  public static void createOrUpdateTruststore(Path customTruststorePath, X509Certificate certificate, String aliasPrefix) throws Exception {
    Objects.requireNonNull(customTruststorePath, "customTruststorePath");
    Objects.requireNonNull(certificate, "certificate");

    if ((aliasPrefix == null) || aliasPrefix.isBlank()) {
      aliasPrefix = DEFAULT_ALIAS_PREFIX;
    }

    Path parent = customTruststorePath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    KeyStore customStore = KeyStore.getInstance("PKCS12");
    if (isTruststorePresent(customTruststorePath)) {
      try (InputStream in = Files.newInputStream(customTruststorePath)) {
        customStore.load(in, CUSTOM_TRUSTSTORE_PASSWORD);
      }
    } else {
      customStore.load(null, CUSTOM_TRUSTSTORE_PASSWORD);
      copyTruststore(getDefaultTruststore(), customStore);
    }

    if (!containsCertificate(customStore, certificate)) {
      String alias = makeUniqueAlias(customStore, aliasPrefix);
      addCertificate(customStore, alias, certificate);
    }

    try (OutputStream out = Files.newOutputStream(customTruststorePath)) {
      customStore.store(out, CUSTOM_TRUSTSTORE_PASSWORD);
    }
  }

  /**
   * Adds the given certificate to the truststore under the specified alias. If the alias already exists, it will be overwritten.
   *
   * @param truststore the truststore to add the certificate to.
   * @param alias the alias under which to add the certificate.
   * @param certificate the certificate to add to the truststore.
   * @throws Exception if an error occurs while adding the certificate to the truststore.
   */
  public static void addCertificate(KeyStore truststore, String alias, X509Certificate certificate) throws Exception {
    Objects.requireNonNull(truststore, "truststore");
    Objects.requireNonNull(alias, "alias");
    Objects.requireNonNull(certificate, "certificate");
    truststore.setCertificateEntry(alias, certificate);
  }

  /**
   * Parses a user input to a TLS endpoint supporting forms like {@code host}, {@code host:port}, and {@code https://host[:port]/path}.
   *
   * @param input the user input.
   * @return the parsed {@link TlsEndpoint}.
   */
  public static TlsEndpoint parseTlsEndpoint(String input) {
    if ((input == null) || input.isBlank()) {
      throw new IllegalArgumentException("URL/host must not be empty.");
    }
    String candidate = input.trim();

    if (candidate.startsWith("http://")) {
      throw new IllegalArgumentException("Only HTTPS URLs are supported: " + input);
    }

    if (candidate.startsWith("https://")) {
      return parseEndpointFromUri(input, URI.create(candidate));
    }

    if (candidate.contains("://")) {
      URI uri = URI.create(candidate);
      String scheme = uri.getScheme();
      if ((scheme == null) || !"https".equals(scheme.toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException("Only HTTPS URLs are supported: " + input);
      }
      return parseEndpointFromUri(input, uri);
    }

    int separatorIndex = candidate.lastIndexOf(':');
    if (separatorIndex > 0 && separatorIndex < (candidate.length() - 1) && candidate.indexOf(':') == separatorIndex) {
      String host = candidate.substring(0, separatorIndex).trim();
      String portPart = candidate.substring(separatorIndex + 1).trim();
      int port;
      try {
        port = Integer.parseInt(portPart);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid port in input: " + input, e);
      }
      validateEndpoint(host, port, input);
      return new TlsEndpoint(host, port);
    }

    validateEndpoint(candidate, 443, input);
    return new TlsEndpoint(candidate, 443);
  }

  private static TlsEndpoint parseEndpointFromUri(String input, URI uri) {
    String host = uri.getHost();
    int port = (uri.getPort() > 0) ? uri.getPort() : 443;
    validateEndpoint(host, port, input);
    return new TlsEndpoint(host, port);
  }

  private static void validateEndpoint(String host, int port, String input) {
    if ((host == null) || host.isBlank()) {
      throw new IllegalArgumentException("Missing host in input: " + input);
    }
    if ((port < 1) || (port > 65535)) {
      throw new IllegalArgumentException("Port out of range in input: " + input);
    }
  }

  /**
   * Checks if a TLS endpoint can be reached and validated with the current default trust configuration.
   *
   * @param host the server host to connect to.
   * @param port the server port to connect to.
   * @return {@code true} if TLS handshake succeeds without truststore changes, {@code false} otherwise.
   */
  public static boolean isReachable(String host, int port) {
    validateEndpoint(host, port, host + ":" + port);
    try {
      SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
      sslContext.init(null, null, new SecureRandom());
      SSLSocketFactory factory = sslContext.getSocketFactory();

      try (SSLSocket socket = connectTlsSocket(factory, host, port)) {
        socket.startHandshake();
      }
      return true;
    } catch (Exception e) {
      return false;
    }

  }

  /**
   * Checks if a TLS endpoint can be reached and validated using the provided custom truststore.
   *
   * @param host the server host to connect to.
   * @param port the server port to connect to.
   * @param truststorePath the path to the custom truststore to use.
   * @return {@code true} if TLS handshake succeeds with the custom truststore, {@code false} otherwise.
   */
  public static boolean isReachable(String host, int port, Path truststorePath) {
    validateEndpoint(host, port, host + ":" + port);
    Objects.requireNonNull(truststorePath, "truststorePath");
    try {
      verifyConnectionWithTruststore(host, port, truststorePath);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static SSLSocket connectTlsSocket(SSLSocketFactory factory, String host, int port) throws Exception {
    SSLSocket socket = (SSLSocket) factory.createSocket();
    try {
      socket.connect(new InetSocketAddress(host, port), DEFAULT_TIMEOUT_MILLIS);
      socket.setSoTimeout(DEFAULT_TIMEOUT_MILLIS);
      return socket;
    } catch (Exception e) {
      try {
        socket.close();
      } catch (Exception ignored) {
        // ignore close failures on unsuccessful connect
      }
      throw e;
    }
  }

  /**
   * Fetches the server certificate from the specified host and port by performing a TLS handshake and capturing the certificate chain using a custom trust
   * manager.
   *
   * @param host the server host to connect to.
   * @param port the server port to connect to.
   * @return the server certificate captured from the TLS handshake.
   * @throws Exception if an error occurs while fetching the server certificate, e.g. due to connection issues or if the server does not provide a
   *     certificate chain.
   */
  public static X509Certificate fetchServerCertificate(String host, int port) throws Exception {
    Objects.requireNonNull(host, "host");
    if (host.isBlank()) {
      throw new IllegalArgumentException("host must not be blank");
    }
    if ((port < 1) || (port > 65535)) {
      throw new IllegalArgumentException("port must be between 1 and 65535");
    }

    SavingTrustManager savingTrustManager = new SavingTrustManager();

    SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
    sslContext.init(null, new TrustManager[] { savingTrustManager }, new SecureRandom());

    SSLSocketFactory factory = sslContext.getSocketFactory();
    try (SSLSocket socket = connectTlsSocket(factory, host, port)) {
      socket.startHandshake();
    } catch (SSLException e) {
      // expected: trust manager aborts after capturing the chain
    }

    X509Certificate[] chain = savingTrustManager.getChain();
    if ((chain == null) || (chain.length == 0)) {
      throw new CertificateException("Could not capture server certificate chain from " + host + ":" + port);
    }

    return chain[chain.length - 1];
  }

  /**
   * Verifies that a TLS connection to the specified host and port can be established using the truststore at the given path by performing a TLS handshake. If
   * the handshake is successful, the method returns normally. If the handshake fails due to trust issues, an SSLException is thrown.
   *
   * @param host the server host to connect to.
   * @param port the server port to connect to.
   * @param truststorePath the path to the truststore file to use for the TLS handshake.
   * @throws Exception if an error occurs while verifying the connection, e.g. due to connection issues, TLS handshake failure, or if the truststore file
   *     cannot be loaded.
   */
  public static void verifyConnectionWithTruststore(String host, int port, Path truststorePath) throws Exception {
    KeyStore truststore = KeyStore.getInstance("PKCS12");
    try (InputStream in = Files.newInputStream(truststorePath)) {
      truststore.load(in, CUSTOM_TRUSTSTORE_PASSWORD);
    }

    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(truststore);

    SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
    sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

    SSLSocketFactory socketFactory = sslContext.getSocketFactory();
    try (SSLSocket socket = (SSLSocket) socketFactory.createSocket(host, port)) {
      socket.setSoTimeout(DEFAULT_TIMEOUT_MILLIS);
      socket.startHandshake();
    }
  }


  /**
   * Generates a human-readable description of the given X.509 certificate including subject, issuer, serial number, validity period, signature algorithm, and
   *
   * @param certificate the certificate to describe.
   * @return a human-readable description of the given X.509 certificate.
   */
  public static String describeCertificate(X509Certificate certificate) {
    String nl = "\n";
    StringBuilder sb = new StringBuilder();
    sb.append("Subject: ").append(certificate.getSubjectX500Principal()).append(nl);
    sb.append("Issuer : ").append(certificate.getIssuerX500Principal()).append(nl);
    sb.append("Serial : ").append(certificate.getSerialNumber()).append(nl);
    sb.append("Valid  : ").append(certificate.getNotBefore()).append(" -> ").append(certificate.getNotAfter()).append(nl);
    sb.append("SigAlg : ").append(certificate.getSigAlgName()).append(nl);

    Set<String> critical = certificate.getCriticalExtensionOIDs();
    Set<String> nonCritical = certificate.getNonCriticalExtensionOIDs();
    sb.append("Critical extensions    : ").append((critical == null) ? "[]" : critical).append(nl);
    sb.append("Non-critical extensions: ").append((nonCritical == null) ? "[]" : nonCritical);

    return sb.toString();
  }

  private static boolean containsCertificate(KeyStore keyStore, X509Certificate certificate) throws Exception {
    Enumeration<String> aliases = keyStore.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      Certificate existing = keyStore.getCertificate(alias);
      if ((existing instanceof X509Certificate existingX509) && Arrays.equals(existingX509.getEncoded(), certificate.getEncoded())) {
        return true;
      }
    }
    return false;
  }

  private static String makeUniqueAlias(KeyStore keyStore, String baseAlias) throws Exception {
    String alias = baseAlias;
    int i = 1;
    while (keyStore.containsAlias(alias)) {
      alias = baseAlias + "-" + i;
      i++;
    }
    return alias;
  }

  private static final class SavingTrustManager implements X509TrustManager {

    private X509Certificate[] chain;

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
      // not needed
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      this.chain = (chain == null) ? null : Arrays.copyOf(chain, chain.length);
      if ((chain == null) || (chain.length == 0)) {
        throw new CertificateException("Server certificate chain is empty");
      }
      throw new CertificateException("Captured server certificate chain");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

    public X509Certificate[] getChain() {
      return this.chain;
    }
  }
}
