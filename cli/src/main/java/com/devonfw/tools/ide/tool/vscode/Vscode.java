package com.devonfw.tools.ide.tool.vscode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * {@link ToolCommandlet} for <a href="https://code.visualstudio.com/">vscode</a>.
 */
public class Vscode extends IdeToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Vscode.class);

  /** The {@link #getConfiguredEdition() edition} for VSCodium. */
  private static final String EDITION_VSCODIUM = "vscodium";

  /** Folder name for VSCode per-project configuration. */
  private static final String FOLDER_VSCODE = ".vscode";

  /**
   * Name of the VSCode multi-root workspace file that is generated in the workspace root and opened on launch so that the imported projects are loaded as
   * project roots (e.g. by the Java language server).
   */
  private static final String WORKSPACE_FILE = "ide.code-workspace";

  /** Map of build tool classes to their corresponding VSCode workspace template. */
  private static final Map<Class<? extends LocalToolCommandlet>, String> BUILD_TOOL_TO_TEMPLATE =
      Map.of(Mvn.class, WORKSPACE_FILE, Gradle.class, WORKSPACE_FILE);

  /** The {@code folders} key of a VSCode multi-root {@code .code-workspace} file. */
  private static final String FOLDERS_KEY = "folders";

  /** The {@code path} key of a single entry in the {@link #FOLDERS_KEY folders} array. */
  private static final String FOLDER_PATH_KEY = "path";

  /** JSON mapper for reading/writing the (untyped) {@code .code-workspace} file. */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Vscode(IdeContext context) {

    super(context, "vscode", Set.of(Tag.VS_CODE));
  }

  @Override
  protected String getBinaryName() {

    if (EDITION_VSCODIUM.equals(getConfiguredEdition())) {
      return "codium";
    }
    return "code";
  }


  @Override
  public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {

    List<String> extensionsCommands = new ArrayList<>();
    extensionsCommands.add("--force");
    extensionsCommands.add("--install-extension");
    String extensionInstallTarget = plugin.id();
    // If a version number was specified, add it to the extension identifier with the format "extensionId@version"
    boolean versionSpecified = (plugin.version() != null) && !plugin.version().isBlank();
    if (versionSpecified) {
      extensionInstallTarget = extensionInstallTarget + "@" + plugin.version();
    }
    extensionsCommands.add(extensionInstallTarget);
    ProcessResult result = runTool(pc, ProcessMode.DEFAULT_CAPTURE, extensionsCommands);
    if (result.isSuccessful()) {
      if (versionSpecified) {
        IdeLogLevel.SUCCESS.log(LOG, "Successfully installed plugin: {} with version: {}", plugin.name(), plugin.version());
      } else {
        IdeLogLevel.SUCCESS.log(LOG, "Successfully installed plugin: {}", plugin.name());
      }
      step.success();
      return true;
    }
    if (versionSpecified) {
      IdeLogLevel.ERROR.log(LOG, "Failed to install plugin: {} with version: {}", plugin.name(), plugin.version());
    } else {
      IdeLogLevel.ERROR.log(LOG, "Failed to install plugin: {}", plugin.name());
    }
    return false;
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, List<String> args) {

    if (this.context.getSystemInfo().isWsl()) {
      pc.withEnvVar("DONT_PROMPT_WSL_INSTALL", "1");
    }
    Path vsCodeConf = this.context.getWorkspacePath().resolve(".vscode/.userdata");
    pc.addArg("--new-window");
    pc.addArg("--user-data-dir=" + vsCodeConf);
    Path vsCodeExtensionFolder = this.context.getIdeHome().resolve("plugins/vscode");
    pc.addArg("--extensions-dir=" + vsCodeExtensionFolder);
    // Open the multi-root workspace file (if present) so the imported projects are loaded as project roots; fall back to the workspace folder.
    pc.addArg(getWorkspaceTarget());
    super.configureToolArgs(pc, processMode, args);
  }

  /**
   * @return the {@link Path} to open when launching VSCode: the multi-root {@code .code-workspace} file in the workspace root if it exists (created via
   *     {@link #importRepository(Path)}), otherwise the workspace folder itself.
   */
  protected Path getWorkspaceTarget() {

    Path workspaceFolder = this.context.getWorkspacePath();
    Path workspaceFile = workspaceFolder.resolve(WORKSPACE_FILE);
    if (Files.exists(workspaceFile)) {
      return workspaceFile;
    }
    return workspaceFolder;
  }

  @Override
  protected Map<Class<? extends LocalToolCommandlet>, String> getBuildTool2TemplateMap() {

    return BUILD_TOOL_TO_TEMPLATE;
  }

  /**
   * The VSCode multi-root workspace template lives directly in the workspace root (no configuration sub-folder such as {@code .vscode}), so the template
   * folder is empty and the template file is placed in the root of both the settings repository and the workspace.
   *
   * @return an empty {@link String}, see {@link IdeToolCommandlet#getTemplateFolder()}.
   */
  @Override
  protected String getTemplateFolder() {

    return "";
  }

  /**
   * Merges the repository workspace template into the workspace's multi-root {@link #WORKSPACE_FILE} file (in the workspace root). The folders contributed
   * by the imported repository (with {@code PROJECT_PATH} resolved to the project's relative path) are <strong>appended</strong> to the existing
   * {@code folders} array, so that multiple imported repositories become the multiple roots of a single VSCode workspace. Roots that no longer exist on
   * disk are dropped (auto-cleanup) and duplicates (by {@code path}) are not added again.
   *
   * @param templateFile the resolved {@link Path} to the workspace template in the settings repository.
   * @param workspaceFile the {@link Path} to the workspace {@code .code-workspace} file to merge into.
   * @param environmentVariables the {@link EnvironmentVariables} to resolve variables (e.g. {@code PROJECT_PATH}) in the template.
   */
  @Override
  protected void doMergeTemplate(Path templateFile, Path workspaceFile, EnvironmentVariables environmentVariables) {

    List<String> newFolders = readResolvedFolders(templateFile, environmentVariables);
    ObjectNode workspace = readWorkspaceFile(workspaceFile);
    ArrayNode folders = workspace.get(FOLDERS_KEY) instanceof ArrayNode existingFolders ? existingFolders : workspace.putArray(FOLDERS_KEY);
    Path workspaceRoot = workspaceFile.getParent();
    // auto-cleanup: drop roots that no longer exist on disk
    for (int i = folders.size() - 1; i >= 0; i--) {
      if (isStaleFolder(folders.get(i), workspaceRoot)) {
        folders.remove(i);
      }
    }
    // add the imported project root (if not already present)
    for (String folderPath : newFolders) {
      if (!containsFolder(folders, folderPath)) {
        folders.addObject().put(FOLDER_PATH_KEY, folderPath);
      }
    }
    writeFileContent(workspace, workspaceFile);
    LOG.debug("Merged VSCode workspace file into {} ({} folder(s))", workspaceFile, folders.size());
  }

  /**
   * Reads the {@code folders} array of the given workspace template and resolves the {@code path} of each entry (e.g. {@code $[PROJECT_PATH]}) to a
   * concrete project path.
   */
  private List<String> readResolvedFolders(Path templateFile, EnvironmentVariables environmentVariables) {

    try {
      JsonNode template = MAPPER.readTree(this.context.getFileAccess().readFileContent(templateFile));
      List<String> result = new ArrayList<>();
      if (template.get(FOLDERS_KEY) instanceof JsonNode folders && folders.isArray()) {
        for (JsonNode folder : folders) {
          if (folder.get(FOLDER_PATH_KEY) instanceof TextNode pathNode) {
            result.add(environmentVariables.resolve(pathNode.asText(), templateFile, false));
          }
        }
      }
      return result;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read VSCode workspace template from " + templateFile, e);
    }
  }

  /**
   * @param workspaceFile the {@link Path} to the {@code .code-workspace} file to read.
   * @return the existing workspace JSON as an {@link ObjectNode}, or a new empty object if the file does not yet exist.
   */
  private ObjectNode readWorkspaceFile(Path workspaceFile) {

    if (!Files.exists(workspaceFile)) {
      return MAPPER.createObjectNode();
    }
    try {
      JsonNode node = MAPPER.readTree(this.context.getFileAccess().readFileContent(workspaceFile));
      return node instanceof ObjectNode objectNode ? objectNode : MAPPER.createObjectNode();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read VSCode workspace file " + workspaceFile, e);
    }
  }

  /**
   * @return {@code true} if the given {@code folders} array already contains a folder with the given {@code path}.
   */
  private boolean containsFolder(JsonNode folders, String folderPath) {

    for (JsonNode folder : folders) {
      if (folderPath.equals(folder.get(FOLDER_PATH_KEY).asText())) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param folder the folder entry of the {@code folders} array.
   * @param workspaceRoot the {@link Path} of the workspace root the {@code .code-workspace} file belongs to.
   * @return {@code true} if the folder's {@code path} is missing or no longer exists on disk (relative to the workspace root), so that it should be
   *     dropped (auto-cleanup).
   */
  private boolean isStaleFolder(JsonNode folder, Path workspaceRoot) {

    JsonNode pathNode = folder.get(FOLDER_PATH_KEY);
    if (!(pathNode instanceof TextNode textNode) || textNode.asText().isBlank()) {
      return false; // not a path entry -> keep it
    }
    return !Files.exists(workspaceRoot.resolve(textNode.asText()));
  }

  /**
   * Writes the given workspace JSON to the file, pretty-printed with two spaces (as expected by the VSCode {@code .code-workspace} format).
   */
  private void writeFileContent(JsonNode workspace, Path workspaceFile) {

    try {
      String content = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(workspace);
      this.context.getFileAccess().writeFileContent(content, workspaceFile);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to write VSCode workspace file " + workspaceFile, e);
    }
  }

}
