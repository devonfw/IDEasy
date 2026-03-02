package com.devonfw.tools.ide.variable;

import java.net.http.HttpClient.Version;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

/**
 * Test of {@link IdeVariables}.
 */
class IdeVariablesTest extends Assertions {

  /** Test of {@link IdeVariables#IDE_TOOLS}. */
  @Test
  void testIdeTools() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    // act
    List<String> ideTools = IdeVariables.IDE_TOOLS.get(context);
    // assert
    assertThat(ideTools).containsExactly("mvn", "npm");
  }

  /** Test of {@link IdeVariables#HTTP_VERSIONS}. */
  @Test
  void testHttpProtocols() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    // act
    List<Version> httpVersionsEmpty = IdeVariables.HTTP_VERSIONS.get(context);
    List<Version> httpVersions2_11 = IdeVariables.HTTP_VERSIONS.fromString("HTTP_2, http_1_1", context);
    // assert
    assertThat(httpVersionsEmpty).isEmpty();
    assertThat(httpVersions2_11).containsExactly(Version.HTTP_2, Version.HTTP_1_1);
  }

  /** Test of {@link IdeVariables#IDE_TOOLS} with bash array syntax using commas. */
  @Test
  void testIdeToolsWithCommasInBashArray() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    // act - using bash array syntax with commas (supported for convenience)
    List<String> ideTools = IdeVariables.IDE_TOOLS.fromString("(java, maven, python, node)", context);
    // assert - should parse correctly with comma as separator
    assertThat(ideTools).containsExactly("java", "maven", "python", "node");
  }

}
