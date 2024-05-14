package com.devonfw.tools.ide.context;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public class ProxyContext {

  private final IdeContext context;

  private static final String HTTP_PROXY = "http_proxy";

  private static final String HTTPS_PROXY = "https_proxy";

  public static final String PROXY_DOCUMENTATION_PAGE = "https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc";

  public static final String PROXY_FORMAT_WARNING_MESSAGE =
      "Please note that IDEasy can detect a proxy only if the corresponding environmental variables are properly formatted. " + "For further details, see "
          + PROXY_DOCUMENTATION_PAGE;

  private ProxyConfig httpProxyConfig;

  private ProxyConfig httpsProxyConfig;

  public ProxyContext(IdeContext context) {

    this.context = context;

    httpProxyConfig = new ProxyConfig(HTTP_PROXY, context);
    httpsProxyConfig = new ProxyConfig(HTTPS_PROXY, context);
  }

  public Proxy getProxy(String url) {

    ProxyConfig proxyConfig = getProxyConfig(url);
    if (proxyConfig != null) {
      String proxyHost = proxyConfig.getHost();
      int proxyPort = proxyConfig.getPort();

      if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0 && proxyPort <= 65535) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
      }
    }
    return Proxy.NO_PROXY;
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
      this.context.warning(ProxyContext.PROXY_FORMAT_WARNING_MESSAGE);
    }
    return null; // Unsupported protocol or invalid URL
  }
  
}

