package com.devonfw.tools.ide.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Test of {@link Ideasy}.
 */
class IdeasyTest extends AbstractIdeContextTest {

  /**
   * Test of {@link Ideasy#run(String...)} so that {@link CliExitException} is thrown and ensure it is not logged.
   */
  @Test
  void testEnvOutsideProjectDoesNotLogCliExitException() {

    // arrange
    IdeTestContext context = newContext(Path.of("/"));
    Ideasy ideasy = new Ideasy(context);

    // act
    ideasy.run("--debug", "env");

    // assert
    assertThat(context).logAtDebug().hasMessage("Step 'ide' ended with failure.");
    assertThat(context).log().hasNoEntryWithException();
  }

  /**
   * Test that a {@link Commandlet#isProcessableOutput() processable-output} commandlet that throws inside {@link Commandlet#run() run} does not leak
   * an ERROR-level error block ("An unexpected error occurred! … please file a bug") nor a "Logfile can be found at …" line into the captured log, while
   * still marking the step as failed.
   * <p>
   * Regression test: rethrowing the exception made {@link Ideasy#run(String...)} log the error at ERROR level into the machine-consumed (auto-completion)
   * output. The fix swallows the failure for processable-output commandlets instead of rethrowing it.
   */
  @Test
  void testProcessableOutputCommandletFailureDoesNotLogError() {

    // arrange
    IdeTestContext context = newContext(Path.of("/"));
    context.addCommandlet(new ThrowingProcessableCommandlet(context));
    Ideasy ideasy = new Ideasy(context);

    // act
    int exitCode = ideasy.run("throw");

    // assert - the step is marked as failed
    assertThat(context).logAtDebug().hasMessage("Step 'ide' ended with failure.");
    // assert - no ERROR-level error block and no "Logfile can be found at" line leaked into the captured output
    assertThat(exitCode).isEqualTo(1);
    assertThat(context).logAtError().hasNoMessageContaining("An unexpected error occurred");
    assertThat(context).log().hasNoMessageContaining("An unexpected error occurred");
    assertThat(context).log().hasNoMessageContaining("Logfile can be found at");
    assertThat(context).log().hasNoEntryWithException();
  }

  /**
   * A minimal {@link Commandlet} that produces processable output (like {@code complete}) but always fails, used to verify how a failure in such a
   * commandlet is reported.
   */
  private static final class ThrowingProcessableCommandlet extends Commandlet {

    /**
     * @param context the {@link IdeContext}.
     */
    ThrowingProcessableCommandlet(IdeContext context) {

      super(context);
      addKeyword("throw");
    }

    @Override
    public String getName() {

      return "throw";
    }

    @Override
    public boolean isIdeRootRequired() {

      return false;
    }

    @Override
    public boolean isProcessableOutput() {

      return true;
    }

    @Override
    protected void doRun() {

      throw new IllegalStateException("boom");
    }
  }

  /**
   * Test of {@code ide --version}.
   */
  @Test
  void testVersionLongOption() {

    // arrange
    IdeTestContext context = newContext(Path.of("/"));
    Ideasy ideasy = new Ideasy(context);

    // act
    int exitCode = ideasy.run("--version");

    // assert
    assertThat(exitCode).isEqualTo(0);
    assertThat(context).logAtProcessable().hasMessage(IdeVersion.getVersionString());
    assertThat(context).logAtError().hasNoMessageContaining("Unknown command");
  }

  /**
   * Test of {@code ide -v}.
   */
  @Test
  void testVersionShortOption() {

    // arrange
    IdeTestContext context = newContext(Path.of("/"));
    Ideasy ideasy = new Ideasy(context);

    // act
    int exitCode = ideasy.run("-v");

    // assert
    assertThat(exitCode).isEqualTo(0);
    assertThat(context).logAtProcessable().hasMessage(IdeVersion.getVersionString());
    assertThat(context).logAtError().hasNoMessageContaining("Unknown command");
  }

  /**
   * Test that running 'ide' without arguments does not trigger any tool installation.
   * Verifies fix for issue #1667.
   */
  @Test
  public void testRunWithoutArgumentsDoesNotTriggerInstallation() {

    // arrange
    String path = "project/workspaces/foo-test";
    IdeTestContext context = newContext("environment", path, false);
    Ideasy ideasy = new Ideasy(context);

    // Take snapshot of software directory before running ide command
    Path softwarePath = context.getSoftwarePath();
    Set<String> existingToolsBefore = new HashSet<>();
    if (Files.exists(softwarePath)) {
      try (var stream = Files.list(softwarePath)) {
        stream.forEach(p -> existingToolsBefore.add(p.getFileName().toString()));
      } catch (Exception e) {
        fail("Failed to list software directory: " + e.getMessage());
      }
    }

    // Take snapshot of _ide/software repository before running ide command
    Path ideaSoftwarePath = context.getIdeRoot().resolve("_ide").resolve("software");
    Set<String> existingIdeToolsBefore = new HashSet<>();
    if (Files.exists(ideaSoftwarePath)) {
      try (var stream = Files.list(ideaSoftwarePath)) {
        stream.forEach(p -> existingIdeToolsBefore.add(p.getFileName().toString()));
      } catch (Exception e) {
        fail("Failed to list _ide/software directory: " + e.getMessage());
      }
    }

    // act - run 'ide' without any arguments (triggers default env behavior)
    ideasy.run();

    // assert - verify no new tools were installed
    Set<String> existingToolsAfter = new HashSet<>();
    if (Files.exists(softwarePath)) {
      try (var stream = Files.list(softwarePath)) {
        stream.forEach(p -> existingToolsAfter.add(p.getFileName().toString()));
      } catch (Exception e) {
        fail("Failed to list software directory after ide: " + e.getMessage());
      }
    }

    Set<String> existingIdeToolsAfter = new HashSet<>();
    if (Files.exists(ideaSoftwarePath)) {
      try (var stream = Files.list(ideaSoftwarePath)) {
        stream.forEach(p -> existingIdeToolsAfter.add(p.getFileName().toString()));
      } catch (Exception e) {
        fail("Failed to list _ide/software directory after ide: " + e.getMessage());
      }
    }

    // Verify no new tools were added to software directory
    assertThat(existingToolsAfter).as("No new tools should be installed in software directory").isEqualTo(existingToolsBefore);

    // Verify no new tools were added to _ide/software repository
    assertThat(existingIdeToolsAfter).as("No new tools should be installed in _ide/software repository").isEqualTo(existingIdeToolsBefore);
  }

}
