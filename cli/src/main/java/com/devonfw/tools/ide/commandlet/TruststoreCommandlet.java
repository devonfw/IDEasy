package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.util.TruststoreUtil;

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
    this.url = add(new StringProperty("", false, "url"));
  }

  @Override
  public String getName() {
    return "fix-vpn-tls-problem";
  }

  @Override
  public boolean isIdeHomeRequired() {
    return false;
  }

  /**
   * This commandlet tries to fix TLS problems for VPN users by capturing the untrusted certificate from the target endpoint and adding it to a custom
   * truststore. It also configures IDE_OPTIONS to use the custom truststore by default. The commandlet is idempotent and will not make changes if the endpoint
   * is already reachable or if the certificate is already trusted.
   * <p>
   * The flow is as follows:
   * <ul>
   * <li>Parse the input URL/host and port.</li>
   * <li>Check if a custom truststore already exists and can establish a TLS connection to the endpoint. If yes, exit successfully.</li>
   * <li>Check if the endpoint is reachable without any certificate changes. If yes, exit successfully.</li>
   * <li>Try to capture the server certificate from the endpoint. If it fails, log an error and exit.</li>
   * <li>Show the captured certificate details to the user and ask if they want to add it to the custom truststore.</li>
   * <li>If the user agrees, ask for a password for the custom truststore and create/update it with the captured certificate.</li>
   * <li>Configure IDE_OPTIONS to use the custom truststore by default.</li>
   * <li>Check if the endpoint is now reachable with the custom truststore and log the result.</li>
   * </ul>
   */
  @Override
  protected void doRun() {

    String endpointInput = this.url.getValueAsString();
    boolean defaultUrlUsed = false;

    if (endpointInput == null || endpointInput.isBlank()) {
      endpointInput = "https://www.github.com";
      defaultUrlUsed = true;
    }

    TruststoreUtil.TlsEndpoint endpoint;
    try {
      endpoint = TruststoreUtil.parseTlsEndpoint(endpointInput);
    } catch (IllegalArgumentException e) {
      throw new CliException("Invalid target URL/host '" + endpointInput + "': " + e.getMessage(), e);
    }

    String host = endpoint.host();
    int port = endpoint.port();
    Path customTruststorePath = this.context.getUserHomeIde().resolve("truststore").resolve("truststore.p12");

    if (TruststoreUtil.isTruststorePresent(customTruststorePath) && TruststoreUtil.isReachable(host, port, customTruststorePath)) {
      IdeLogLevel.SUCCESS.log(LOG, "TLS handshake succeeded with existing custom truststore at {}.", customTruststorePath);
      configureIdeOptions(customTruststorePath);
      return;
    }

    if (TruststoreUtil.isReachable(host, port)) {
      IdeLogLevel.SUCCESS.log(LOG, "Successfully connected to {}:{} without certificate changes.", host, port);
      LOG.info("No truststore update is required for the given address.");
      if (defaultUrlUsed) {
        LOG.info(
            "If the issue still occurs try to call the command again and add the url that is causing the problem to the command: \n ide fix-vpn-tls-problem <url>");
      }

      return;
    }

    LOG.info("The given address {}:{} is not reachable/valid without certificate changes. Continuing with certificate capture.", host, port);

    X509Certificate certificate;
    try {
      certificate = TruststoreUtil.fetchServerCertificate(host, port);
    } catch (Exception e) {
      LOG.error("Failed to capture certificate from {}:{}.", host, port, e);
      IdeLogLevel.INTERACTION.log(LOG,
          "Please check proxy/VPN and retry. You can also follow: https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc#tls-certificate-issues");
      return;
    }

    LOG.info("Captured untrusted certificate:");
    LOG.info(TruststoreUtil.describeCertificate(certificate));

    boolean addToTruststore = this.context.question("Do you want to add this certificate to the custom truststore at {}?", customTruststorePath);

    if (!addToTruststore) {
      LOG.info("Skipped truststore update by user choice.");
      return;
    }

    try {
      TruststoreUtil.createOrUpdateTruststore(customTruststorePath, certificate, "custom");
      IdeLogLevel.SUCCESS.log(LOG, "Custom truststore updated at {}", customTruststorePath);
    } catch (Exception e) {
      LOG.error("Failed to create or update custom truststore at {}", customTruststorePath, e);
      return;
    }

    configureIdeOptions(customTruststorePath);

    if (TruststoreUtil.isReachable(host, port, customTruststorePath)) {
      IdeLogLevel.SUCCESS.log(LOG, "TLS handshake succeeded with custom truststore.");
    } else {
      LOG.warn("TLS handshake still fails even with custom truststore.");
    }
  }

  private void configureIdeOptions(Path customTruststorePath) {
    String truststorePath = customTruststorePath.toAbsolutePath().toString();
    String truststoreOption = TRUSTSTORE_OPTION_PREFIX + truststorePath;
    String truststorePasswordOption = TRUSTSTORE_PASSWORD_OPTION_PREFIX + Arrays.toString(TruststoreUtil.CUSTOM_TRUSTSTORE_PASSWORD);

    EnvironmentVariables confVariables = this.context.getVariables().getByType(EnvironmentVariablesType.USER);

    if (confVariables == null) {
      IdeLogLevel.INTERACTION.log(LOG, "Please configure IDE_OPTIONS manually: {} {}", truststoreOption, truststorePasswordOption);
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
      System.setProperty("javax.net.ssl.trustStorePassword", Arrays.toString(TruststoreUtil.CUSTOM_TRUSTSTORE_PASSWORD));
      IdeLogLevel.SUCCESS.log(LOG, "IDE_OPTIONS configured to use custom truststore by default.");
    } catch (UnsupportedOperationException e) {
      IdeLogLevel.INTERACTION.log(LOG, "Please configure IDE_OPTIONS manually: {} {}", truststoreOption, truststorePasswordOption);
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
