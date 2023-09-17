package com.devonfw.tools.ide.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link FilenameUtil}.
 */
public class FilenameUtilTest extends Assertions {

  /**
   * Test of {@link FilenameUtil#getExtension(String)}.
   */
  @Test
  public void testGetExtension() {

    assertThat(FilenameUtil.getExtension("foo_zip")).isNull();
    assertThat(FilenameUtil.getExtension("foo.zip")).isEqualTo("zip");
    assertThat(FilenameUtil.getExtension("FOO.ZIP")).isEqualTo("zip");
    assertThat(FilenameUtil.getExtension("file.name.tar.gz")).isEqualTo("tar.gz");
    assertThat(FilenameUtil.getExtension("file.name.Tar.Gz")).isEqualTo("tar.gz");
    assertThat(FilenameUtil.getExtension("FILE.NAME.TAR.GZ")).isEqualTo("tar.gz");
    assertThat(FilenameUtil.getExtension("https://server.com/")).isNull();
    assertThat(FilenameUtil.getExtension("https://server.com/folder.zip")).isEqualTo("zip");
    assertThat(FilenameUtil.getExtension("https://server.com/folder.zip/")).isNull();
    assertThat(FilenameUtil.getExtension("https://server.com/folder.zip/file")).isNull();
    assertThat(FilenameUtil.getExtension("https://server.com/folder.zip/app.dmg")).isEqualTo("dmg");
    assertThat(FilenameUtil.getExtension("https://server.com/folder.zip/file.tar.bz2")).isEqualTo("tar.bz2");
  }

}
