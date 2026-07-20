package com.devonfw.tools.ide.io;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;

import javax.net.ssl.SSLContext;

/**
 * Factory for {@link HttpClient} instances that are configured consistently across IDEasy. All HTTP communication (tool and plugin downloads, reachability
 * checks, URL updates, ...) has to {@link Redirect#ALWAYS follow redirects} because download URLs are frequently shortened or redirected (e.g.
 * {@code https://aka.ms/...}). Centralizing the configuration here ensures that no code-path accidentally skips redirect handling.
 */
public final class HttpClientFactory {

  private HttpClientFactory() {
    // utility class
  }

  /**
   * @return a new {@link HttpClient.Builder} that {@link Redirect#ALWAYS always follows redirects}.
   */
  public static HttpClient.Builder createBuilder() {
    return HttpClient.newBuilder().followRedirects(Redirect.ALWAYS);
  }

  /**
   * @return a new redirect-following {@link HttpClient} using the default TLS configuration.
   */
  public static HttpClient create() {
    return createBuilder().build();
  }

  /**
   * @param sslContext the {@link SSLContext} to use for TLS connections or {@code null} to use the default configuration.
   * @return a new redirect-following {@link HttpClient} using the given {@link SSLContext}.
   */
  public static HttpClient create(SSLContext sslContext) {
    HttpClient.Builder builder = createBuilder();
    if (sslContext != null) {
      builder.sslContext(sslContext);
    }
    return builder.build();
  }
}
