package com.devonfw.tools.ide.cli;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Ideasy}.
 */
class IdeasyTest extends AbstractIdeContextTest {

  /**
   * Test of {@link Ideasy#run(String...)} so that {@link CliExitException} is thrown and ensure it is not logged.
   */
  @Test
  void testEnvOutsideProjectDoesNotLogCliExitException() {

    // arrange
    IdeTestContext context = newContext(Path.of("/"));
    Ideasy ideasy = new Ideasy(context);

    // act
    ideasy.run("--debug", "env");

    // assert
    assertThat(context).logAtDebug().hasMessage("Step 'ide' ended with failure.");
    assertThat(context).log().hasNoEntryWithException();
  }

}
