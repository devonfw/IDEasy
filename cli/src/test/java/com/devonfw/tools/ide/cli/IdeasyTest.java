package com.devonfw.tools.ide.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

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
