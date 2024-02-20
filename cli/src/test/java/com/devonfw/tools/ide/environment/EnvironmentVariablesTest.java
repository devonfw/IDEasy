package com.devonfw.tools.ide.environment;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link EnvironmentVariables}.
 */
public class EnvironmentVariablesTest extends AbstractIdeContextTest {

  /**
   * Test of {@link EnvironmentVariables#resolve(String, Object)} with self referencing variables.
   */
  @Test
  public void testProperEvaluationOfVariables() {

    // arrange
    String path = "workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    EnvironmentVariables variables = context.getVariables();

    // act
    String TEST_ARGS1 = variables.get("TEST_ARGS1");
    String TEST_ARGS2 = variables.get("TEST_ARGS2");
    String TEST_ARGS3 = variables.get("TEST_ARGS3");
    String TEST_ARGS4 = variables.get("TEST_ARGS4");
    String TEST_ARGS5 = variables.get("TEST_ARGS5");
    String TEST_ARGS6 = variables.get("TEST_ARGS6");
    String TEST_ARGS7 = variables.get("TEST_ARGS7");
    String TEST_ARGS8 = variables.get("TEST_ARGS8");
    String TEST_ARGS9 = variables.get("TEST_ARGS9");
    String TEST_ARGS10 = variables.get("TEST_ARGS10");
    // some more advanced cases
    String TEST_ARGSa = variables.get("TEST_ARGSa");
    String TEST_ARGSb = variables.get("TEST_ARGSb");
    String TEST_ARGSc = variables.get("TEST_ARGSc");
    String TEST_ARGSd = variables.get("TEST_ARGSd");

    // assert
    assertThat(TEST_ARGS1).isEqualTo(" user1 settings1 workspace1 conf1");
    assertThat(TEST_ARGS2).isEqualTo(" user2 conf2");
    assertThat(TEST_ARGS3).isEqualTo(" user3 workspace3");
    assertThat(TEST_ARGS4).isEqualTo(" settings4");
    assertThat(TEST_ARGS5).isEqualTo(" settings5 conf5");
    assertThat(TEST_ARGS6).isEqualTo(" settings6 workspace6 conf6");

    assertThat(TEST_ARGS7).isEqualTo("user7 settings7 workspace7 conf7");
    assertThat(TEST_ARGS8).isEqualTo("settings8 workspace8 conf8");
    assertThat(TEST_ARGS9).isEqualTo("settings9 workspace9");
    assertThat(TEST_ARGS10).isEqualTo("user10 workspace10");

    assertThat(TEST_ARGSa).isEqualTo(" user1 settings1 workspace1 conf1  user3 workspace3 confa");
    assertThat(TEST_ARGSb)
        .isEqualTo("user10 workspace10 settingsb  user1 settings1 workspace1 conf1  user3 workspace3 confa userb");

    assertThat(TEST_ARGSc).isEqualTo(" user1 settings1 workspace1 conf1 userc settingsc confc");
    assertThat(TEST_ARGSd).isEqualTo(" user1 settings1 workspace1 conf1 userd workspaced");
  }
}
