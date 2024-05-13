package com.devonfw.tools.ide.context;

import java.net.MalformedURLException;
import java.net.URL;

public class ProxyConfig {
  private String host;

  private int port;

  public ProxyConfig(String proxyEnvVariable) {

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
      host = url.getHost();
      port = url.getPort() != -1 ? url.getPort() : 8888; // Default port if not specified
    } catch (MalformedURLException e) {
      // TODO: send and appropriate error message
      host = null;
      port = -1;
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
