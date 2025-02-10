package com.devonfw.tools.ide.migration.v2025;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

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

  @Disabled("Only for manual testing")
  @Test
  public void testMigration() {

    IdeTestContext context = newContext("migration");
    new Mig202502001().run(context);
  }

}
