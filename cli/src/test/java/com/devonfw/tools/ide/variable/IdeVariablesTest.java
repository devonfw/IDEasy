package com.devonfw.tools.ide.variable;

import java.net.http.HttpClient.Version;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

/**
 * Test of {@link IdeVariables}.
 */
public class IdeVariablesTest extends AbstractIdeContextTest {

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

  /** Test of {@link IdeVariables#HTTP_VERSIONS}. */
  @Test
  public void testHttpProtocols() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    // act
    List<Version> httpVersionsEmpty = IdeVariables.HTTP_VERSIONS.get(context);
    List<Version> httpVersions2_11 = IdeVariables.HTTP_VERSIONS.fromString("HTTP_2, http_1_1", context);
    // assert
    assertThat(httpVersionsEmpty).isEmpty();
    assertThat(httpVersions2_11).containsExactly(Version.HTTP_2, Version.HTTP_1_1);
  }

  /** Test of {@link IdeVariables#IDE_TOOLS} with improper bash array syntax (commas instead of spaces). */
  @Test
  public void testIdeToolsWithCommasInBashArray() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    // act - using bash array syntax with commas (improper format)
    List<String> ideTools = IdeVariables.IDE_TOOLS.fromString("(java, maven, python, node)", context);
    // assert - should still parse but with warning logged
    assertThat(ideTools).containsExactly("java,", "maven,", "python,", "node");
    // verify warning was logged
    assertThat(context).logAtWarning().hasMessageContaining("Detected comma in bash array");
  }

}
