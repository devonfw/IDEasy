package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link StatusCommandlet}.
 */
public class StatusCommandletTest extends AbstractIdeContextTest {

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
   * Tests that if {@link StatusCommandlet} is run outside a project and the cases of IDE_ROOT and CWD differ, no error message about the wrong CWD path will be
   * shown.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testStatusWhenUsingWrongCaseOutsideOfProjectShowsNoError(@TempDir Path tempDir) {

    // arrange
    IdeTestContext context = new IdeTestContext();
    Path ideRootLowercase = tempDir.resolve("projects");
    Path ideRootUppercase = tempDir.resolve("Projects");
    context.setIdeRoot(ideRootLowercase);
    context.getSystem().setEnv("IDE_ROOT", ideRootLowercase.toString());
    context.setCwd(ideRootUppercase.getParent(), "", null);
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).logAtWarning()
        .hasNoMessageContaining("Your CWD path");
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run within a project and the IDE_ROOT and CWD cases did not match.
   *
   * @param os String of the OS to use.
   * @param tempDir temporary directory to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac" })
  public void testStatusWhenUsingWrongCaseInProjectShowsError(String os, @TempDir() Path tempDir) {

    // arrange
    IdeTestContext context = new IdeTestContext();
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Path ideRootLowercase = tempDir.resolve("projects");
    Path ideRootUppercase = tempDir.resolve("Projects");
    context.setIdeRoot(ideRootLowercase);
    context.getSystem().setEnv("IDE_ROOT", ideRootLowercase.toString());
    context.setCwd(ideRootUppercase, "", Path.of("non-existing-project"));
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).logAtError()
        .hasMessage("Your CWD path: '" + ideRootUppercase + "' and your IDE_ROOT path: '" + ideRootLowercase + "' cases do not match!\n"
            + "Please check your 'user.dir' or starting directory and make sure that it matches your IDE_ROOT path.");
  }

  /**
   * Tests the output if {@link StatusCommandlet} is run within a project and the IDE_ROOT and CWD cases did not match on a linux system no error message will
   * be shown.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testStatusWhenUsingWrongCaseInProjectShowsNoErrorOnLinux(@TempDir() Path tempDir) {

    // arrange
    IdeTestContext context = new IdeTestContext();
    SystemInfo systemInfo = SystemInfoMock.of("linux");
    context.setSystemInfo(systemInfo);
    Path ideRootLowercase = tempDir.resolve("projects");
    Path ideRootUppercase = tempDir.resolve("Projects");
    context.setIdeRoot(ideRootLowercase);
    context.getSystem().setEnv("IDE_ROOT", ideRootLowercase.toString());
    context.setCwd(ideRootUppercase, "", Path.of("non-existing-project"));
    StatusCommandlet status = context.getCommandletManager().getCommandlet(StatusCommandlet.class);

    // act
    status.run();

    // assert
    assertThat(context).logAtWarning()
        .hasNoMessageContaining("Your CWD path");
  }
}
