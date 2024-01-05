package com.devonfw.tools.ide.tool.mvn;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Mvn}.
 */
public class MvnTest extends AbstractIdeContextTest {

  @Test
  public void testMvnPostInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, "", true);
    Mvn mvn = (Mvn) context.getCommandletManager().getToolCommandlet("mvn");
    Path pluginActive = context.getSoftwarePath().resolve("mvn").resolve("lib/ext/maven-notifier.jar");
    Path pluginInactive = context.getSoftwarePath().resolve("mvn").resolve("lib/ext/maven-profiler.jar");
    // act
    mvn.postInstall();
    // assert
    assertThat(pluginActive).exists();
    assertThat(pluginInactive).doesNotExist();
  }
}
