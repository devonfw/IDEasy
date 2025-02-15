package com.devonfw.tools.ide.url.model.file.json;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.model.AbstractUrlModelTest;
import com.devonfw.tools.ide.url.model.folder.AbstractUrlToolOrEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Test of {@link ToolDependencies} and {@link AbstractUrlToolOrEdition#getDependencyFile()}.
 */
public class ToolDependenciesTest extends AbstractUrlModelTest {


  @Test
  public void testEditionSpecific() {

    // arrange
    IdeContext context = newContext();

    // act
    Collection<ToolDependency> dependencies = context.getDefaultToolRepository()
        .findDependencies("tomcat", "tomcat", VersionIdentifier.of("11.0.0"));

    // assert
    assertThat(dependencies).containsExactly(new ToolDependency("java", VersionRange.of("[17,)")));
  }

  @Test
  public void testEditionFallback() {

    // arrange
    IdeContext context = newContext();

    // act
    Collection<ToolDependency> dependencies = context.getDefaultToolRepository()
        .findDependencies("tomcat", "undefined", VersionIdentifier.of("11.0.0"));

    // assert
    assertThat(dependencies).containsExactly(
        new ToolDependency("this-is-the-wrong-file-only-for-testing", VersionRange.of("[1.0,2.0]")));
  }

  @Test
  public void testEditionUnspecific() {

    // arrange
    IdeContext context = newContext();

    // act
    Collection<ToolDependency> dependencies = context.getDefaultToolRepository()
        .findDependencies("mvn", "undefined", VersionIdentifier.of("3.9.0"));

    // assert
    assertThat(dependencies).containsExactly(new ToolDependency("java", VersionRange.of("[8,)")));
  }
}
