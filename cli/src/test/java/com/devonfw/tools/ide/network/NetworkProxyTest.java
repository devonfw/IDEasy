package com.devonfw.tools.ide.network;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link NetworkProxy}.
 */
class NetworkProxyTest extends AbstractIdeContextTest {

  private static final String PROXY_DOCUMENTATION_PAGE = "https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc";

  /**
   * Verifies that when the HTTP_PROXY variable contains a malformed URL, {@link NetworkProxy#configure()} does not configure proxy properties.
   */
  @Test
  void testHttpProxyMalformedUrl() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    NetworkProxy networkProxy = new NetworkProxy(context);
    String invalidUrl = "htt:p//example.com";
    context.getSystem().setEnv("HTTP_PROXY", invalidUrl);
    context.getSystem().setEnv("no_proxy", ".foo.com,localhost");

    // act
    networkProxy.configure();

    // assert
    assertThat(context.getSystem().getProperties()).isEmpty();
    assertThat(context).logAtWarning().hasMessageContaining("Invalid http proxy configuration detected with URL " + invalidUrl + ".");
    assertThat(context).logAtWarning().hasMessageContaining(PROXY_DOCUMENTATION_PAGE);
  }

  /**
   * Verifies that in an environment where no proxy variables are set, {@link NetworkProxy#configure()} does not configure proxy properties.
   */
  @Test
  void testNoProxy() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    NetworkProxy networkProxy = new NetworkProxy(context);

    // act
    networkProxy.configure();

    // assert
    assertThat(context.getSystem().getProperties()).isEmpty();
  }

  /**
   * Verifies that in an environment where a HTTP_PROXY variable is set, {@link NetworkProxy#configure()} configures the according Java proxy properties.
   */
  @Test
  void testHttpProxyUpperCase() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    NetworkProxy networkProxy = new NetworkProxy(context);
    context.getSystem().setEnv("HTTP_PROXY", "http://proxy.host.com:8888");
    String noProxy = ".foo.com,localhost";
    context.getSystem().setEnv("NO_PROXY", noProxy);

    // act
    networkProxy.configure();

    // assert
    assertThat(context.getSystem().getProperty("http.proxyHost")).isEqualTo("proxy.host.com");
    assertThat(context.getSystem().getProperty("http.proxyPort")).isEqualTo("8888");
    assertThat(context.getSystem().getProperty("http.nonProxyHosts")).isEqualTo(noProxy);
    assertThat(context.getSystem().getProperties()).hasSize(3);
  }

  /**
   * Verifies that in an environment where a http_proxy variable is set, {@link NetworkProxy#configure()} configures the according Java proxy properties.
   */
  @Test
  void testHttpProxyLowercase() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    NetworkProxy networkProxy = new NetworkProxy(context);
    context.getSystem().setEnv("http_proxy", "http://proxy.host.com:8888");
    String noProxy = ".foo.com,localhost";
    context.getSystem().setEnv("no_proxy", noProxy);

    // act
    networkProxy.configure();

    // assert
    assertThat(context.getSystem().getProperty("http.proxyHost")).isEqualTo("proxy.host.com");
    assertThat(context.getSystem().getProperty("http.proxyPort")).isEqualTo("8888");
    assertThat(context.getSystem().getProperty("http.nonProxyHosts")).isEqualTo(noProxy);
    assertThat(context.getSystem().getProperties()).hasSize(3);
  }

  /**
   * Verifies that in an environment where a HTTPS_PROXY variable is set, {@link NetworkProxy#configure()} configures the according Java proxy properties.
   * object.
   */
  @Test
  void testHttpsProxyUpperCase() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    NetworkProxy networkProxy = new NetworkProxy(context);
    context.getSystem().setEnv("HTTPS_PROXY", "https://secure.proxy.com:8443");
    String noProxy = ".foo.com,localhost";
    context.getSystem().setEnv("NO_PROXY", noProxy);

    // act
    networkProxy.configure();

    // assert
    assertThat(context.getSystem().getProperty("https.proxyHost")).isEqualTo("secure.proxy.com");
    assertThat(context.getSystem().getProperty("https.proxyPort")).isEqualTo("8443");
    assertThat(context.getSystem().getProperty("https.nonProxyHosts")).isEqualTo(noProxy);
    assertThat(context.getSystem().getProperties()).hasSize(3);
  }

  /**
   * Verifies that in an environment where an all_proxy variable is set, {@link NetworkProxy#configure()} configures the according Java proxy properties.
   * object.
   */
  @Test
  void testAllProxyLowerCase() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    NetworkProxy networkProxy = new NetworkProxy(context);
    context.getSystem().setEnv("all_proxy", "https://secure.proxy.com");
    String noProxy = ".foo.com,localhost";
    context.getSystem().setEnv("no_proxy", noProxy);

    // act
    networkProxy.configure();

    // assert
    assertThat(context.getSystem().getProperty("http.proxyHost")).isEqualTo("secure.proxy.com");
    assertThat(context.getSystem().getProperty("http.proxyPort")).isEqualTo("443");
    assertThat(context.getSystem().getProperty("http.nonProxyHosts")).isEqualTo(noProxy);
    assertThat(context.getSystem().getProperty("https.proxyHost")).isEqualTo("secure.proxy.com");
    assertThat(context.getSystem().getProperty("https.proxyPort")).isEqualTo("443");
    assertThat(context.getSystem().getProperty("https.nonProxyHosts")).isEqualTo(noProxy);
    assertThat(context.getSystem().getProperties()).hasSize(6);
  }

  /**
   * Verifies that in an environment where an ALL_PROXY variable is set, {@link NetworkProxy#configure()} configures the according Java proxy properties.
   * object.
   */
  @Test
  void testAllProxyUpperCase() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    NetworkProxy networkProxy = new NetworkProxy(context);
    context.getSystem().setEnv("ALL_PROXY", "http://proxy.company.com");
    String noProxy = ".foo.com,localhost";
    context.getSystem().setEnv("no_proxy", noProxy);

    // act
    networkProxy.configure();

    // assert
    assertThat(context.getSystem().getProperty("http.proxyHost")).isEqualTo("proxy.company.com");
    assertThat(context.getSystem().getProperty("http.proxyPort")).isEqualTo("80");
    assertThat(context.getSystem().getProperty("http.nonProxyHosts")).isEqualTo(noProxy);
    assertThat(context.getSystem().getProperty("https.proxyHost")).isEqualTo("proxy.company.com");
    assertThat(context.getSystem().getProperty("https.proxyPort")).isEqualTo("80");
    assertThat(context.getSystem().getProperty("https.nonProxyHosts")).isEqualTo(noProxy);
    assertThat(context.getSystem().getProperties()).hasSize(6);
  }

}
