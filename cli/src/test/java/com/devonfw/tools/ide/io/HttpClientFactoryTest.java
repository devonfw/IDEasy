package com.devonfw.tools.ide.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Test;

/**
 * Test of {@link HttpClientFactory}.
 */
class HttpClientFactoryTest {

  @Test
  void testCreateAlwaysFollowsRedirects() {

    try (HttpClient client = HttpClientFactory.create()) {
      assertThat(client.followRedirects()).isEqualTo(Redirect.ALWAYS);
    }
  }

  @Test
  void testCreateWithSslContextAlwaysFollowsRedirects() throws Exception {

    SSLContext sslContext = SSLContext.getDefault();
    try (HttpClient client = HttpClientFactory.create(sslContext)) {
      assertThat(client.followRedirects()).isEqualTo(Redirect.ALWAYS);
      assertThat(client.sslContext()).isSameAs(sslContext);
    }
  }

  @Test
  void testCreateWithNullSslContextUsesDefaultAndFollowsRedirects() {

    try (HttpClient client = HttpClientFactory.create(null)) {
      assertThat(client).isNotNull();
      assertThat(client.followRedirects()).isEqualTo(Redirect.ALWAYS);
    }
  }
}
