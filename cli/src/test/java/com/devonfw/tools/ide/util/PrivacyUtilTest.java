package com.devonfw.tools.ide.util;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class for {@link PrivacyUtil}.
 */
public class PrivacyUtilTest extends Assertions {

  /**
   * Tests username replacements on various OS paths (windows, linux, wsl, mac).
   *
   * @param original path to check
   * @param expected path to check.
   */
  @ParameterizedTest
  @MethodSource("providePathStrings")
  public void testReplacePathString(String original, String expected) {
    String modified = PrivacyUtil.applyPrivacyToString(original);
    assertThat(modified).isEqualTo(expected);
  }

  private static Stream<Arguments> providePathStrings() {
    return Stream.of(
        Arguments.of("C:\\Users\\testuser", "C:\\Users\\<username>"),
        Arguments.of("/home/testuser", "/home/<username>"),
        Arguments.of("/mnt/c/Users/testuser", "/mnt/c/Users/<username>"),
        Arguments.of("/Users/testuser", "/Users/<username>")
    );
  }

}
