package com.devonfw.tools.ide.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link HexUtil}.
 */
public class HexUtilTest extends Assertions {

  /** Test of {@link HexUtil#toHexString(byte[])}. */
  @Test
  public void testToHexString() {

    // given
    byte[] data = { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef };
    // when
    String hex = HexUtil.toHexString(data);
    // then
    assertThat(hex).isEqualTo("0123456789abcdef");
  }

}
