package com.devonfw.tools.ide.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link PrivacyUtil}.
 */
public class PrivacyUtilTest extends Assertions {

  /**
   * Test of {@link PrivacyUtil#removeSensitivePathInformation(String)}
   */
  @Test
  public void testRemoveSensitiveFolderNameOrPath() {

    String string = "In project /projects/myproject the file ~/projects/myproject/secret-module/src/main/java/com/secret_company/secret_project/common/data\\SecretClass.java could not be deleted.";
    String result = "In project /projects/********* the file ~/projects/*********/******-module/src/main/java/com/******_*******/******_project/common/data/***********.java could not be deleted.";
    assertThat(PrivacyUtil.removeSensitivePathInformation(string)).isEqualTo(result);
  }

}
