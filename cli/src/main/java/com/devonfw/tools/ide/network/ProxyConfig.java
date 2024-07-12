package com.devonfw.tools.ide.network;

import java.net.MalformedURLException;
import java.net.URL;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Class responsible for parsing and storing the host and port information from a given proxy URL.
 */
public class ProxyConfig {

  private final IdeContext context;

  private String host;

  private int port;

  ProxyConfig(String proxyUrl, IdeContext context) {

    this.context = context;

    try {
      URL url = new URL(proxyUrl);
      this.host = url.getHost();
      this.port = url.getPort();
    } catch (MalformedURLException e) {
      this.context.warning(ProxyContext.PROXY_FORMAT_WARNING_MESSAGE);
    }
  }

  /**
   * @return a {@link String} representing the host of the proxy
   */
  public String getHost() {

    return this.host;
  }

  /**
   * @return an {@code int} representing the port of the proxy
   */
  public int getPort() {

    return this.port;
  }
}
