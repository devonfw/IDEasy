package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Test of {@link EnvironmentCommandlet}.
 */
public class EnvironmentCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link EnvironmentCommandlet#normalizeWindowsValue(String)} for Windows.
   */
  @Test
  public void testNormalizeWindowsValue2Windows() {

    EnvironmentCommandlet env = new EnvironmentCommandlet(IdeTestContextMock.get());
    assertThat(env.normalizeWindowsValue("")).isEqualTo("");
    assertThat(env.normalizeWindowsValue("*")).isEqualTo("*");
    assertThat(env.normalizeWindowsValue("$:\\\\{garbage}§")).isEqualTo("$:\\\\{garbage}§");
    assertThat(env.normalizeWindowsValue("/c/Windows/system32/drivers/etc/hosts"))
        .isEqualTo("C:\\Windows\\system32\\drivers\\etc\\hosts");
    assertThat(env.normalizeWindowsValue("C:\\Windows\\system32\\drivers\\etc\\hosts"))
        .isEqualTo("C:\\Windows\\system32\\drivers\\etc\\hosts");
    assertThat(env.normalizeWindowsValue("C:\\Users\\login/.ide/scripts/ide"))
        .isEqualTo("C:\\Users\\login\\.ide\\scripts\\ide");
    assertThat(env.normalizeWindowsValue("\\login/.ide/scripts/ide")).isEqualTo("\\login/.ide/scripts/ide");
  }

  /**
   * Test of {@link EnvironmentCommandlet#normalizeWindowsValue(String)} for (Git-)Bash.
   */
  @Test
  public void testNormalizeWindowsValue2Bash() {

    EnvironmentCommandlet env = new EnvironmentCommandlet(IdeTestContextMock.get());
    env.bash.setValue(true);
    assertThat(env.normalizeWindowsValue("")).isEqualTo("");
    assertThat(env.normalizeWindowsValue("*")).isEqualTo("*");
    assertThat(env.normalizeWindowsValue("$:\\\\{garbage}§")).isEqualTo("$:\\\\{garbage}§");
    assertThat(env.normalizeWindowsValue("C:\\Windows\\system32\\drivers\\etc\\hosts"))
        .isEqualTo("/c/Windows/system32/drivers/etc/hosts");
    assertThat(env.normalizeWindowsValue("/c/Windows/system32/drivers/etc/hosts"))
        .isEqualTo("/c/Windows/system32/drivers/etc/hosts");
  }

  /**
   * Test of {@link EnvironmentCommandlet#normalizeWindowsValue(VariableLine)} for Windows.
   */
  @Test
  public void testNormalizeWindowsLine() {

    // arrange
    VariableLine line = VariableLine.of(true, "MAGIC_PATH", "/c/Windows/system32/drivers/etc/hosts");
    EnvironmentCommandlet env = new EnvironmentCommandlet(IdeTestContextMock.get());
    // act
    VariableLine normalized = env.normalizeWindowsValue(line);
    // assert
    assertThat(normalized.getValue()).isEqualTo("C:\\Windows\\system32\\drivers\\etc\\hosts");
    assertThat(normalized.isExport()).isTrue();
    assertThat(normalized.getName()).isEqualTo("MAGIC_PATH");
  }

  /**
   * Test of {@link EnvironmentCommandlet} run.
   */
  @Test
  public void testRun() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    EnvironmentCommandlet env = context.getCommandletManager().getCommandlet(EnvironmentCommandlet.class);
    // act
    env.run();
    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "MVN_VERSION=\"3.9.*\"");
    assertLogMessage(context, IdeLogLevel.INFO, "SOME=\"some-${UNDEFINED}\"");
    assertLogMessage(context, IdeLogLevel.INFO, "BAR=\"bar-some-${UNDEFINED}\"");
    assertLogMessage(context, IdeLogLevel.INFO, "IDE_TOOLS=\"mvn,eclipse\"");
    assertLogMessage(context, IdeLogLevel.INFO, "ECLIPSE_VERSION=\"2023-03\"");
    assertLogMessage(context, IdeLogLevel.INFO, "FOO=\"foo-bar-some-${UNDEFINED}\"");
    assertLogMessage(context, IdeLogLevel.INFO, "JAVA_VERSION=\"17*\"");
    assertLogMessage(context, IdeLogLevel.INFO, "INTELLIJ_EDITION=\"ultimate\"");
    assertLogMessage(context, IdeLogLevel.INFO, "DOCKER_EDITION=\"docker\"");
  }

  /**
   * Test that {@link EnvironmentCommandlet} requires home.
   */
  @Test
  public void testThatHomeIsRequired() {

    // arrange
    EnvironmentCommandlet env = new EnvironmentCommandlet(IdeTestContextMock.get());
    // act & assert
    assertThat(env.isIdeHomeRequired()).isTrue();
  }
}
