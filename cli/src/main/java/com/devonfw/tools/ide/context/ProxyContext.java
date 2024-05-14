package com.devonfw.tools.ide.context;

import java.net.MalformedURLException;
import java.net.URL;

public class ProxyContext {

  private final IdeContext context;

  private static final String HTTP_PROXY = "http_proxy";

  private static final String HTTPS_PROXY = "https_proxy";

  private ProxyConfig httpProxyConfig;

  private ProxyConfig httpsProxyConfig;

  public ProxyContext(IdeContext context) {

    this.context = context;

    httpProxyConfig = new ProxyConfig(HTTP_PROXY);
    httpsProxyConfig = new ProxyConfig(HTTPS_PROXY);
  }

  public ProxyConfig getProxyConfig(String url) {

    try {
      URL parsedUrl = new URL(url);
      String protocol = parsedUrl.getProtocol();
      if ("http".equalsIgnoreCase(protocol)) {
        return httpProxyConfig;
      } else if ("https".equalsIgnoreCase(protocol)) {
        return httpsProxyConfig;
      }
    } catch (MalformedURLException e) {
      // Handle the error appropriately
    }
    return null; // Unsupported protocol or invalid URL
  }

  // Getters for proxy configurations
  public ProxyConfig getHttpProxyConfig() {

    return httpProxyConfig;
  }

  public ProxyConfig getHttpsProxyConfig() {

    return httpsProxyConfig;
  }
}

