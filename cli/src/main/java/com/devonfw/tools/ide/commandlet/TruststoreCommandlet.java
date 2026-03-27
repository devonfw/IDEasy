package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.truststore.TruststoreUtilImpl;

/**
 * {@link Commandlet} to fix the TLS problem for VPN users.
 */
public class TruststoreCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(TruststoreCommandlet.class);

  private static final String IDE_OPTIONS = "IDE_OPTIONS";

  private static final String TRUSTSTORE_OPTION_PREFIX = "-Djavax.net.ssl.trustStore=";

  private static final String TRUSTSTORE_PASSWORD_OPTION_PREFIX = "-Djavax.net.ssl.trustStorePassword=";

  private final StringProperty url;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public TruststoreCommandlet(IdeContext context) {
    super(context);
    addKeyword(getName());
    this.url = add(new StringProperty("", true, "url"));
  }

  @Override
  public String getName() {

    return "fix-vpn-tls-problem";
  }

  // Steps:
  // 1. Check if there was an TLS issue before, if not, log and return.
  //   - Check if there was an SSLHandshakeException with a message like "PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target"
  // 2. Fetch the URL again and retrieve Certificate Chain and save it
  // 3. Print the untrusted Certificate including extensions
  // 4. Ask the user if they want to add the certificate to a custom truststore
  // 5. If yes, create or update the custom truststore
  //  - Create
  //    - Create new Truststore file in IDEasy settings (e.g. settings/truststore/truststore.p12)
  //    - Retrieve certificates from default truststore (e.g. cacerts) and add them to the new truststore
  //    - Add the new certificate to the new truststore
  //     - Create new Alias for the new certificate (e.g. "custom-1", "custom-2", etc.)
  //  - Update
  //    - Load existing custom truststore
  //    - Check if the certificate is already in the truststore, if yes, log and return
  //    - Add the new certificate to the new truststore
  //     - Create new Alias for the new certificate (e.g. "custom-1", "custom-2", etc.)
  // 6. Fetch the URL again with the new truststore and check if the TLS issue is resolved,
  //  - if yes, log success,
  //  - if not, log failure and potential next steps (e.g. check if you are behind a corporate proxy and need to configure it in IDEasy).
  @Override
  protected void doRun() {

    String endpointInput = this.url.getValueAsString();
    TruststoreUtilImpl.TlsEndpoint endpoint;
    try {
      endpoint = TruststoreUtilImpl.parseTlsEndpoint(endpointInput);
    } catch (IllegalArgumentException e) {
      throw new CliException("Invalid target URL/host '" + endpointInput + "': " + e.getMessage(), e);
    }

    String host = endpoint.host();
    int port = endpoint.port();
    Path customTruststorePath = this.context.getSettingsPath().resolve("truststore").resolve("truststore.p12");

    if (TruststoreUtilImpl.isTruststorePresent(customTruststorePath)
        && TruststoreUtilImpl.isReachable(host, port, customTruststorePath)) {
      IdeLogLevel.SUCCESS.log(LOG, "TLS handshake succeeded with existing custom truststore at {}.", customTruststorePath);
      configureIdeOptions(customTruststorePath);
      return;
    }

    if (TruststoreUtilImpl.isReachable(host, port)) {
      LOG.info("Successfully connected to {}:{} without certificate changes.", host, port);
      LOG.info("No truststore update is required for the given address.");
      return;
    }

    LOG.info("The given address {}:{} is not reachable/valid without certificate changes. Continuing with certificate capture.", host, port);

    X509Certificate certificate;
    try {
      certificate = TruststoreUtilImpl.fetchServerCertificate(host, port);
    } catch (Exception e) {
      LOG.error("Failed to capture certificate from {}:{}.", host, port, e);
      IdeLogLevel.INTERACTION.log(LOG,
          "Please check proxy/VPN and retry. You can also follow: https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc#tls-certificate-issues");
      return;
    }

    LOG.info("Captured untrusted certificate:");
    LOG.info(TruststoreUtilImpl.describeCertificate(certificate));

    boolean addToTruststore = this.context.question("Do you want to add this certificate to the custom truststore at {}?", customTruststorePath);

    if (!addToTruststore) {
      LOG.info("Skipped truststore update by user choice.");
      return;
    }

    try {
      TruststoreUtilImpl.createOrUpdateTruststore(customTruststorePath, certificate, "custom");
      IdeLogLevel.SUCCESS.log(LOG, "Custom truststore updated at {}", customTruststorePath);
    } catch (Exception e) {
      LOG.error("Failed to create or update custom truststore at {}", customTruststorePath, e);
      return;
    }

    configureIdeOptions(customTruststorePath);

    if (TruststoreUtilImpl.isReachable(host, port, customTruststorePath)) {
      IdeLogLevel.SUCCESS.log(LOG, "TLS handshake succeeded with custom truststore.");
    } else {
      LOG.warn("TLS handshake still fails even with custom truststore.");
    }
  }

  private void configureIdeOptions(Path customTruststorePath) {
    String truststorePath = customTruststorePath.toAbsolutePath().toString();
    String truststoreOption = TRUSTSTORE_OPTION_PREFIX + truststorePath;
    String truststorePasswordOption = TRUSTSTORE_PASSWORD_OPTION_PREFIX + "changeit";

    EnvironmentVariables confVariables = this.context.getVariables().getByType(EnvironmentVariablesType.CONF);
    if (confVariables == null) {
      IdeLogLevel.INTERACTION.log(LOG,
          "Please configure IDE_OPTIONS manually: {} {}",
          truststoreOption,
          truststorePasswordOption);
      return;
    }

    String options = confVariables.getFlat(IDE_OPTIONS);
    options = removeOptionWithPrefix(options, TRUSTSTORE_OPTION_PREFIX);
    options = removeOptionWithPrefix(options, TRUSTSTORE_PASSWORD_OPTION_PREFIX);
    options = appendOption(options, truststoreOption);
    options = appendOption(options, truststorePasswordOption);

    try {
      confVariables.set(IDE_OPTIONS, options, true);
      confVariables.save();
      // Apply directly for the current process as well.
      System.setProperty("javax.net.ssl.trustStore", truststorePath);
      System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
      IdeLogLevel.SUCCESS.log(LOG, "IDE_OPTIONS configured to use custom truststore by default.");
    } catch (UnsupportedOperationException e) {
      IdeLogLevel.INTERACTION.log(LOG,
          "Please configure IDE_OPTIONS manually: {} {}",
          truststoreOption,
          truststorePasswordOption);
    }
  }

  private static String removeOptionWithPrefix(String options, String prefix) {
    if ((options == null) || options.isBlank()) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    String[] tokens = options.trim().split("\\s+");
    for (String token : tokens) {
      if (!token.startsWith(prefix)) {
        if (!result.isEmpty()) {
          result.append(' ');
        }
        result.append(token);
      }
    }
    return result.toString();
  }

  private static String appendOption(String options, String option) {
    if ((options == null) || options.isBlank()) {
      return option;
    }
    return options + " " + option;
  }


}
