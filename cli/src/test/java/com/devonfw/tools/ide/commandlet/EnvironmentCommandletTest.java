package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.environment.VariableLine;

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

}
