package com.devonfw.tools.ide.context;

import java.net.MalformedURLException;
import java.net.URL;

public class ProxyConfig {

  private final IdeContext context;

  private String protocol;

  private String host;

  private int port;

  public ProxyConfig(String proxyEnvVariable, IdeContext context) {

    this.context = context;

    String proxyUrl = parseProxyValue(proxyEnvVariable);
    parseProxyUrl(proxyUrl);
  }

  private String parseProxyValue(String proxyEnvVariable) {

    String proxyUrl = System.getenv(proxyEnvVariable);
    if (proxyUrl == null) {
      proxyUrl = System.getenv(proxyEnvVariable.toUpperCase()); // Prefer uppercase environment variables
    }
    return proxyUrl;
  }

  private void parseProxyUrl(String proxyUrl) {

    try {
      URL url = new URL(proxyUrl);
      protocol = url.getProtocol();
      host = url.getHost();
      port = url.getPort();
    } catch (MalformedURLException e) {
      this.context.warning(ProxyContext.PROXY_FORMAT_WARNING_MESSAGE);
      // TODO: send and appropriate error message
    }
  }

  // Getters for host and port
  public String getHost() {

    return host;
  }

  public int getPort() {

    return port;
  }
}
