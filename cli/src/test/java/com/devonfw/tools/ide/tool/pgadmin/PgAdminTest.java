package com.devonfw.tools.ide.tool.pgadmin;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link PgAdmin}.
 */
class PgAdminTest extends AbstractIdeContextTest {

  /**
   * Tests that the pgAdmin binary name matches the target operating system.
   *
   * @param os the operating system to simulate.
   * @param binaryName the expected binary name.
   */
  @ParameterizedTest
  @CsvSource({ "windows, pgadmin4", "linux, pgadmin4", "mac, pgAdmin 4" })
  void testGetBinaryName(String os, String binaryName) {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setSystemInfo(SystemInfoMock.of(os));
    PgAdmin pgAdmin = new PgAdmin(context);

    // act & assert
    assertThat(pgAdmin.getBinaryName()).isEqualTo(binaryName);
  }

}
