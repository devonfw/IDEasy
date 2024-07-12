package com.devonfw.tools.ide.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests of {@link SystemPath}.
 */
public class SystemPathTest {

  @ParameterizedTest
  // arrange
  @ValueSource(strings = { "C:\\Users\\User\\Documents\\My Pictures\\photo.jpg",
      "C:\\Windows\\System32\\drivers\\etc.sys", "D:\\Projects\\ProjectA\\source\\main.py" })
  public void systemPathShouldRecognizeWindowsPaths(String pathStringToTest) {

    // act
    boolean testResult = SystemPath.isValidWindowsPath(pathStringToTest);
    // assert
    assertThat(testResult).isTrue();
  }

  @ParameterizedTest
  // arrange
  @ValueSource(strings = { "-kill", "none", "--help", "/usr/local/bin/firefox.exe" })
  public void systemPathShouldRecognizeNonWindowsPaths(String pathStringToTest) {

    // act
    boolean testResult = SystemPath.isValidWindowsPath(pathStringToTest);
    // assert
    assertThat(testResult).isFalse();
  }

}
