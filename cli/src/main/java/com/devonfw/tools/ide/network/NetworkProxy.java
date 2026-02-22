package com.devonfw.tools.ide.network;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.IdeSystem;

/**
 * Simple class to {@link #configure()} network proxy.
 */
public class NetworkProxy {

  private static final Logger LOG = LoggerFactory.getLogger(NetworkProxy.class);

  private static final String PROXY_DOCUMENTATION_PAGE = "https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc";

  private final IdeContext context;

  private String nonProxyHosts;

  private String allProxy;

  /**
   * @param context the {@link IdeContext}.
   */
  public NetworkProxy(IdeContext context) {

    super();
    this.context = context;
  }

  /**
   * Perform the actual {@link NetworkProxy} configuration.
   */
  public void configure() {

    setupNetworkProxy("http");
    setupNetworkProxy("https");
  }

  private void setupNetworkProxy(String protocol) {

    String systemPropertyProxyHost = protocol + ".proxyHost";
    String configuredValue = System.getProperty(systemPropertyProxyHost);
    if (configuredValue != null) {
      LOG.trace("Proxy already configured via system property {}={}", systemPropertyProxyHost, configuredValue);
      return;
    }
    String proxyUrlString = getProxyUrlFromEnvironmentVariable(protocol);
    if (proxyUrlString == null) {
      LOG.trace("No {} proxy configured.", protocol);
      return;
    }
    try {
      URL proxyUrl = new URL(proxyUrlString);
      IdeSystem system = this.context.getSystem();
      system.setProperty(systemPropertyProxyHost, proxyUrl.getHost());
      int port = proxyUrl.getPort();
      if (port == -1) {
        String urlProtocol = proxyUrl.getProtocol().toLowerCase(Locale.ROOT);
        if ("http".equals(urlProtocol)) {
          port = 80;
        } else if ("https".equals(urlProtocol)) {
          port = 443;
        } else if ("ftp".equals(urlProtocol)) {
          port = 21;
        }
      }
      system.setProperty(protocol + ".proxyPort", Integer.toString(port));
      if (this.nonProxyHosts == null) {
        this.nonProxyHosts = getEnvironmentVariableNonNull("no_proxy");
      }
      if (!this.nonProxyHosts.isEmpty()) {
        system.setProperty(protocol + ".nonProxyHosts", this.nonProxyHosts);
      }
    } catch (MalformedURLException e) {
      LOG.warn("Invalid {} proxy configuration detected with URL {}. Proxy configuration will be skipped.\n"
          + "For further details, see " + PROXY_DOCUMENTATION_PAGE, protocol, proxyUrlString, e);
    }
  }

  private String getProxyUrlFromEnvironmentVariable(String protocol) {

    String proxyUrl = getEnvironmentVariableCaseInsensitive(protocol + "_proxy");
    if (proxyUrl == null) {
      if (this.allProxy == null) {
        this.allProxy = getEnvironmentVariableNonNull("all_proxy");
      }
      if (!this.allProxy.isEmpty()) {
        proxyUrl = this.allProxy;
      }
    }
    return proxyUrl;
  }

  private String getEnvironmentVariableNonNull(String nameLowerCase) {

    String value = getEnvironmentVariableCaseInsensitive(nameLowerCase);
    if (value == null) {
      return "";
    } else {
      return value.trim();
    }
  }

  private String getEnvironmentVariableCaseInsensitive(String nameLowerCase) {

    String value = getEnvironmentVariable(nameLowerCase);
    if (value == null) {
      value = getEnvironmentVariable(nameLowerCase.toUpperCase(Locale.ROOT));
    }
    return value;
  }

  private String getEnvironmentVariable(String name) {

    String value = this.context.getSystem().getEnv(name);
    if (value != null) {
      LOG.trace("Found environment variable {}={}", name, value);
    }
    return value;
  }

}
