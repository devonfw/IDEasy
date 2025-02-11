package com.devonfw.tools.ide.migration.v2025;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsHelper;

/**
 * Test of {@link Mig202502001}.
 */
public class Mig202502001Test extends AbstractIdeContextTest {

  @Test
  public void testRemoveObsoleteEntryFromWindowsPath() {

    assertThat(Mig202502001.removeObsoleteEntryFromWindowsPath("C:\\projects\\_ide\\bin;C:\\Windows\\System32;C:\\Program Files\\Git\\cmd")).isEqualTo(
        "C:\\Windows\\System32;C:\\Program Files\\Git\\cmd");
    assertThat(Mig202502001.removeObsoleteEntryFromWindowsPath("C:\\Windows\\System32;C:\\Program Files\\Git\\cmd;C:\\projects\\_ide\\bin")).isEqualTo(
        "C:\\Windows\\System32;C:\\Program Files\\Git\\cmd");
    assertThat(Mig202502001.removeObsoleteEntryFromWindowsPath("C:\\Windows\\System32;C:\\projects\\_ide\\bin;C:\\Program Files\\Git\\cmd")).isEqualTo(
        "C:\\Windows\\System32;C:\\Program Files\\Git\\cmd");
    assertThat(Mig202502001.removeObsoleteEntryFromWindowsPath(
        "C:\\Windows\\System32;C:\\projects\\_ide\\bin;C:\\projects\\_ide\\installation\\bin;C:\\Program Files\\Git\\cmd")).isEqualTo(
        "C:\\Windows\\System32;C:\\projects\\_ide\\installation\\bin;C:\\Program Files\\Git\\cmd");
    assertThat(Mig202502001.removeObsoleteEntryFromWindowsPath(
        "C:\\Windows\\System32;C:\\projects\\_ide\\installation\\bin;C:\\Program Files\\Git\\cmd")).isNull();
  }

  @Test
  public void testMigration() {

    // arrange
    IdeTestContext context = newContext("migration");
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    WindowsHelper helper = context.getWindowsHelper();
    String originalPath = helper.getUserEnvironmentValue("PATH");
    helper.setUserEnvironmentValue("PATH", "C:\\projects\\_ide\\bin;" + originalPath);
    // act
    new Mig202502001().run(context);
    // assert
    assertThat(helper.getUserEnvironmentValue("PATH")).isEqualTo(originalPath);
    assertThat(context.getUserHome().resolve(".bashrc")).hasContent("""
        shopt -s histappend
        bind "set completion-ignore-case off"
        
        alias devon="source ~/.devon/devon"
        devon
        source ~/.devon/autocomplete
        
        
        source "$IDE_ROOT/_ide/installation/functions"
        """);
  }

}
