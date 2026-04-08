package com.devonfw.tools.ide.tool.squirrelsql;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link SquirrelSql}
 */
@WireMockTest
public class SquirrelSqlTest extends AbstractIdeContextTest {

  private static final String PROJECT_SQUIRREL_SQL = "squirrelsql";
  private static final String SQUIRREL_SQL_VERSION = "5.1.0";

  @Test
  void testSquirrelSqlInstallAndRun(WireMockRuntimeInfo wireMockRuntimeInfo) {
    // arrange
    IdeTestContext context = newContext(PROJECT_SQUIRREL_SQL, wireMockRuntimeInfo);
    SquirrelSql squirrelSql = context.getCommandletManager().getCommandlet(SquirrelSql.class);

    // act
    squirrelSql.run();

    // assert
    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
    assertThat(context.getSoftwarePath().resolve("squirrelsql/.ide.software.version")).exists().hasContent(SQUIRREL_SQL_VERSION);
    assertThat(context.getSoftwarePath().resolve("squirrelsql/squirrelsql.sh")).exists();
    assertThat(context.getSoftwarePath().resolve("squirrelsql/squirrelsql.bat")).exists();
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed squirrelsql in version " + SQUIRREL_SQL_VERSION);
  }
}
