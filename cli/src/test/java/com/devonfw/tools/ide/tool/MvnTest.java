package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.mvn.Mvn;

/**
 * Test of {@link Mvn}.
 */
public class MvnTest extends AbstractIdeContextTest {

  @Test
  public void testMvnPostInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, "", false);
    ToolCommandlet mvn = context.getCommandletManager().getToolCommandlet("mvn");
    Path mavenPlugins = context.getSoftwarePath().resolve("mvn").resolve("lib/ext/maven-notifier.jar");
    // act
    mvn.postInstall();
    // assert
    assertThat(Files.exists(mavenPlugins)).isEqualTo(true);

  }
}
