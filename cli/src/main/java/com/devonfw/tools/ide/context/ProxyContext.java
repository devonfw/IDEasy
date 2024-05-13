package com.devonfw.tools.ide.context;

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

  // Getters for proxy configurations
  public ProxyConfig getHttpProxyConfig() {

    return httpProxyConfig;
  }

  public ProxyConfig getHttpsProxyConfig() {

    return httpsProxyConfig;
  }
}

