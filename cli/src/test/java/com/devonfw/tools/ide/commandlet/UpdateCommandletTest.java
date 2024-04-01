package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class UpdateCommandletTest extends AbstractIdeContextTest {

  private final String PROJECT_UPDATE = "update";

  @Test
  public void testRunPullSettings() {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    UpdateCommandlet uc = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);
    // act
    uc.run();

    // assert
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully updated settings repository.");
    assertThat(Files.exists(context.getConfPath()));
    assertThat(Files.exists(context.getSettingsPath().resolve("java")));
    assertThat(Files.exists(context.getSettingsPath().resolve("mvn")));
    assertLogMessage(context, IdeLogLevel.SUCCESS, "All 2 steps ended successfully!");
  }

  @Test
  public void testRunTemplatesNotFound() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_UPDATE);
    UpdateCommandlet uc = context.getCommandletManager().getCommandlet(UpdateCommandlet.class);
    deleteTemplatesFolder(context);

    // act
    uc.run();

    // assert
    assertLogMessage(context, IdeLogLevel.WARNING, "Templates folder is missing in settings folder.");
  }

  private void deleteTemplatesFolder(IdeContext context) throws IOException {
    
    Path templates = context.getSettingsPath().resolve(IdeContext.FOLDER_TEMPLATES).resolve(IdeContext.FOLDER_CONF);
    Files.delete(templates);
    Files.delete(templates.getParent());
  }
}