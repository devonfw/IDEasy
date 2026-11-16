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
import com.devonfw.tools.ide.context.IdeContext;
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
   * Test that {@code ide check} reports a missing .gitignore and fails (exit code 1) without --fix.
   */
  @Test
  void testCheckMissingGitignoreWithoutFix(@TempDir Path tempDir) throws IOException {

    Files.createDirectory(tempDir.resolve(".git"));
    IdeTestContext context = newContext(tempDir);
    CheckCommandlet check = new CheckCommandlet(context);

    assertThrows(CliException.class, check::run);
    // findRepositoryRoot resolves symlinks, so /var and /private/var must not be compared as different paths.
    Path gitignore = context.getFileAccess().toRealPath(tempDir).resolve(".gitignore");
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

    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("No issues found."));
  }

  /**
   * Test that {@code ide check --all} detects all repositories across all workspaces and fails without --fix if they lack a .gitignore.
   */
  @Test
  void testCheckAllDetectsAllRepositoriesAcrossWorkspaces(@TempDir Path tempDir) throws IOException {

    Path ideHome = tempDir;
    Path repoA = ideHome.resolve("workspaces").resolve("main").resolve("testRepoA");
    Path repoB = ideHome.resolve("workspaces").resolve("dev").resolve("testRepoB");
    Path settings = ideHome.resolve("settings");
    newGitRepo(repoA);
    newGitRepo(repoB);
    newGitRepo(settings);

    Path cwd = repoA;
    IdeTestContext context = newContext(cwd);
    context.setCwd(cwd, IdeContext.WORKSPACE_MAIN, ideHome);
    CheckCommandlet check = new CheckCommandlet(context);
    check.all.setValue(true);

    assertThrows(CliException.class, check::run);
    assertThat(context).logAtWarning().hasMessageContaining(repoA.resolve(".gitignore").toString());
    assertThat(context).logAtWarning().hasMessageContaining(repoB.resolve(".gitignore").toString());
  }

  /**
   * Test that {@code ide check --all --fix} creates a .gitignore in every detected repository and does not touch repositories above the CWD (e.g. the settings
   * repository).
   */
  @Test
  void testCheckAllWithFixCreatesGitignoreInAllRepositories(@TempDir Path tempDir) throws IOException {

    Path ideHome = tempDir;
    Path repoA = ideHome.resolve("workspaces").resolve("main").resolve("testRepoA");
    Path repoB = ideHome.resolve("workspaces").resolve("dev").resolve("testRepoB");
    Path settings = ideHome.resolve("settings");
    newGitRepo(repoA);
    newGitRepo(repoB);
    newGitRepo(settings);

    Path cwd = repoA;
    IdeTestContext context = newContext(cwd);
    context.setCwd(cwd, IdeContext.WORKSPACE_MAIN, ideHome);
    CheckCommandlet check = new CheckCommandlet(context);
    check.all.setValue(true);
    check.fix.setValue(true);

    check.run();

    assertThat(repoA.resolve(".gitignore")).exists();
    assertThat(repoB.resolve(".gitignore")).exists();
    assertThat(settings.resolve(".gitignore")).doesNotExist();
  }

  /**
   * Test that {@code ide check --all} succeeds without issues if no repository is found.
   */
  @Test
  void testCheckAllWithoutRepositoriesSucceeds(@TempDir Path tempDir) throws IOException {

    Path ideHome = tempDir;
    Path cwd = ideHome.resolve("workspaces").resolve("main").resolve("empty");
    Files.createDirectories(cwd);
    IdeTestContext context = newContext(cwd);
    context.setCwd(cwd, IdeContext.WORKSPACE_MAIN, ideHome);
    CheckCommandlet check = new CheckCommandlet(context);
    check.all.setValue(true);

    check.run();

    assertThat(context).log().hasEntries(IdeLogEntry.ofSuccess("No issues found."));
  }

  /**
   * Test that {@code ide check --all} stops at each detected repository root and does not descend into it (so a nested repository is not checked twice).
   */
  @Test
  void testCheckAllDoesNotDescendIntoDetectedRepository(@TempDir Path tempDir) throws IOException {

    Path ideHome = tempDir;
    Path repoA = ideHome.resolve("workspaces").resolve("main").resolve("myrepo");
    Path nested = repoA.resolve("nested");
    newGitRepo(repoA);
    newGitRepo(nested);

    Path cwd = repoA;
    IdeTestContext context = newContext(cwd);
    context.setCwd(cwd, IdeContext.WORKSPACE_MAIN, ideHome);
    CheckCommandlet check = new CheckCommandlet(context);
    check.all.setValue(true);
    check.fix.setValue(true);

    check.run();

    assertThat(repoA.resolve(".gitignore")).exists();
    assertThat(nested.resolve(".gitignore")).doesNotExist();
  }

  /**
   * Test that {@code ide check --all} reports each repository root only once when the CWD lies within the workspaces base path, so a repository found by both
   * scans is de-duplicated.
   */
  @Test
  void testCheckAllDeduplicatesRepositoryRoots(@TempDir Path tempDir) throws IOException {

    Path ideHome = tempDir;
    Path repoA = ideHome.resolve("workspaces").resolve("main").resolve("testRepoA");
    Path repoB = ideHome.resolve("workspaces").resolve("dev").resolve("testRepoB");
    newGitRepo(repoA);
    newGitRepo(repoB);

    // the CWD is inside the workspaces base path, so repoA is discovered by both the workspaces scan and the CWD scan
    Path cwd = repoA;
    IdeTestContext context = newContext(cwd);
    context.setCwd(cwd, IdeContext.WORKSPACE_MAIN, ideHome);
    CheckCommandlet check = new CheckCommandlet(context);
    check.all.setValue(true);

    assertThrows(CliException.class, check::run);
    // without de-duplication repoA would be found twice, resulting in 3 instead of 2 repository roots
    assertThat(context).logAtInfo().hasMessage("Found 2 repository root(s) to check.");
  }

  /**
   * Creates a fake git repository at the given directory (i.e. creates the directory along with a {@code .git} sub-directory).
   *
   * @param repositoryRoot the {@link Path} of the repository to create.
   * @return the {@link Path} of the created {@code .git} folder.
   * @throws IOException if the directories could not be created.
   */
  private Path newGitRepo(Path repositoryRoot) throws IOException {

    return Files.createDirectories(repositoryRoot.resolve(".git"));
  }
}
