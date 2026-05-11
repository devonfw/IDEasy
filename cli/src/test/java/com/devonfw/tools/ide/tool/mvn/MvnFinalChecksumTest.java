package com.devonfw.tools.ide.tool.mvn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test verifying that the deterministic archives produced by our mock repositories
 * match our hardcoded "Gold Standard" values.
 */
@WireMockTest
class MvnFinalChecksumTest extends AbstractIdeContextTest {

  private static final String PROJECT_MVN = "mvn";



  /**
   * Integration test verifying that the tool installation correctly performs and validates 
   * the checksum from the URL repository (urls.sha256).
   */
  @Test
  void testVerifyUrlChecksum(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // 1. Arrange: Use the "mvn" project context with WireMock
    IdeTestContext context = newContext(PROJECT_MVN, wmRuntimeInfo);
    Mvn mvn = context.getCommandletManager().getCommandlet(Mvn.class);

    // Read the expected hash from the URL repository file in the test resources
    Path urlsSha256Path = context.getIdePath().resolve("urls/mvn/mvn/3.9.7/urls.sha256");
    String expectedSha256 = Files.readString(urlsSha256Path).trim();

    // 2. Act: Trigger the installation of Maven 3.9.7
    // This will use ToolRepositoryMock which triggers deterministic compression
    // and verifies it against urls.sha256 in the test resources.
    mvn.install();

    // 3. Assert: Verify the installation and checksum success logs
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed mvn in version 3.9.7");
    assertThat(context).logAtSuccess().hasMessageContaining("SHA-256 checksum " + expectedSha256 + " is correct.");
  }
}
