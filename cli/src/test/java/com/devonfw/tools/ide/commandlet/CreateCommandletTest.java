package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class CreateCommandletTest extends AbstractIdeContextTest {

  private final String NEW_PROJECT_NAME = "newProject";

  @Test
  public void testCreateCommandletRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    CreateCommandlet cc = context.getCommandletManager().getCommandlet(CreateCommandlet.class);
    cc.newProject.setValueAsString(NEW_PROJECT_NAME, context);
    // act
    cc.run();
    // assert
    Path newProjectPath = context.getIdeRoot().resolve(NEW_PROJECT_NAME);
    assertThat(newProjectPath).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_PLUGINS)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_SOFTWARE)).exists();
    assertThat(newProjectPath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN)).exists();
  }
}
