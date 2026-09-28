package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.androidstudio.AndroidStudio;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.pycharm.Pycharm;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Tests that the {@code «IDE»_PROPERTIES} environment variables ({@code IDEA_PROPERTIES}, {@code STUDIO_PROPERTIES},
 * {@code PYCHARM_PROPERTIES}) point at the {@link IdeToolCommandlet#getOpenPath() folder being opened} rather than unconditionally at the managed
 * workspace. This is the "two-phase wiring" of #2494: {@code setEnvironment} (shell env / install phase) and {@code runTool} (launch) must agree on the
 * opened folder. Because the {@code --project} flag is remembered on the commandlet before the ensure-install step runs {@code setEnvironment}
 * (see {@link IdeToolCommandlet#extractProjectOption}), {@code getOpenPath()} is already the correct folder in both phases.
 */
class IdeaPropertiesRetargetTest extends AbstractIdeContextTest {

  private static final Path DUMMY = Path.of("/dummy/tool");

  private static final ToolInstallation INSTALLATION = new ToolInstallation(DUMMY, DUMMY, DUMMY, VersionIdentifier.LATEST, false);

  /**
   * Tests that by default (no {@code --project} flag) {@code IDEA_PROPERTIES} points at the managed workspace's {@code idea.properties} (regression
   * guard: default behavior is unchanged).
   */
  @Test
  void testIdeaPropertiesDefaultIsWorkspacePath() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij intellij = new Intellij(context);
    RecordingEnvironment env = new RecordingEnvironment();
    // act
    intellij.setEnvironment(env, INSTALLATION, false);
    // assert
    assertThat(env.set.get("IDEA_PROPERTIES")).isEqualTo(context.getWorkspacePath().resolve("idea.properties").toString());
  }

  /**
   * Tests that with an explicit {@code --project} folder the IDE-specific {@code *_PROPERTIES} env var (IDEA / STUDIO / PYCHARM) points at the opened
   * folder's properties file instead of the managed workspace.
   *
   * @param ide the commandlet name to test.
   * @param commandletName the {@code ide} commandlet name used to build the context.
   * @param envVar the IDE-specific properties env var name.
   * @param propertiesFile the properties file name expected inside the opened folder.
   */
  @ParameterizedTest
  @CsvSource({ "intellij, intellij, IDEA_PROPERTIES, idea.properties",
      "android-studio, android-studio, STUDIO_PROPERTIES, studio.properties",
      "pycharm, pycharm, PYCHARM_PROPERTIES, pycharm.properties" })
  void testPropertiesExternalProjectIsOpenPath(String ide, String commandletName, String envVar, String propertiesFile) {

    // arrange
    IdeTestContext context = newContext(commandletName);
    IdeToolCommandlet commandlet = buildIde(context, commandletName);
    Path external = context.getIdeHome().resolve("external-project").normalize();
    commandlet.project.setValue(external);
    RecordingEnvironment env = new RecordingEnvironment();
    // act
    commandlet.setEnvironment(env, INSTALLATION, false);
    // assert
    assertThat(env.set.get(envVar)).isEqualTo(external.resolve(propertiesFile).toString());
    assertThat(env.set.get(envVar)).isNotEqualTo(context.getWorkspacePath().resolve(propertiesFile).toString());
  }

  /**
   * Builds the concrete {@link IdeToolCommandlet} for the given commandlet name.
   *
   * @param context the {@link IdeContext}.
   * @param commandletName the commandlet name.
   * @return the built commandlet.
   */
  private static IdeToolCommandlet buildIde(IdeTestContext context, String commandletName) {

    return switch (commandletName) {
      case "intellij" -> new Intellij(context);
      case "android-studio" -> new AndroidStudio(context);
      case "pycharm" -> new Pycharm(context);
      default -> throw new IllegalArgumentException("Unsupported IDE: " + commandletName);
    };
  }

  /**
   * Tests the "two-phase agreement" end-to-end: a real {@code ide intellij --project <path>} launch must pass the launched IDE an {@code IDEA_PROPERTIES}
   * environment variable pointing at the opened folder's {@code idea.properties} (the same folder the IDE is told to open). The IntelliJ mock binary
   * captures the {@code IDEA_PROPERTIES} env var it is launched with (see {@code ideaprops}), so we assert both the launch args and the env var point at the
   * same external folder - proving the {@code setEnvironment} (shell env) and {@code runTool} (launch) phases agree.
   */
  @Test
  void testExternalLaunchPassesIdeaPropertiesOfOpenFolder() {

    // arrange
    IdeTestContext context = newContext("intellij");
    context.setSystemInfo(SystemInfoMock.of("linux"));
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    Path external = context.getIdeHome().resolve("external-project").normalize();
    context.getFileAccess().mkdirs(external);
    // act - a real CLI launch exactly like `ide intellij --project <path>`
    int exitCode = context.run(new CliArguments("intellij", "--project", external.toString()));
    // assert
    assertThat(exitCode).isEqualTo(0);
    // the IDE is launched with the opened folder as its argument (launch phase)
    String launchedArgs = context.getFileAccess().readFileContent(intellij.getToolBinPath().resolve("intellijtest")).trim();
    assertThat(launchedArgs).endsWith(external.toString());
    // the launched IDE received IDEA_PROPERTIES pointing at the opened folder's idea.properties (shell env phase) - same folder as the launch arg
    String ideaProperties = context.getFileAccess().readFileContent(intellij.getToolBinPath().resolve("ideaprops")).trim();
    assertThat(ideaProperties).isEqualTo(external.resolve("idea.properties").toString());
  }

  /**
   * {@link EnvironmentContext} test double that records the variables set via {@link #withEnvVar(String, String)}.
   */
  private static class RecordingEnvironment implements EnvironmentContext {

    private final Map<String, String> set = new HashMap<>();

    @Override
    public EnvironmentContext withEnvVar(String key, String value) {
      this.set.put(key, value);
      return this;
    }

    @Override
    public EnvironmentContext withPathEntry(Path path) {
      return this;
    }
  }
}
