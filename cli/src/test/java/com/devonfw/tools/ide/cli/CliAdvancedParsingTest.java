package com.devonfw.tools.ide.cli;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link com.devonfw.tools.ide.context.AbstractIdeContext#run(CliArguments) CLI parsing}.
 */
public class CliAdvancedParsingTest extends AbstractIdeContextTest {

  private static final String PROJECT_MVN = "mvn";

  /**
   * Test that implicit end-options is triggered for multi-valued arguments to prevent splitting odd-formatted short-options like "-version".
   */
  @Test
  public void testPreventShortOptionsForMultivaluedArguments() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    CliArguments args = new CliArguments("java", "-version");
    args.next();
    // act
    context.run(args);
    // assert
    assertThat(context).logAtInfo().hasNoMessage("java -v -e -r -s -i -o -n").hasMessage("java -version");
  }
}
