package com.devonfw.tools.ide.tool.vscode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextTestImpl;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * Test of {@link Vscode} class.
 */
class VscodeTest extends AbstractIdeContextTest {

  private static final String PROJECT_VSCODE = "vscode";

  private static final String PROJECT_VSCODIUM = "vscodium";

  @Test
  void testVscodeInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);

    // install
    vscodeCommandlet.install();

    // assert
    checkInstallation(context);
  }

  @Test
  void testVscodeRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);

    // install
    vscodeCommandlet.run();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if after the installation of vscode the expected plugin marker file is existing.
   */
  @Test
  void testCheckPluginInstallation() {
    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);

    // act
    Vscode commandlet = context.getCommandletManager().getCommandlet(Vscode.class);
    commandlet.run();

    assertThat(context).logAtSuccess().hasMessage("Successfully installed plugin: mockedPlugin");
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin mockedPlugin (1/1)'.");

    // assert
    assertThat(commandlet.retrievePluginMarkerFilePath(commandlet.getPlugin("mockedPlugin"))).exists();
    assertThat(context.getIdeHome().resolve("plugins").resolve("vscode")).exists();

    // part 2 of test

    // arrange
    context.getTestStartContext().getEntries().clear();
    // act
    commandlet.run();
    // assert
    assertThat(context).logAtDebug().hasNoMessage("Successfully installed plugin: ActivePlugin");
  }

  @Test
  void testInstallPluginUsesExtensionVersionIfConfigured() {

    IdeTestContext context = newContext(PROJECT_VSCODE);
    CapturingVscode vscodeCommandlet = new CapturingVscode(context);
    ToolPluginDescriptor plugin = new ToolPluginDescriptor("publisher.extension", "mockedPlugin", null, "1.2.3", true, null, null);
    Step step = context.newStep("Install plugin mockedPlugin");

    step.run(() -> vscodeCommandlet.installPlugin(plugin, step, new ProcessContextTestImpl(context)));

    assertThat(vscodeCommandlet.lastArgs).contains("--install-extension", "publisher.extension@1.2.3");
  }

  @Test
  void testInstallPluginUsesExtensionIdWithoutVersionByDefault() {

    IdeTestContext context = newContext(PROJECT_VSCODE);
    CapturingVscode vscodeCommandlet = new CapturingVscode(context);
    ToolPluginDescriptor plugin = new ToolPluginDescriptor("publisher.extension", "mockedPlugin", null, null, true, null, null);
    Step step = context.newStep("Install plugin mockedPlugin");

    step.run(() -> vscodeCommandlet.installPlugin(plugin, step, new ProcessContextTestImpl(context)));

    assertThat(vscodeCommandlet.lastArgs).contains("--install-extension", "publisher.extension");
    assertThat(vscodeCommandlet.lastArgs).doesNotContain("publisher.extension@null");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("vscode/bin/code.cmd")).exists().hasContent("@echo test for windows");
    assertThat(context.getSoftwarePath().resolve("vscode/bin/code")).exists().hasContent("#!/bin/bash\n" + "echo \"Test for linux and Mac\"");

    assertThat(context.getSoftwarePath().resolve("vscode/.ide.software.version")).exists().hasContent("1.92.1");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed vscode in version 1.92.1");
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin mockedPlugin (1/1)'.");
  }

  @Test
  void testConfigureToolArgsSetsWslEnvVarOnWsl() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    context.setSystemInfo(SystemInfoMock.LINUX_WSL_X64);
    Vscode commandlet = new Vscode(context);
    EnvCapturingProcessContext pc = new EnvCapturingProcessContext(context);
    // act
    commandlet.configureToolArgs(pc, ProcessMode.DEFAULT, List.of());
    // assert
    assertThat(pc.getEnvVar("DONT_PROMPT_WSL_INSTALL")).isEqualTo("1");
  }

  @Test
  void testConfigureToolArgsDoesNotSetWslEnvVarOnNonWsl() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Vscode commandlet = new Vscode(context);
    EnvCapturingProcessContext pc = new EnvCapturingProcessContext(context);
    // act
    commandlet.configureToolArgs(pc, ProcessMode.DEFAULT, List.of());
    // assert
    assertThat(pc.getEnvVar("DONT_PROMPT_WSL_INSTALL")).isNull();
  }

  /**
   * Tests that {@code VSCODE_OPTIONS} is honoured by appending its tokens as additional command-line arguments when starting the IDE (analogue to the
   * global {@code IDE_OPTIONS} used for IDEasy itself, see issue #788).
   */
  @Test
  void testRunAddsVscodeOptions() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("VSCODE_OPTIONS", "--wait --new-window");
    CapturingVscode commandlet = new CapturingVscode(context);
    // act
    commandlet.run();
    // assert
    assertThat(commandlet.lastArgs).contains("--wait", "--new-window");
  }

  @Test
  void testVscodiumInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODIUM);
    Vscode vscodium = new Vscode(context);

    // install
    vscodium.install();

    // assert
    checkVscodiumInstallation(context);
  }

  @Test
  void testVscodiumRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODIUM);
    Vscode vscodium = new Vscode(context);

    // install
    vscodium.run();

    // assert
    checkVscodiumInstallation(context);
  }

  @Test
  void testVscodiumSkipsPluginWithVscodiumInExcludedEditions() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODIUM);
    CapturingVscode vscodiumCommandlet = new CapturingVscode(context);
    ToolPluginDescriptor excludedPlugin = new ToolPluginDescriptor("publisher.excluded", "excludedPlugin", null,
        "1.0.0", true, Set.of(), Set.of("vscodium"));
    List<ToolPluginDescriptor> plugins = List.of(excludedPlugin);

    // act
    vscodiumCommandlet.installPluginsForTest(plugins, new ProcessContextTestImpl(context));

    // assert
    assertThat(vscodiumCommandlet.lastArgs).isEmpty();
    assertThat(context).logAtDebug().hasMessage("Skipping plugin 'excludedPlugin' (excluded for edition 'vscodium').");
  }

  @Test
  void testVscodeInstallsPluginExcludedForVscodium() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    CapturingVscode vscodeCommandlet = new CapturingVscode(context);
    ToolPluginDescriptor excludedPlugin = new ToolPluginDescriptor("publisher.excluded", "excludedPlugin", null,
        "1.0.0", true, Set.of(), Set.of("vscodium"));
    List<ToolPluginDescriptor> plugins = List.of(excludedPlugin);

    // act
    vscodeCommandlet.installPluginsForTest(plugins, new ProcessContextTestImpl(context));

    // assert
    assertThat(vscodeCommandlet.lastArgs).contains("--install-extension", "publisher.excluded@1.0.0");
  }

  @Test
  void testCsvParsingOfExcludedEditions() {
    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);

    // act
    ToolPluginDescriptor excludedPlugin = ToolPluginDescriptor.of(
        context.getSettingsPath().resolve("vscode/plugins/excludedPlugin.properties"), context, false);
    Set<String> excludedEditions = excludedPlugin.excludedEditions();

    // assert
    assertThat(excludedEditions).contains("vscode", "vscodium");
  }

  /**
   * Tests that {@link Vscode#importRepository(Path)} creates a multi-root {@code .code-workspace} file for a Maven project so the project is opened as a
   * project root on launch.
   */
  @Test
  void testVscodeMvnRepositoryImport() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);
    Path repositoryPath = context.getWorkspacePath().resolve("test_mvn");

    // act
    vscodeCommandlet.importRepository(repositoryPath);

    // assert
    Path workspaceFile = context.getWorkspacePath().resolve("ide.code-workspace");
    assertThat(workspaceFile).exists().content().contains("folders").contains("test_mvn");
  }

  /**
   * Tests that {@link Vscode#importRepository(Path)} accumulates multiple imported projects as the multiple roots of a single {@code .code-workspace}
   * file and does not create duplicates when a project is imported twice.
   */
  @Test
  void testVscodeRepositoryImportAccumulatesMultipleProjects() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);
    Path workspaceFile = context.getWorkspacePath().resolve("ide.code-workspace");

    // act: import two distinct projects into the same workspace
    vscodeCommandlet.importRepository(context.getWorkspacePath().resolve("test_mvn"));
    vscodeCommandlet.importRepository(context.getWorkspacePath().resolve("test_mvn2"));

    // assert: both projects are roots of the single workspace
    assertThat(context.getFileAccess().readFileContent(workspaceFile)).contains("test_mvn", "test_mvn2");
    assertThat(countOccurrences(context.getFileAccess().readFileContent(workspaceFile), "\"path\"")).isEqualTo(2);

    // act: re-import an already imported project
    vscodeCommandlet.importRepository(context.getWorkspacePath().resolve("test_mvn"));

    // assert: no duplicate is added
    assertThat(countOccurrences(context.getFileAccess().readFileContent(workspaceFile), "\"path\"")).isEqualTo(2);
  }

  /**
   * Tests that {@link Vscode#importRepository(Path)} drops a root from the {@code .code-workspace} file when the corresponding project folder no longer
   * exists on disk (auto-cleanup).
   */
  @Test
  void testVscodeRepositoryImportRemovesDeletedProject() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);
    Path workspacePath = context.getWorkspacePath();
    Path workspaceFile = workspacePath.resolve("ide.code-workspace");
    Path deletedProject = workspacePath.resolve("test_mvn");
    Path keptProject = workspacePath.resolve("test_mvn2");

    // act: import two projects, then delete one of them from disk and import it again (or import the other)
    vscodeCommandlet.importRepository(deletedProject);
    vscodeCommandlet.importRepository(keptProject);
    deleteRecursively(deletedProject);
    vscodeCommandlet.importRepository(keptProject);

    // assert: the deleted project is pruned, the kept project remains
    String content = context.getFileAccess().readFileContent(workspaceFile);
    assertThat(content).doesNotContain("test_mvn\"").contains("test_mvn2");
    assertThat(countOccurrences(content, "\"path\"")).isEqualTo(1);
  }

  /**
   * Recursively deletes the given file or directory.
   */
  private static void deleteRecursively(Path path) {

    if (!Files.exists(path)) {
      return;
    }
    try (var paths = Files.walk(path)) {
      paths.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException e) {
          throw new IllegalStateException("Failed to delete " + p, e);
        }
      });
    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete " + path, e);
    }
  }

  /**
   * Counts the occurrences of the given needle in the given haystack.
   */
  private static int countOccurrences(String haystack, String needle) {

    int count = 0;
    int index = 0;
    while ((index = haystack.indexOf(needle, index)) != -1) {
      count++;
      index += needle.length();
    }
    return count;
  }

  /**
   * Tests that {@link Vscode#importRepository(Path)} creates a multi-root {@code .code-workspace} file for a Gradle project in a non-main workspace, with
   * the project path relative to that workspace.
   */
  @Test
  void testVscodeGradleRepositoryImport() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);
    Path repositoryPath = context.getWorkspacePath("test").resolve("subfolder/test_gradle");

    // act
    vscodeCommandlet.importRepository(repositoryPath);

    // assert
    Path workspaceFile = context.getWorkspacePath("test").resolve("ide.code-workspace");
    assertThat(workspaceFile).exists().content().contains("folders").contains("subfolder/test_gradle");
  }

  /**
   * Tests that {@link Vscode#importRepository(Path)} logs a warning and creates no {@code .code-workspace} file when the repository contains no supported
   * build descriptor.
   */
  @Test
  void testVscodeRepositoryImportWithoutSupportedBuildDescriptor() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);
    Path repositoryPath = context.getWorkspacePath().resolve("empty_project");
    context.getFileAccess().mkdirs(repositoryPath);

    // act
    vscodeCommandlet.importRepository(repositoryPath);

    // assert
    assertThat(context).logAtWarning().hasMessageContaining("No supported build descriptor was found for project import in");
    assertThat(context.getWorkspacePath().resolve("ide.code-workspace")).doesNotExist();
  }

  /**
   * Tests that {@link Vscode#getWorkspaceTarget()} falls back to the workspace folder when no {@code .code-workspace} file has been created yet.
   */
  @Test
  void testVscodeWorkspaceTargetFallsBackToFolder() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);

    // act & assert
    assertThat(vscodeCommandlet.getWorkspaceTarget()).isEqualTo(context.getWorkspacePath());
  }

  /**
   * Tests that {@link Vscode#getWorkspaceTarget()} returns the {@code .code-workspace} file so that VSCode opens it (and thus loads the imported projects
   * as project roots) once the file has been created by a repository import.
   */
  @Test
  void testVscodeWorkspaceTargetUsesWorkspaceFile() {

    // arrange
    IdeTestContext context = newContext(PROJECT_VSCODE);
    Vscode vscodeCommandlet = new Vscode(context);
    Path workspaceFile = context.getWorkspacePath().resolve("ide.code-workspace");
    context.getFileAccess().writeFileContent("{\"folders\": []}", workspaceFile);

    // act & assert
    assertThat(vscodeCommandlet.getWorkspaceTarget()).isEqualTo(workspaceFile);
  }


  /**
   * Test double for {@link Vscode} that captures CLI arguments passed to {@link #runTool(ProcessContext, ProcessMode, List)} so tests can assert command
   * construction without spawning an external process.
   */
  private static class CapturingVscode extends Vscode {

    private List<String> lastArgs;

    private CapturingVscode(IdeTestContext context) {

      super(context);
      this.lastArgs = List.of();
    }

    @Override
    public ProcessResult runTool(ProcessContext pc, ProcessMode processMode, List<String> args) {

      // Capture effective CLI args for assertions in unit tests.
      this.lastArgs = new ArrayList<>(args);
      // Return a successful dummy result to keep tests isolated from real VS Code execution.
      return new ProcessResultImpl("code", "code", 0, List.of());
    }

    /** Exposes the protected {@link com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet#installPlugins(Collection, ProcessContext)} for testing. */
    public void installPluginsForTest(Collection<ToolPluginDescriptor> plugins, ProcessContext pc) {
      installPlugins(plugins, pc);
    }
  }

  /**
   * {@link ProcessContextTestImpl} subclass that captures calls to {@link #withEnvVar(String, String)} for test assertions.
   */
  private static class EnvCapturingProcessContext extends ProcessContextTestImpl {

    private final Map<String, String> capturedEnvVars = new HashMap<>();

    private EnvCapturingProcessContext(IdeTestContext context) {

      super(context);
    }

    @Override
    public ProcessContext withEnvVar(String key, String value) {

      this.capturedEnvVars.put(key, value);
      return super.withEnvVar(key, value);
    }

    String getEnvVar(String key) {

      return this.capturedEnvVars.get(key);
    }
  }

  private void checkVscodiumInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("vscode/bin/codium.cmd")).exists().hasContent("@echo test for windows");
    assertThat(context.getSoftwarePath().resolve("vscode/bin/codium")).exists().hasContent("#!/bin/bash\n" + "echo \"Test for linux and Mac\"");

    assertThat(context.getSoftwarePath().resolve("vscode/.ide.software.version")).exists().hasContent("1.116.02821");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed vscode/vscodium in version 1.116.02821");
  }
}
