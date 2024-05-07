package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

/** Integration test of {@link EditionListCommandlet}. */
public class EditionListCommandletTest extends AbstractIdeContextTest {

  /** Test of {@link EditionListCommandlet} run. */
  @Test
  public void testEditionListCommandletRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    EditionListCommandlet editionList = context.getCommandletManager().getCommandlet(EditionListCommandlet.class);
    editionList.tool.setValueAsString("mvn", context);

    // act
    editionList.run();

    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "mvn");
    assertLogMessage(context, IdeLogLevel.INFO, "secondMvnEdition");
  }
}