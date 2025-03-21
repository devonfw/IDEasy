package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link StatusCommandlet}.
 */
public class StatusCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BASIC = "basic";

  @Test
  public void testStatusOutsideOfHome() {
    //arrange
    IdeTestContext context = new IdeTestContext();
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    //act
    status.run();

    //assert
    assertThat(context).logAtWarning().hasMessageContaining("You are not inside an IDE project: ");
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run without internet connection.
   */
  @Test
  public void testStatusWhenOffline() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setOnline(false);
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).logAtWarning().hasMessage("Check for newer version of IDEasy is skipped due to no network connectivity.");
  }
}
