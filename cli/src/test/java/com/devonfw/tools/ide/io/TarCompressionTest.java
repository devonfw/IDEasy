package com.devonfw.tools.ide.io;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link TarCompression}.
 */
class TarCompressionTest extends Assertions {

  /** Test of {@link TarCompression#of(String)}. */
  @Test
  void testOf() {

    assertThat(TarCompression.of(".tar")).isSameAs(TarCompression.NONE);
    assertThat(TarCompression.of("tar")).isSameAs(TarCompression.NONE);
    assertThat(TarCompression.of("file.tgz")).isSameAs(TarCompression.GZ);
    assertThat(TarCompression.of("tgz")).isSameAs(TarCompression.GZ);
    assertThat(TarCompression.of("gz")).isNull();
    assertThat(TarCompression.of("tar.gz")).isSameAs(TarCompression.GZ);
    assertThat(TarCompression.of("tar.file.tar.gz")).isSameAs(TarCompression.GZ);
    assertThat(TarCompression.of("tbz2")).isSameAs(TarCompression.BZIP2);
    assertThat(TarCompression.of("bz2")).isNull();
    assertThat(TarCompression.of("bzip2")).isNull();
    assertThat(TarCompression.of("file.tar.bz2")).isSameAs(TarCompression.BZIP2);
    assertThat(TarCompression.of("file.tar.bzip2")).isSameAs(TarCompression.BZIP2);
    assertThat(TarCompression.of(".pkg")).isNull();
    assertThat(TarCompression.of("tfoo")).isNull();
    assertThat(TarCompression.of("file.tar.foo")).isNull();
  }

}
