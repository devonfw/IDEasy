package com.devonfw.tools.ide.network;

import com.devonfw.tools.ide.context.IdeContext;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public class ProxyContext {

  private final IdeContext context;

  private static final String HTTP_PROXY = "http_proxy";

  private static final String HTTPS_PROXY = "httpss_proxy";

  private static final String PROXY_DOCUMENTATION_PAGE = "https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc";

  static final String PROXY_FORMAT_WARNING_MESSAGE =
      "Proxy configuration detected, but the formatting appears to be incorrect. Proxy configuration will be skipped.\n"
          + "Please note that IDEasy can detect a proxy only if the corresponding environmental variables are properly formatted. "
          + "For further details, see " + PROXY_DOCUMENTATION_PAGE;

  final private ProxyConfig httpProxyConfig;

  final private ProxyConfig httpsProxyConfig;

  public ProxyContext(IdeContext context) {

    this.context = context;
    this.httpProxyConfig = initializeProxyConfig(HTTP_PROXY);
    this.httpsProxyConfig = initializeProxyConfig(HTTPS_PROXY);
  }

  private ProxyConfig initializeProxyConfig(String proxyEnvVariable) {

    String proxyUrl = System.getenv(proxyEnvVariable);
    if (proxyUrl == null) {
      proxyUrl = System.getenv(proxyEnvVariable.toUpperCase());
    }
    return (proxyUrl != null && !proxyUrl.isEmpty()) ? new ProxyConfig(proxyUrl, context) : null;
  }

  public Proxy getProxy(String url) {

    ProxyConfig proxyConfig = getProxyConfig(url);
    if (proxyConfig != null) {
      String proxyHost = proxyConfig.getHost();
      int proxyPort = proxyConfig.getPort();

      if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0 && proxyPort <= 65535) {
        InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);
        if (proxyAddress.isUnresolved()) {
          this.context.warning(ProxyContext.PROXY_FORMAT_WARNING_MESSAGE + " CONFIG");
          return Proxy.NO_PROXY;
        }
        return new Proxy(Proxy.Type.HTTP, proxyAddress);
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
      this.context.warning(ProxyContext.PROXY_FORMAT_WARNING_MESSAGE + " CONTEXT");
    }
    return null; // Unsupported protocol or invalid URL
  }

}

