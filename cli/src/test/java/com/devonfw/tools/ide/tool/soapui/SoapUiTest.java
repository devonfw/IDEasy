package com.devonfw.tools.ide.tool.soapui;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link SoapUi}.
 */
@WireMockTest
public class SoapUiTest extends AbstractIdeContextTest {

  private static final String PROJECT_SOAPUI = "soapui";

  private static final String SOAPUI_VERSION = "5.10.0";

  @Test
  void testSoapUiInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SOAPUI, wireMockRuntimeInfo);
    SoapUi soapUiCommandlet = context.getCommandletManager().getCommandlet(SoapUi.class);

    // act
    soapUiCommandlet.install();

    // assert
    assertThat(context.getSoftwarePath().resolve("soapui/.ide.software.version")).exists().hasContent(SOAPUI_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed soapui in version " + SOAPUI_VERSION);
  }
}
