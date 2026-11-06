package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.HelpCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextTestImpl;
import com.devonfw.tools.ide.git.repository.RepositoryCommandlet;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.claude.RecordingEnvironmentContext;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Intellij}.
 */
@WireMockTest
class IntellijTest extends AbstractIdeContextTest {

  private static final String PROJECT_INTELLIJ = "intellij";
  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);

  /**
   * Tests if the {@link Intellij} can be installed properly.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testIntellijInstall(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);

    //if tool already installed
    commandlet.install();
    assertThat(this.context).logAtDebug().hasMessageContaining("Version 2023.3.3 of tool intellij is already installed");
  }

  /**
   * Tests if the {@link Intellij} can be installed properly, and a plugin can be uninstalled afterward.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testIntellijUninstallPluginAfterwards(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);
    assertThat(commandlet.getToolBinPath().resolve("customRepoTest")).hasContent(
        "custom plugin repo url is: http://customRepo");

    // act
    commandlet.uninstallPlugin(commandlet.getPlugins().getById("activePlugin"));

    //assert
    assertThat(context.getPluginsPath().resolve("intellij").resolve("activePlugin")).doesNotExist();
  }

  /**
   * Tests if {@link Intellij IntelliJ IDE} can be run.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testIntellijRun(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    Intellij commandlet = new Intellij(this.context);

    // act
    commandlet.run();

    // assert
    checkInstallation(this.context);
    assertThat(commandlet.getToolBinPath().resolve("intellijtest")).hasContent(
        "intellij " + this.context.getSystemInfo().getOs() + " nosplash " + this.context.getWorkspacePath());
  }

  /**
   * Test of {@link HelpCommandlet} renders the {@code --project} option for the {@link Intellij} commandlet (the flag is declared on
   * {@link com.devonfw.tools.ide.tool.ide.AbstractIdeToolCommandlet} and inherited here). The assertion is form-independent so it does not depend on whether
   * the option is spelled {@code --project} or {@code --project=<path>} in the help text.
   */
  @Test
  void testIntellijHelpDocumentsProjectOption() {

    // arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    context.getStartContext().setLocale(Locale.ROOT);
    HelpCommandlet help = context.getCommandletManager().getCommandlet(HelpCommandlet.class);
    help.commandlet.setValueAsString("intellij", context);
    // act
    help.run();
    // assert - the option shows up in the usage line and its help text is rendered
    assertThat(context).logAtInfo().hasMessageContaining("[--project]");
    assertThat(context).logAtInfo()
        .hasMessageContaining("the folder to open in the IDE instead of the default workspace");
  }

  /**
   * Tests that the {@code --project} flag is consumed by IDEasy and not passed through to the IDE binary. The flag arrives in the multi-valued passthrough
   * {@code args} of the tool commandlet, so IDEasy extracts and remembers it instead of forwarding it to the IDE. As of {@code #2492} the IntelliJ launch
   * arguments also point at the selected folder ({@link com.devonfw.tools.ide.tool.ide.AbstractIdeToolCommandlet#getOpenPath()}) instead of the managed
   * workspace.
   */
  @Test
  void testIntellijRunWithProjectFlagConsumedByIdeasy() {

    // arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    context.setSystemInfo(SystemInfoMock.of("linux"));
    Path external = context.getIdeHome().resolve("external-project").normalize();
    context.getFileAccess().mkdirs(external);
    // act - the --project=<path> (equals) form
    CliArguments args = new CliArguments("intellij", "--project=" + external.toString());
    args.next();
    int exitCode = context.run(args);
    // assert
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    assertThat(exitCode).isEqualTo(0);
    assertThat(intellij.getOpenPath()).isEqualTo(external);
    // the --project flag token was stripped from the arguments passed to the IDE binary, and the IDE launch now targets the selected folder (not the managed
    // workspace)
    String launchedArgs = context.getFileAccess().readFileContent(intellij.getToolBinPath().resolve("intellijtest")).trim();
    assertThat(launchedArgs).doesNotContain("--project");
    assertThat(launchedArgs).contains(external.toString());
    assertThat(launchedArgs).doesNotContain(context.getWorkspacePath().toString());
  }

  /**
   * Tests that without a {@code --project} flag the launch still runs the workspace configuration (regression guard): the {@code "Configuring workspace …"}
   * step is logged as today.
   */
  @Test
  void testDefaultLaunchRunsWorkspaceConfiguration() {

    // arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    context.setSystemInfo(SystemInfoMock.of("linux"));
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    intellij.install(); // make the IDE binary available so the launch path under test can run
    context.getTestStartContext().getEntries().clear(); // ignore the merge that happened during install
    // act - invoke the launch seam directly without a --project folder (bypassing the ensure-install phase)
    intellij.runTool(new ProcessContextTestImpl(context), ProcessMode.BACKGROUND, new ArrayList<>());
    // assert
    assertThat(context).log().hasMessageContaining("Configuring workspace");
  }

  /**
   * Tests the full CLI launch path ({@code ide intellij --project <path>}) with an already-installed IDE: the workspace configuration must not run, i.e. no
   * {@code "Configuring workspace …"} step. This covers the {@code postInstall} call site that is reached by the launch's ensure-install step
   * ({@code toolAlreadyInstalled -> postInstall -> configureWorkspace}), in addition to the {@code runTool} launch seam.
   */
  @Test
  void testExternalLaunchViaCliSkipsWorkspaceConfiguration() {

    // arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    context.setSystemInfo(SystemInfoMock.of("linux"));
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    intellij.install(); // pre-install so the launch takes the ensure-install (toolAlreadyInstalled -> postInstall) path
    Path external = context.getIdeHome().resolve("external-project").normalize();
    context.getFileAccess().mkdirs(external);
    context.getTestStartContext().getEntries().clear(); // ignore the merge that happened during install
    // act - a real CLI launch exactly like `ide intellij --project=<path>`
    int exitCode = context.run(new CliArguments("intellij", "--project=" + external.toString()));
    // assert
    assertThat(exitCode).isEqualTo(0);
    assertThat(intellij.getOpenPath()).isEqualTo(external);
    // the managed workspace must not be configured for an external launch (covers both the runTool seam and the postInstall call site)
    assertThat(context).log().hasNoMessageContaining("Configuring workspace");
  }

  /**
   * Tests that a bare {@code --project} flag (no value) opens the current working directory instead of the managed workspace (the {@code #2236} case: the IDE
   * opens the folder the user is standing in).
   */
  @Test
  void testIntellijRunWithBareProjectFlagOpensCurrentDir() {

    // arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    context.setSystemInfo(SystemInfoMock.of("linux"));
    // act - bare --project, no value
    int exitCode = context.run(new CliArguments("intellij", "--project"));
    // assert
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    assertThat(exitCode).isEqualTo(0);
    // a bare --project selects the current working directory
    assertThat(intellij.getOpenPath()).isEqualTo(context.getCwd());
    // the launched IDE targets the current working directory, not the managed workspace
    String launchedArgs = context.getFileAccess().readFileContent(intellij.getToolBinPath().resolve("intellijtest")).trim();
    assertThat(launchedArgs).doesNotContain(context.getWorkspacePath().toString());
    assertThat(launchedArgs).contains(context.getCwd().toString());
  }

  /**
   * Tests that {@code --project=<path>} is safe for paths with spaces (the shell passes such a path as a single token, and the equals form keeps it intact),
   * unlike a space-separated {@code --project <path>} which would leak the tail to the IDE.
   */
  @Test
  void testIntellijRunWithProjectEqualsPathWithSpaces() {

    // arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    context.setSystemInfo(SystemInfoMock.of("linux"));
    Path external = context.getIdeHome().resolve("some project with spaces").normalize();
    context.getFileAccess().mkdirs(external);
    // act
    int exitCode = context.run(new CliArguments("intellij", "--project=" + external.toString()));
    // assert
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    assertThat(exitCode).isEqualTo(0);
    assertThat(intellij.getOpenPath()).isEqualTo(external);
    // the whole path (including the spaces) is passed to the IDE, and nothing leaked as extra tokens
    String launchedArgs = context.getFileAccess().readFileContent(intellij.getToolBinPath().resolve("intellijtest")).trim();
    assertThat(launchedArgs).endsWith(external.toString());
  }

  /**
   * Tests if after the installation of intellij the expected plugin marker file is existing.
   */
  @Test
  void testCheckPluginInstallation() {
    // arrange
    IdeTestContext context = newContext("intellij");

    // act
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    commandlet.run();

    assertThat(context).logAtSuccess().hasMessage("Successfully installed plugin: ActivePlugin");
    assertThat(context).logAtSuccess().hasNoMessageContaining("Successfully ended step 'Install plugin ActivePlugin");
    assertThat(context).log().hasMessage("Start: Install plugin ActivePlugin (1/1)");

    // assert
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();

    // part 2 of test

    // arrange
    context.getTestStartContext().getEntries().clear();
    // act
    commandlet.run();
    // assert
    assertThat(context).log().hasNoMessageContaining("Install plugin ActivePlugin");
  }

  /**
   * Tests by using 2 installations of intellij with different editions, if the plugins get re-installed and if all marker files get re-initialized properly.
   */
  @Test
  void testCheckEditionConflictInstallation() {
    // arrange
    IdeTestContext context = newContext("intellij");
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);

    // act
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    commandlet.run();

    // assert
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();

    // act
    commandlet.setEdition("ultimate");
    commandlet.run();

    // assert
    assertThat(context).logAtDebug()
        .hasEntries("Plugin marker file " + context.getIdeHome().resolve(".ide").resolve("plugin.intellij.intellij.ActivePlugin") + " got deleted.");
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("ActivePlugin"))).exists();
  }

  /**
   * Tests if the repository commandlet can trigger an import of a mvn and a gradle project.
   */
  @Test
  void testIntellijMvnAndGradleRepositoryImport() {
    // arrange
    IdeTestContext context = newContext("intellij");
    RepositoryCommandlet rc = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);

    // act
    rc.run();

    // assert
    assertThat(context.getWorkspacePath().resolve(".idea").resolve("misc.xml")).hasContent("""
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <project version="4">
          <component name="MavenProjectsManager">
            <option name="originalFiles">
              <list>
                <option value="$PROJECT_DIR$/test_mvn/pom.xml"/>
              </list>
            </option>
          </component>
        </project>
        """);
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES).resolve("test").resolve(".idea").resolve("gradle.xml")).hasContent("""
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <project version="4">
          <component migrationVersion="1" name="GradleMigrationSettings"/>
          <component name="GradleSettings">
            <option name="linkedExternalProjectsSettings">
              <GradleProjectSettings>
                <option name="externalProjectPath" value="$PROJECT_DIR$/subfolder/test_gradle"/>
              </GradleProjectSettings>
            </option>
          </component>
        </project>
        """);
  }

  /**
   * Tests whether IDEasy correctly switches editions when no version is specified or configured
   */
  @Test
  void testAdjustRequestedEditionSwitchesForUltimateWithoutConfiguredVersion() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    ToolEditionAndVersion requested = new ToolEditionAndVersion(new ToolEdition("intellij", "ultimate"));
    requested.setVersion(VersionIdentifier.LATEST);

    // act
    ToolEditionAndVersion adjusted = commandlet.adjustRequestedEdition(requested);

    // assert
    assertThat(adjusted.getEdition().edition()).isEqualTo("intellij");
  }

  /**
   * Tests whether IDEasy correctly switches editions when the specified version is after 2025.2.6.1
   */
  @Test
  void testAdjustRequestedEditionSwitchesForUltimateWithVersionAboveCutoff() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    ToolEditionAndVersion requested = new ToolEditionAndVersion(new ToolEdition("intellij", "ultimate"));
    requested.setVersion(VersionIdentifier.of("2025.3"));

    // act
    ToolEditionAndVersion adjusted = commandlet.adjustRequestedEdition(requested);

    // assert
    assertThat(adjusted.getEdition().edition()).isEqualTo("intellij");
  }

  /**
   * Tests whether IDEasy correctly remains on the ultimate edition when the specified version is 2025.2.6.1
   */
  @Test
  void testAdjustRequestedEditionDoesNotSwitchForUltimateAtCutoffVersion() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    ToolEditionAndVersion requested = new ToolEditionAndVersion(new ToolEdition("intellij", "ultimate"));
    requested.setVersion(VersionIdentifier.of("2025.2.6.1"));

    // act
    ToolEditionAndVersion adjusted = commandlet.adjustRequestedEdition(requested);

    // assert
    assertThat(adjusted.getEdition().edition()).isEqualTo("ultimate");
  }

  /**
   * Tests if the custom jvm options of the ide variable INTELLI_VM_ARGS have been set.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testIntellijRunWithCustomJvmOptions(String os) {
    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);

    // act
    intellij.run();

    // assert
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve("intellij").resolve(context.getWorkspaceName()).resolve(".idea.vmoptions"))
        .exists()
        .hasContent("""
            -Xms256m
            -Xmx4096m
            -XX:ReservedCodeCacheSize=256m
            -ea
            -Dsun.io.useCanonCaches=true
            """);
  }

  /**
   * Tests that extra Java SDK installations configured via ide-extra-tools.json are imported into IntelliJ jdk.table.xml when IntelliJ is started through
   * IDEasy.
   */
  @Test
  void testImportExtraJavaSdksIntoJdkTable() throws Exception {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);

    Path extraJavaRoot = context.getSoftwareExtraPath().resolve("java");
    context.getFileAccess().mkdirs(extraJavaRoot.resolve("client"));
    context.getFileAccess().mkdirs(extraJavaRoot.resolve("process-engine"));

    // act
    commandlet.run();

    // assert
    Path jdkTable = context.getWorkspacePath()
        .resolve(".intellij")
        .resolve("config")
        .resolve("options")
        .resolve("jdk.table.xml");
    assertThat(jdkTable).exists();

    String jdkTableContent = Files.readString(jdkTable);
    assertThat(jdkTableContent).contains("<name value=\"client\"/>");
    assertThat(jdkTableContent).contains("<name value=\"process-engine\"/>");
    assertThat(jdkTableContent).contains("software/extra/java/client");
    assertThat(jdkTableContent).contains("software/extra/java/process-engine");
  }

  /**
   * Tests that an extra Java SDK using the reserved name "java" is skipped instead of being imported.
   */
  @Test
  void testSkipExtraJavaSdkWithReservedName() throws Exception {

    // arrange
    IdeTestContext context = newContext("intellij");
    Path extraToolsFile = context.getSettingsPath().resolve("ide-extra-tools.json");
    context.getFileAccess().writeFileContent("""
            {
              "java": {
                "java": {
                  "version": "21"
                }
              }
            }
            """,
        extraToolsFile);

    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);

    Path extraJavaRoot = context.getSoftwareExtraPath().resolve("java");
    context.getFileAccess().mkdirs(extraJavaRoot.resolve("java"));

    // act
    commandlet.run();

    // assert
    Path jdkTable = context.getWorkspacePath()
        .resolve(".intellij")
        .resolve("config")
        .resolve("options")
        .resolve("jdk.table.xml");
    assertThat(jdkTable).exists();

    String jdkTableContent = Files.readString(jdkTable);
    assertThat(jdkTableContent).doesNotContain("software/extra/java/java");
    assertThat(context).logAtWarning().hasNoMessageContaining("software/extra/java/java");
  }

  /**
   * Tests that a configured extra Java SDK is skipped if the corresponding installation directory does not exist.
   */
  @Test
  void testSkipMissingExtraJavaSdkDirectory() throws Exception {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);

    // do not create client/process-engine directories on purpose

    // act
    commandlet.run();

    // assert
    Path jdkTable = context.getWorkspacePath()
        .resolve(".intellij")
        .resolve("config")
        .resolve("options")
        .resolve("jdk.table.xml");
    assertThat(jdkTable).exists();

    String jdkTableContent = Files.readString(jdkTable);
    assertThat(jdkTableContent).doesNotContain("software/extra/java/client");
    assertThat(jdkTableContent).doesNotContain("software/extra/java/process-engine");
    assertThat(context).logAtWarning()
        .hasMessageContaining("Skipping extra tool installation import to intellij because it is missing at");
  }

  /** Tests that IntelliJ extra Java SDK import writes jdk.table.xml in the workspace options folder. */
  @Test
  void testMissingExtraJavaTemplateFails() throws Exception {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);

    Path extraJavaRoot = context.getSoftwareExtraPath().resolve("java");
    context.getFileAccess().mkdirs(extraJavaRoot.resolve("client"));

    // act
    commandlet.run();

    // assert
    Path jdkTable = context.getWorkspacePath()
        .resolve(".intellij")
        .resolve("config")
        .resolve("options")
        .resolve("jdk.table.xml");
    assertThat(jdkTable).exists();

    String jdkTableContent = Files.readString(jdkTable);
    assertThat(jdkTableContent).contains("software/extra/java/client");
  }

  /**
   * Tests if the environment variable {@code IDEA_PROPERTIES} is set to the path of the {@code idea.properties} file in the workspace.
   */
  @Test
  void testSetEnvironmentSetsIdeaProperties() {

    // arrange
    Intellij commandlet = new Intellij(this.context);
    Path dummy = context.getSoftwarePath().resolve("intellij");
    ToolInstallation installation = new ToolInstallation(dummy, dummy, dummy, VersionIdentifier.of("2023.3.3"), false);
    RecordingEnvironmentContext environmentContext = new RecordingEnvironmentContext();

    // act
    commandlet.setEnvironment(environmentContext, installation, false);

    // assert
    assertThat(environmentContext.set).containsEntry("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve("idea.properties").toString());
  }

  private void checkInstallation(IdeTestContext context) {

    Intellij commandlet = context.getCommandletManager().getCommandlet(Intellij.class);
    assertThat(commandlet.getInstalledVersion().toString()).isEqualTo("2023.3.3");
    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7", true),
        new IdeLogEntry(IdeLogLevel.SUCCESS, "Successfully installed intellij in version 2023.3.3", true));
    assertThat(context).logAtDebug().hasMessage("Omitting installation of inactive plugin InactivePlugin (inactivePlugin).");
    assertThat(context).logAtSuccess().hasMessage("Successfully installed plugin: ActivePlugin");
  }
}
