package com.devonfw.tools.ide.url.model.file.json;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.url.model.AbstractUrlModelTest;
import com.devonfw.tools.ide.url.model.folder.AbstractUrlToolOrEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Test of {@link ToolDependencies} and {@link AbstractUrlToolOrEdition#getDependencyFile()}.
 */
class ToolDependenciesTest extends AbstractUrlModelTest {

  @Test
  void testEditionSpecific() {

    // arrange
    IdeContext context = newContext();

    // act
    Collection<ToolDependency> dependencies = context.getDefaultToolRepository()
        .findDependencies("tomcat", "tomcat", VersionIdentifier.of("11.0.0"));

    // assert
    assertThat(dependencies).containsExactly(new ToolDependency("java", VersionRange.of("[17,)")));
  }

  @Test
  void testEditionFallback() {

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
  void testEditionUnspecific() {

    // arrange
    IdeContext context = newContext();

    // act
    Collection<ToolDependency> dependencies = context.getDefaultToolRepository()
        .findDependencies("mvn", "undefined", VersionIdentifier.of("3.9.0"));

    // assert
    assertThat(dependencies).containsExactly(new ToolDependency("java", VersionRange.of("[8,)")));
  }

  @Test
  void testDependencyFilteringByOsAndArch() {

    // arrange
    IdeContext context = newContext();
    ToolDependencies dependencies = context.getUrls().getEdition("fixtures", "fixtures").getDependencyFile()
        .getDependencies();

    // act
    Collection<ToolDependency> macArmDependencies = dependencies.findDependencies(VersionIdentifier.of("1.5"),
        new SystemInfoImpl("Mac OS X", "13.0", "arm64"));
    Collection<ToolDependency> windowsX64Dependencies = dependencies.findDependencies(VersionIdentifier.of("1.5"),
        new SystemInfoImpl("Windows 11", "11.0", "amd64"));
    Collection<ToolDependency> linuxArmDependencies = dependencies.findDependencies(VersionIdentifier.of("1.5"),
        new SystemInfoImpl("Linux", "5.15", "arm64"));

    // assert
    assertThat(macArmDependencies).extracting(ToolDependency::tool)
        .containsExactly("global", "mac-only", "arm-only", "mac-arm");
    assertThat(windowsX64Dependencies).extracting(ToolDependency::tool)
        .containsExactly("global", "x64-only");
    assertThat(linuxArmDependencies).extracting(ToolDependency::tool)
        .containsExactly("global", "linux-only", "arm-only");

  }
}
