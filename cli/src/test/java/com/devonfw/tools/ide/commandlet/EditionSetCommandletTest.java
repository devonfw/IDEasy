package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesFiles;

/** Test of {@link EditionSetCommandlet}. */
class EditionSetCommandletTest extends AbstractIdeContextTest {

  /** Test of {@link VersionSetCommandlet} run. */
  @Test
  void testEditionSetCommandletRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    EditionSetCommandlet editionSet = context.getCommandletManager().getCommandlet(EditionSetCommandlet.class);
    editionSet.tool.setValueAsString("mvn", context);
    editionSet.edition.setValueAsString("setEdition", context);

    // act
    editionSet.run();

    // assert
    assertThat(context).logAtWarning().hasMessage("Edition setEdition seems to be invalid");
    Path settingsIdeProperties = context.getSettingsPath().resolve("ide.properties");
    assertThat(settingsIdeProperties).hasContent("""
        #********************************************************************************
        # This file contains project specific environment variables
        #********************************************************************************
        JAVA_VERSION=17*
        MVN_VERSION=3.9.0
        ECLIPSE_VERSION=2023-03
        INTELLIJ_EDITION=ultimate
        IDE_TOOLS=mvn,eclipse
        BAR=bar-${SOME}
        MVN_EDITION=setEdition""");
  }

  @Test
  void testEditionSetCommandletRunOverridenEdition() throws IOException {

    IdeTestContext context = newContext(PROJECT_BASIC);

    //arrange
    Path settingsIdeProperties = context.getSettingsPath().resolve("ide.properties");
    Path confIdeProperties = context.getConfPath().resolve("ide.properties");
    EditionSetCommandlet editionSet = context.getCommandletManager().getCommandlet(EditionSetCommandlet.class);

    // act
    editionSet.tool.setValueAsString("mvn", context);
    editionSet.cfg.setValue(EnvironmentVariablesFiles.CONF);
    editionSet.edition.setValueAsString("confSetEdition", context);
    editionSet.run();

    //act again
    editionSet.cfg.clearValue();
    editionSet.edition.clearValue();
    editionSet.edition.setValueAsString("settingsSetEdition", context);
    editionSet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("MVN_EDITION=confSetEdition");
    assertThat(context).logAtInfo().hasMessageContaining("MVN_EDITION=settingsSetEdition");
    assertThat(context).logAtWarning().hasMessageContaining("The variable MVN_EDITION is overridden");

    String content = Files.readString(confIdeProperties, StandardCharsets.UTF_8);
    assertThat(content).contains("MVN_EDITION=confSetEdition");
    content = Files.readString(settingsIdeProperties, StandardCharsets.UTF_8);
    assertThat(content).contains("MVN_EDITION=settingsSetEdition");
  }
}
