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

    try {
      URL url = new URL(proxyUrl);
      host = url.getHost();
      port = url.getPort();
    } catch (MalformedURLException e) {
      this.context.warning(ProxyContext.PROXY_FORMAT_WARNING_MESSAGE);
    }
  }

  String getHost() {

    return host;
  }

  int getPort() {

    return port;
  }
}
