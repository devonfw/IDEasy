package com.devonfw.tools.ide.network;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import org.junit.jupiter.api.Test;

import java.net.Proxy;

public class ProxyContextTest extends AbstractIdeContextTest {
  @Test
  public void testNoProxy() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    // act
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);

    Proxy proxy = context.getProxyContext().getProxy("https://example.com");

    assertThat(proxy).isEqualTo(Proxy.NO_PROXY);
  }
}
