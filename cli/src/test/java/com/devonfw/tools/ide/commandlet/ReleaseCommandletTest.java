package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.git.GitContextImplMock;
import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * Test of {@link ReleaseCommandlet}.
 */
class ReleaseCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_RELEASE = "release";

  private IdeTestContext newReleaseContext(boolean untrackedFiles) {

    IdeTestContext context = newContext(PROJECT_RELEASE);
    GitContextImplMock gitContext = new GitContextImplMock(context, context.getWorkspacePath().resolve("mvn"));
    gitContext.setSimulateUntrackedFiles(untrackedFiles);
    context.setGitContext(gitContext);
    context.setCwd(context.getWorkspacePath().resolve("mvn"), context.getWorkspacePath().toString(), context.getIdeHome());
    return context;
  }

  @Test
  void testReleaseWithUntrackedFilesThrowsException() {

    IdeTestContext context = newReleaseContext(true);
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    assertThrows(CliException.class, releaseCommandlet::run);
  }

  @Test
  void testRelease() {

    IdeTestContext context = newReleaseContext(false);
    context.setAnswers("1.0.0", "yes");
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    releaseCommandlet.run();

    assertThat(context).log().hasEntries(
        IdeLogEntry.ofInfo("mvn versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false"),
        IdeLogEntry.ofInfo("mvn clean deploy"),
        IdeLogEntry.ofInfo("mvn versions:set -DnewVersion=1.0.1-SNAPSHOT -DgenerateBackupPoms=false"),
        IdeLogEntry.ofInfo("Successfully released version 1.0.0."));
  }

  @Test
  void testReleaseWithAdditionalArguments() {

    IdeTestContext context = newReleaseContext(false);
    context.setAnswers("1.0.0", "yes");
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);
    releaseCommandlet.arguments.addValue("-DskipTests");

    releaseCommandlet.run();

    assertThat(context).log().hasEntries(
        IdeLogEntry.ofInfo("mvn clean deploy -DskipTests"));
  }

  @Test
  void testReleaseAbortedByUser() {

    IdeTestContext context = newReleaseContext(false);
    context.setAnswers("1.0.0", "no");
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    assertThrows(CliAbortException.class, releaseCommandlet::run);
  }

  @Test
  void testReleaseWithoutBuildDescriptor() {

    IdeTestContext context = newReleaseContext(false);
    context.setAnswers("1.0.0", "yes");
    context.setCwd(context.getWorkspacePath().resolve("empty"), context.getWorkspacePath().toString(), context.getIdeHome());
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    assertThrows(CliException.class, releaseCommandlet::run);
  }
}
