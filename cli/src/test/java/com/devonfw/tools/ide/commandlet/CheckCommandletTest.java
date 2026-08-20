package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.check.CheckCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * Test of {@link CheckCommandlet}
 */
public class CheckCommandletTest extends AbstractIdeContextTest {

  /**
   * Test that {@code ide check} does not fail if the CWD is not inside a git repository.
   */
  @Test
  void testCheckWithoutGitRepoOnlyWarns(@TempDir Path tempDir) {

    IdeTestContext context = newContext(tempDir);
    CheckCommandlet check = new CheckCommandlet(context);

    check.run();

    assertThat(context).log().hasEntries(
        IdeLogEntry.ofWarning("This does not seem to be a git repository (no .git folder found in " + tempDir + " or any parent directory).")
    );
  }

  /**
   * Test that {@code ide check} reports a missing .gitignore and fails (exit code 0) without --fix.
   */
  @Test
  void testCheckMissingGitignoreWithoutFix(@TempDir Path tempDir) throws IOException {

    Files.createDirectory(tempDir.resolve(".git"));
    IdeTestContext context = newContext(tempDir);
    CheckCommandlet check = new CheckCommandlet(context);

    assertThrows(CliException.class, check::run);
    Path gitignore = tempDir.resolve(".gitignore");
    assertThat(gitignore).doesNotExist();
    assertThat(context).log().hasEntries(IdeLogEntry.ofWarning(gitignore + ": No .gitignore found in repository root."));
  }

  /**
   * Test that {@code ide check --fix} creates a missing .gitignore with rules.
   */
  @Test
  void testCheckMissingGitignoreWithFix(@TempDir Path tempDir) throws IOException {

    Files.createDirectory(tempDir.resolve(".git"));
    IdeTestContext context = newContext(tempDir);
    CheckCommandlet check = new CheckCommandlet(context);
    check.fix.setValue(true);

    check.run();

    Path gitignore = tempDir.resolve(".gitignore");
    assertThat(gitignore).exists();
    List<String> lines = Files.readAllLines(gitignore);
    assertThat(lines).contains(".*", "!.gitignore");
  }

  /**
   * Test that {@code ide check} detects missing rules in an existing .gitignore and fails without --fix.
   */
  @Test
  void testCheckExistingGitignoreMissingRulesWithoutFix(@TempDir Path tempDir) throws IOException {

    Files.createDirectory(tempDir.resolve(".git"));
    Files.writeString(tempDir.resolve(".gitignore"), "target/\n");
    IdeTestContext context = newContext(tempDir);
    CheckCommandlet check = context.getCommandletManager().getCommandlet(CheckCommandlet.class);

    assertThrows(CliException.class, check::run);
    List<String> lines = Files.readAllLines(tempDir.resolve(".gitignore"));
    assertThat(lines).containsExactly("target/");
  }

  /**
   * Test that {@code ide check --fix} appends missing rules to an existing .gitignore.
   */
  @Test
  void testCheckExistingGitignoreMissingRulesWithFix(@TempDir Path tempDir) throws IOException {

    Files.createDirectory(tempDir.resolve(".git"));
    Files.writeString(tempDir.resolve(".gitignore"), "target/\n");
    IdeTestContext context = newContext(tempDir);
    CheckCommandlet check = context.getCommandletManager().getCommandlet(CheckCommandlet.class);
    check.fix.setValue(true);

    check.run();

    List<String> lines = Files.readAllLines(tempDir.resolve(".gitignore"));
    assertThat(lines).contains("target/", ".*", "!.gitignore");
  }

  /**
   * Test that {@code ide check} passes without issues if the .gitignore already has both baseline rule
   */
  @Test
  void testCheckPassesWhenGitignoreAlreadyValid(@TempDir Path tempDir) throws IOException {

    Files.createDirectory(tempDir.resolve(".git"));
    Files.writeString(tempDir.resolve(".gitignore"), ".*\n!.gitignore\n");
    IdeTestContext context = newContext(tempDir);
    CheckCommandlet check = context.getCommandletManager().getCommandlet(CheckCommandlet.class);
    check.fix.setValue(true);

    check.run();

    assertThat(context).log().hasEntries(IdeLogEntry.ofInfo("No issues found."));
  }
}
