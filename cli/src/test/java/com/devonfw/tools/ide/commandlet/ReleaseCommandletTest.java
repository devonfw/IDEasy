package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.git.GitContextImplMock;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link ReleaseCommandlet}. The test fixture's {@code .mvn/maven.config} declares {@code -Drevision=1.0.0-SNAPSHOT}, so the release version is
 * {@code 1.0.0} and the next version is {@code 1.0.1-SNAPSHOT}.
 */
@WireMockTest
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
    // answer 1: "Is the next version correct?", answer 2: "Do you want to push...?"
    context.setAnswers("yes", "yes");
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    releaseCommandlet.run();

    assertThat(context).log().hasEntries(
        IdeLogEntry.ofInfo("mvn clean deploy"),
        IdeLogEntry.ofInfo("Successfully released version 1.0.0."));
    // the maven.config must be left at the next development version
    Path mavenConfig = context.getWorkspacePath().resolve("mvn").resolve(".mvn").resolve("maven.config");
    assertThat(context.getFileAccess().readFileContent(mavenConfig)).contains("-Drevision=1.0.1-SNAPSHOT");
  }

  @Test
  void testReleaseWithAdditionalArguments() {

    IdeTestContext context = newReleaseContext(false);
    context.setAnswers("yes", "yes");
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);
    releaseCommandlet.arguments.addValue("-DskipTests");

    releaseCommandlet.run();

    assertThat(context).log().hasEntries(
        IdeLogEntry.ofInfo("mvn clean deploy -DskipTests"));
  }

  @Test
  void testReleaseAbortedByUser() {

    IdeTestContext context = newReleaseContext(false);
    // answer 1: next version correct -> yes, answer 2: push -> no (aborts)
    context.setAnswers("yes", "no");
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    assertThrows(CliAbortException.class, releaseCommandlet::run);
  }

  @Test
  void testReleaseWithoutBuildDescriptor() {

    IdeTestContext context = newReleaseContext(false);
    context.setAnswers("yes", "yes");
    context.setCwd(context.getWorkspacePath().resolve("empty"), context.getWorkspacePath().toString(), context.getIdeHome());
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    CliException exception = assertThrows(CliException.class, releaseCommandlet::run);
    assertThat(exception).hasMessageContaining("Could not find a build descriptor");
  }

  /**
   * Tests the release of a yarn project. The {@code package.json} declares {@code "version": "1.0.0-SNAPSHOT"}.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testReleaseYarn(WireMockRuntimeInfo wireMockRuntimeInfo) {

    IdeTestContext context = newContext("yarn", wireMockRuntimeInfo);
    Path yarnProject = context.getWorkspacePath().resolve("release");
    context.setGitContext(new GitContextImplMock(context, yarnProject));
    context.setCwd(yarnProject, context.getWorkspacePath().toString(), context.getIdeHome());
    context.setAnswers("yes", "yes");
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    releaseCommandlet.run();

    assertThat(context).log().hasEntries(
        IdeLogEntry.ofInfo("yarn --cwd " + yarnProject + " version --new-version 1.0.0 --no-git-tag-version"),
        IdeLogEntry.ofInfo("yarn publish --non-interactive"),
        IdeLogEntry.ofInfo("yarn --cwd " + yarnProject + " version --new-version 1.0.1-SNAPSHOT --no-git-tag-version"),
        IdeLogEntry.ofInfo("Successfully released version 1.0.0."));
  }

  /**
   * Tests that the release fails gracefully if a build descriptor is found but its {@link com.devonfw.tools.ide.tool.BuildTool} does not support releasing
   * (its {@link com.devonfw.tools.ide.tool.BuildTool#getProjectVersion(java.nio.file.Path) getProjectVersion} returns {@code null}), e.g. a gradle project
   * (only maven currently supports releasing).
   */
  @Test
  void testReleaseWithUnsupportedBuildToolThrowsException() {

    IdeTestContext context = newReleaseContext(false);
    context.setCwd(context.getWorkspacePath().resolve("gradle"), context.getWorkspacePath().toString(), context.getIdeHome());
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    CliException exception = assertThrows(CliException.class, releaseCommandlet::run);
    assertThat(exception).hasMessageContaining("gradle").hasMessageContaining("does not support releasing");
  }
}
