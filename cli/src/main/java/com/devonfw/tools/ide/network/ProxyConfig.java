package com.devonfw.tools.ide.network;

import com.devonfw.tools.ide.context.IdeContext;

import java.net.MalformedURLException;
import java.net.URL;

class ProxyConfig {

  private final IdeContext context;

  private String host;

  private int port;

  ProxyConfig(String proxyUrl, IdeContext context) {

    this.context = context;

    //String proxyUrl = parseProxyValue(proxyEnvVariable);
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
      port = url.getPort();
    } catch (MalformedURLException e) {
      this.context.warning(ProxyContext.PROXY_FORMAT_WARNING_MESSAGE + " CONFIG");
      host = null;
      port = 0;
      this.context.warning("host and port" + host + port);
      // TODO: send and appropriate error message
    }
  }

  // Getters for host and port
  String getHost() {

    return host;
  }

  int getPort() {

    return port;
  }
}
