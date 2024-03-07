package com.devonfw.tools.ide.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests of {@link SystemPath}.
 */
public class SystemPathTest {

  @ParameterizedTest
  @ValueSource(strings = { "C:\\Users\\User\\Documents\\My Pictures\\photo.jpg",
  "C:\\Windows\\System32\\drivers\\etc.sys", "D:\\Projects\\ProjectA\\source\\main.py" })
  public void SystemPathShouldRecognizeWindowsPaths(String pathStringToTest) {

    // act
    boolean testResult = SystemPath.isValidWindowsPath(pathStringToTest);
    assertThat(testResult).isTrue();

  }

  @ParameterizedTest
  @ValueSource(strings = { "-kill", "none", "--help", "/usr/local/bin/firefox.exe" })
  public void SystemPathShouldRecognizeNonWindowsPaths(String pathStringToTest) {

    // act
    boolean testResult = SystemPath.isValidWindowsPath(pathStringToTest);
    assertThat(testResult).isFalse();

  }

  @Test
  public void SystemPathShouldConvertWindowsPathToUnixPath() {

    // arrange
    String windowsPathString = "C:\\Users\\User\\test.exe";
    String expectedUnixPathString = "/c/Users/User/test.exe";

    // act
    String resultPath = SystemPath.convertWindowsPathToUnixPath(windowsPathString);

    // assert
    assertThat(resultPath).isEqualTo(expectedUnixPathString);
  }

}
