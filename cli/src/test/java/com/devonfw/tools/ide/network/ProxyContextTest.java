package com.devonfw.tools.ide.network;

import java.net.Proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/**
 * Test of {@link ProxyContext} and {@link ProxyConfig}.
 */
@ExtendWith(SystemStubsExtension.class)
public class ProxyContextTest extends AbstractIdeContextTest {

  private static final String PROJECT_PATH = "project/workspaces/foo-test/my-git-repo";

  private static final String HTTP_PROXY = "http://127.0.0.1:8888";

  private static final String HTTPS_PROXY = "https://127.0.0.1:8888";

  private static final String HTTP_PROXY_NO_HOST = "http://:8888";
  private static final String HTTP_PROXY_WRONG_HOST = "http://127.0.0.1wrongwrong:8888";

  private static final String HTTP_PROXY_WRONG_PROTOCOL = "wrong://127.0.0.1:8888";

  private static final String HTTP_PROXY_WRONG_FORMAT = "http://127.0.0.1:8888:wrong:wrong";

  private static final String PROXY_DOCUMENTATION_PAGE = "https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc";

  static final String PROXY_FORMAT_WARNING_MESSAGE =
      "Proxy configuration detected, but the formatting appears to be incorrect. Proxy configuration will be skipped.\n"
          + "Please note that IDEasy can detect a proxy only if the corresponding environmental variables are properly formatted. "
          + "For further details, see " + PROXY_DOCUMENTATION_PAGE;

  /**
   * Verifies that when the download URL is malformed, {@link ProxyContext#getProxy(String)} returns {@link Proxy#NO_PROXY}.
   */
  @Test
  public void testNoProxyMalformedUrl() {

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);
    Proxy proxy = context.getProxyContext().getProxy("htt:p//example.com");

    // assert
    assertThat(proxy).isEqualTo(Proxy.NO_PROXY);
  }

  /**
   * Verifies that in an environment where no proxy variables are set, {@link ProxyContext#getProxy(String)} returns {@link Proxy#NO_PROXY}.
   */
  @Test
  public void testNoProxy() {

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);
    Proxy proxy = context.getProxyContext().getProxy("https://example.com");

    // assert
    assertThat(proxy).isEqualTo(Proxy.NO_PROXY);
  }

  @SystemStub
  private final EnvironmentVariables environment = new EnvironmentVariables();

  /**
   * Verifies that in an environment where a http proxy variable is set, {@link ProxyContext#getProxy(String)} returns a correctly configured {@link Proxy}
   * object.
   */
  @Test
  public void testWithMockedHttpVar() {

    // arrange
    this.environment.set("HTTP_PROXY", HTTP_PROXY);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);
    Proxy proxy = context.getProxyContext().getProxy("http://example.com");

    // assert
    assertThat("http:/" + proxy.address().toString()).isEqualTo(HTTP_PROXY);
    assertThat(proxy.type()).isEqualTo(Proxy.Type.HTTP);
  }

  /**
   * Verifies that in an environment where a http proxy variable (lowercase) is set, {@link ProxyContext#getProxy(String)} returns a correctly configured
   * {@link Proxy} object.
   */
  @Test
  public void testWithMockedHttpVarLowercase() {

    // arrange
    this.environment.set("http_proxy", HTTP_PROXY);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);
    Proxy proxy = context.getProxyContext().getProxy("http://example.com");

    // assert
    assertThat("http:/" + proxy.address().toString()).isEqualTo(HTTP_PROXY);
    assertThat(proxy.type()).isEqualTo(Proxy.Type.HTTP);
  }

  /**
   * Verifies that in an environment where a https proxy variable is set, {@link ProxyContext#getProxy(String)} returns a correctly configured {@link Proxy}
   * object.
   */
  @Test
  public void testWithMockedHttpsVar() {

    // arrange
    this.environment.set("HTTPS_PROXY", HTTPS_PROXY);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);
    Proxy proxy = context.getProxyContext().getProxy("https://example.com");

    // assert
    assertThat("https:/" + proxy.address().toString()).isEqualTo(HTTPS_PROXY);
    assertThat(proxy.type()).isEqualTo(Proxy.Type.HTTP);
  }

  /**
   * Verifies that in an environment where a http proxy variable is wrongly formatted, {@link ProxyContext#getProxy(String)} returns {@link Proxy#NO_PROXY}. A
   * warning message is displayed.
   */
  @Test
  public void testWithMockedHttpVarWrongFormat() {

    // arrange
    this.environment.set("HTTP_PROXY", HTTP_PROXY_WRONG_FORMAT);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);
    Proxy proxy = context.getProxyContext().getProxy("http://example.com");

    // assert
    assertThat(proxy).isEqualTo(Proxy.NO_PROXY);
    assertThat(context).logAtWarning().hasMessage(PROXY_FORMAT_WARNING_MESSAGE);
  }

  /**
   * Verifies that in an environment where a http proxy variable is wrongly formatted, i.e. the host is empty, {@link ProxyContext#getProxy(String)} returns
   * {@link Proxy#NO_PROXY}.
   */
  @Test
  public void testWithMockedHttpVarNoHost() {

    // arrange
    this.environment.set("HTTP_PROXY", HTTP_PROXY_NO_HOST);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);
    Proxy proxy = context.getProxyContext().getProxy("http://example.com");

    // assert
    assertThat(proxy).isEqualTo(Proxy.NO_PROXY);
  }

  /**
   * Verifies that in an environment where a http proxy variable is wrongly formatted, {@link ProxyContext#getProxy(String)} returns {@link Proxy#NO_PROXY}. A
   * warning message is displayed.
   */
  @Test
  public void testWithMockedHttpVarWrongHost() {

    // arrange
    this.environment.set("HTTP_PROXY", HTTP_PROXY_WRONG_HOST);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);
    Proxy proxy = context.getProxyContext().getProxy("http://example.com");

    // assert
    assertThat(proxy).isEqualTo(Proxy.NO_PROXY);
    assertThat(context).logAtWarning().hasMessage(PROXY_FORMAT_WARNING_MESSAGE);
  }

  /**
   * Verifies that in an environment where a http proxy variable is wrongly formatted, {@link ProxyContext#getProxy(String)} returns {@link Proxy#NO_PROXY}. A
   * warning message is displayed.
   */
  @Test
  public void testWithMockedHttpVarWrongProtocol() {

    // arrange
    this.environment.set("HTTP_PROXY", HTTP_PROXY_WRONG_PROTOCOL);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);
    Proxy proxy = context.getProxyContext().getProxy("http://example.com");

    // assert
    assertThat(proxy).isEqualTo(Proxy.NO_PROXY);
    assertThat(context).logAtWarning().hasMessage(PROXY_FORMAT_WARNING_MESSAGE);
  }

}
