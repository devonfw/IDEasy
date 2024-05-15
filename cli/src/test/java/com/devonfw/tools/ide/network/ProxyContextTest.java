package com.devonfw.tools.ide.network;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.net.Proxy;

@ExtendWith(SystemStubsExtension.class)
public class ProxyContextTest extends AbstractIdeContextTest {

  private static final String PROJECT_PATH = "project/workspaces/foo-test/my-git-repo";

  private static final String HTTP_PROXY = "http://127.0.0.1:8888";

  private static final String HTTPS_PROXY = "https://127.0.0.1:8888";

  private static final String HTTP_PROXY_WRONG = "http://127.0.0.1wrongwrong:8888";

  private static final String PROXY_DOCUMENTATION_PAGE = "https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc";

  static final String PROXY_FORMAT_WARNING_MESSAGE =
      "Proxy configuration detected, but the formatting appears to be incorrect. Proxy configuration will be skipped.\n"
          + "Please note that IDEasy can detect a proxy only if the corresponding environmental variables are properly formatted. "
          + "For further details, see " + PROXY_DOCUMENTATION_PAGE;

  @Test
  public void testNoProxy() {

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);

    Proxy proxy = context.getProxyContext().getProxy("https://example.com");

    assertThat(proxy).isEqualTo(Proxy.NO_PROXY);
  }

  @SystemStub
  private EnvironmentVariables environment = new EnvironmentVariables();

  @Test
  public void testWithMockedHttpVar() {

    // arrange
    environment.set("HTTP_PROXY", HTTP_PROXY);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);

    // assert
    Proxy proxy = context.getProxyContext().getProxy("http://example.com");
    assertThat("http:/" + proxy.address().toString()).isEqualTo(HTTP_PROXY);
  }

  @Test
  public void testWithMockedHttpVarLowercase() {

    // arrange
    environment.set("http_proxy", HTTP_PROXY);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);

    // assert
    Proxy proxy = context.getProxyContext().getProxy("http://example.com");
    assertThat("http:/" + proxy.address().toString()).isEqualTo(HTTP_PROXY);
  }

  @Test
  public void testWithMockedHttpsVar() {

    // arrange
    environment.set("HTTPS_PROXY", HTTPS_PROXY);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);

    // assert
    Proxy proxy = context.getProxyContext().getProxy("https://example.com");
    assertThat("https:/" + proxy.address().toString()).isEqualTo(HTTPS_PROXY);
  }

  @Test
  public void testWithMockedHttpVarWrong() {

    // arrange
    environment.set("HTTP_PROXY", HTTP_PROXY_WRONG);

    // act
    IdeTestContext context = newContext(PROJECT_BASIC, PROJECT_PATH, false);

    // assert
    Proxy proxy = context.getProxyContext().getProxy("http://example.com");
    assertThat(proxy).isEqualTo(Proxy.NO_PROXY);
    assertLogMessage(context, IdeLogLevel.WARNING, PROXY_FORMAT_WARNING_MESSAGE);
  }

}
