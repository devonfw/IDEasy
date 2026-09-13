package com.devonfw.tools.ide.tool.pgadmin;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link PgAdmin}.
 */
class PgAdminTest extends AbstractIdeContextTest {

  /**
   * Verifies that the macOS application bundle name reported for uninstalling is {@code "pgAdmin 4"}, matching the actual name of the bundle shipped by the
   * official pgAdmin4 macOS installer.
   */
  @Test
  void testGetMacApplicationName() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    PgAdmin pgAdmin = new PgAdmin(context);

    // act + assert
    assertThat(pgAdmin.getMacApplicationName()).isEqualTo("pgAdmin 4");
  }

}
