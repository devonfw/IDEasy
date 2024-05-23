package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

/** Integration test of {@link EditionSetCommandlet}. */
public class EditionSetCommandletTest extends AbstractIdeContextTest {

  /** Test of {@link VersionSetCommandlet} run. */
  @Test
  public void testEditionSetCommandletRun() {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC);
    EditionSetCommandlet editionSet = context.getCommandletManager().getCommandlet(EditionSetCommandlet.class);
    editionSet.tool.setValueAsString("mvn", context);
    editionSet.edition.setValueAsString("setEdition", context);

    // act
    editionSet.run();

    // assert
    List<String> logs = ((IdeTestContext) context).level(IdeLogLevel.WARNING).getMessages();
    assertThat(logs).containsExactly("Edition setEdition seems to be invalid");
    Path settingsIdeProperties = context.getSettingsPath().resolve("ide.properties");
    assertThat(settingsIdeProperties).hasContent("""
        #********************************************************************************
        # This file contains project specific environment variables
        #********************************************************************************

        JAVA_VERSION=17*
        MVN_VERSION=3.9.*
        ECLIPSE_VERSION=2023-03
        INTELLIJ_EDITION=ultimate

        IDE_TOOLS=mvn,eclipse

        BAR=bar-${SOME}

        TEST_ARGS1=${TEST_ARGS1} settings1
        TEST_ARGS4=${TEST_ARGS4} settings4
        TEST_ARGS5=${TEST_ARGS5} settings5
        TEST_ARGS6=${TEST_ARGS6} settings6
        TEST_ARGS7=${TEST_ARGS7} settings7
        TEST_ARGS8=settings8
        TEST_ARGS9=settings9
        TEST_ARGSb=${TEST_ARGS10} settingsb ${TEST_ARGSa} ${TEST_ARGSb}
        TEST_ARGSc=${TEST_ARGSc} settingsc
        MVN_EDITION=setEdition""");
  }
}