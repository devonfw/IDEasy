package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContextMock;

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

}
