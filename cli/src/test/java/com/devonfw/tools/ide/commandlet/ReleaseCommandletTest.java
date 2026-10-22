package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.git.GitContextImplMock;
import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * Test of {@link ReleaseCommandlet}. The test fixture's {@code .mvn/maven.config} declares {@code -Drevision=1.0.0-SNAPSHOT}, so the release version is
 * {@code 1.0.0} and the next version is {@code 1.0.1-SNAPSHOT}.
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
   * Tests the release of a gradle project. The {@code gradle.properties} declares {@code version=1.0.0-SNAPSHOT}.
   */
  @Test
  void testReleaseGradle() {

    IdeTestContext context = newReleaseContext(false);
    context.setAnswers("yes", "yes");
    context.setCwd(context.getWorkspacePath().resolve("gradle"), context.getWorkspacePath().toString(), context.getIdeHome());
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    releaseCommandlet.run();

    assertThat(context).log().hasEntries(
        IdeLogEntry.ofInfo("gradle clean publish"),
        IdeLogEntry.ofInfo("Successfully released version 1.0.0."));
    Path gradleProperties = context.getWorkspacePath().resolve("gradle").resolve("gradle.properties");
    assertThat(context.getFileAccess().readFileContent(gradleProperties)).isEqualTo("version=1.0.1-SNAPSHOT\n\ngroup=com.devonfw\n");
  }

  /**
   * Tests that {@code GRADLE_RELEASE_OPTS} overrides the default options of the gradle release build.
   */
  @Test
  void testReleaseGradleWithReleaseOpts() {

    IdeTestContext context = newReleaseContext(false);
    context.setAnswers("yes", "yes");
    context.setCwd(context.getWorkspacePath().resolve("gradle"), context.getWorkspacePath().toString(), context.getIdeHome());
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("GRADLE_RELEASE_OPTS", "clean build");
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    releaseCommandlet.run();

    assertThat(context).log().hasEntries(
        IdeLogEntry.ofInfo("gradle clean build"),
        IdeLogEntry.ofInfo("Successfully released version 1.0.0."));
  }

  /**
   * Tests that the release of a gradle project fails with a clear message if the {@code gradle.properties} has no {@code version}.
   */
  @Test
  void testReleaseGradleWithoutVersionThrowsException() {

    IdeTestContext context = newReleaseContext(false);
    Path gradleProject = context.getWorkspacePath().resolve("gradle");
    context.setCwd(gradleProject, context.getWorkspacePath().toString(), context.getIdeHome());
    context.getFileAccess().writeFileContent("group=com.devonfw\n", gradleProject.resolve("gradle.properties"));
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    CliException exception = assertThrows(CliException.class, releaseCommandlet::run);
    assertThat(exception).hasMessageContaining("Could not find 'version' in");
  }

  /**
   * Tests that the release fails gracefully if a build descriptor is found but its {@link com.devonfw.tools.ide.tool.BuildTool} does not support releasing
   * (its {@link com.devonfw.tools.ide.tool.BuildTool#getProjectVersion(java.nio.file.Path) getProjectVersion} returns {@code null}), e.g. an npm project.
   */
  @Test
  void testReleaseWithUnsupportedBuildToolThrowsException() {

    IdeTestContext context = newReleaseContext(false);
    context.setCwd(context.getWorkspacePath().resolve("npm"), context.getWorkspacePath().toString(), context.getIdeHome());
    ReleaseCommandlet releaseCommandlet = context.getCommandletManager().getCommandlet(ReleaseCommandlet.class);

    CliException exception = assertThrows(CliException.class, releaseCommandlet::run);
    assertThat(exception).hasMessageContaining("npm").hasMessageContaining("does not support releasing");
  }
}
