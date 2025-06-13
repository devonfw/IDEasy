package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link StatusCommandlet}.
 */
public class StatusCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BASIC = "basic";

  @Test
  public void testStatusOutsideOfHome() {
    //arrange
    IdeTestContext context = new IdeTestContext();
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    //act
    status.run();

    //assert
    assertThat(context).logAtWarning().hasMessageContaining("You are not inside an IDE project: ");
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run without internet connection.
   */
  @Test
  public void testStatusWhenOffline() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setOnline(false);
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).logAtWarning().hasMessage("Check for newer version of IDEasy is skipped due to no network connectivity.");
  }

  /**
   * Tests that the output of {@link StatusCommandlet} does not contain the username when run with active privacy mode on all OS (windows, linux, WSL, mac).
   *
   * @param os the operating system to test on.
   * @param ideHome the path to the IDE home.
   */
  @ParameterizedTest
  @MethodSource("providePrivacyModeTestCases")
  public void testStatusWhenInPrivacyMode(String os, Path ideHome) {
    // arrange
    IdeTestContext context = new IdeTestContext();
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    context.setIdeHome(ideHome);
    context.getStartContext().setPrivacyMode(true);

    // act
    status.run();

    // assert
    assertThat(context).logAtSuccess().hasNoMessageContaining("testuser");
  }

  private static Stream<Arguments> providePrivacyModeTestCases() {
    return Stream.of(
        Arguments.of(
            "linux",
            Path.of("/mnt/c/Users/testuser/projects/myproject")
        ),
        Arguments.of(
            "windows",
            Path.of("C:\\Users\\testuser\\projects\\myproject")
        ),
        Arguments.of(
            "linux",
            Path.of("/home/testuser/projects/myproject")
        ),
        Arguments.of(
            "mac",
            Path.of("/Users/testuser/projects/myproject")
        )
    );
  }
}
