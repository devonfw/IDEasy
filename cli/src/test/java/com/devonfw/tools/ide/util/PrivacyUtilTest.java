package com.devonfw.tools.ide.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link PrivacyUtil}.
 */
public class PrivacyUtilTest extends Assertions {

  /**
   * Test of {@link PrivacyUtil#removeSensitivePathInformation(String)} with un-sensitive Windows path.
   */
  @Test
  public void testUnsensitiveWindowsPath() {

    String string = "C:\\Windows\\System32\\drivers\\etc\\hosts";
    String result = "C:/Windows/System32/drivers/etc/hosts";
    assertThat(PrivacyUtil.removeSensitivePathInformation(string)).isEqualTo(result);
  }

  /**
   * Test of {@link PrivacyUtil#removeSensitivePathInformation(String)} with a path containing sensitive and un-sensitive segments.
   */
  @Test
  public void testMixedPathWithStrangeStuff() {

    String string = "C:\\Windows\\Users\\hohwille/\\projects/IDEasy/../other-project/./settings/intellij/workspaces/update/.intellij/file.txt";
    String result = "C:/Windows/Users/********//projects/******/../*****-project/./settings/intellij/workspaces/update/.intellij/file.txt";
    assertThat(PrivacyUtil.removeSensitivePathInformation(string)).isEqualTo(result);
  }

  /**
   * Test of {@link PrivacyUtil#removeSensitivePathInformation(String)} with text containing multiple paths. In reality this should never happen but if
   * developers do mistakes and use {@link String#format(String, Object...)} and pass the result as logger argument this would still be handled properly. Only
   * if the developer passes the result of {@link String#format(String, Object...)} as message instead of parameter, it will not work. This needs to be revealed
   * in reviews and by tests.
   */
  @Test
  public void testTextMixedWithMultiplePaths() {

    String string = "In project /projects/myproject the file ~/projects/myproject/secret-module/src/main/java/com/secret_company/secret_project/common/data\\SecretClass.java could not be deleted.";
    String result = "In project /projects/********* the file ~/projects/*********/******-module/src/main/java/com/******_*******/******_project/common/data/***********.java could not be deleted.";
    assertThat(PrivacyUtil.removeSensitivePathInformation(string)).isEqualTo(result);
  }

  /**
   * Test of {@link PrivacyUtil#removeSensitivePathInformation(String)} with a variant from StatusCommandletTest that failed on GitHub for no logical reason.
   */
  @Test
  public void testPathWithUsername() {

    String string = "/mnt/c/Users/testuser/projects/project";
    String result = "/mnt/c/Users/********/projects/project";
    assertThat(PrivacyUtil.removeSensitivePathInformation(string)).isEqualTo(result);
  }

}
