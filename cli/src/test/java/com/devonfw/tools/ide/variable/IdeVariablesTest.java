package com.devonfw.tools.ide.variable;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

/**
 * Test of {@link IdeVariables}.
 */
public class IdeVariablesTest extends Assertions {

  /** Test of {@link IdeVariables#IDE_TOOLS}. */
  @Test
  public void testIdeTools() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    // act
    List<String> ideTools = IdeVariables.IDE_TOOLS.get(context);
    // assert
    assertThat(ideTools).containsExactly("mvn", "npm");
  }

  /** Test of {@link IdeVariables#HTTP_PROTOCOLS}. */
  @Test
  public void testHttpProtocols() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    // act
    List<String> ideTools = IdeVariables.HTTP_PROTOCOLS.get(context);
    // assert
    assertThat(ideTools).containsExactly("HTTP_2");
  }

}
