package com.devonfw.tools.ide.tool.uv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.python.PythonUvListEntry;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link AbstractToolCommandlet} for <a href="https://docs.astral.sh/uv/">uv</a>.
 */
public class Uv extends AbstractLocalToolCommandlet {

  /** The folder name of the uv tool store inside the per-project {@code IDE_HOME} directory (see {@link #setEnvironment}). */
  private static final String UV_TOOL_FOLDER = ".uv-tools";

  /**
   * The legacy uv tool store location, introduced by #1518, before the store was made per-project (see #2561): it sat inside the per-project {@code software}
   * folder ({@code <IDE_HOME>/software/python/tools}). Used to migrate previously installed tools into {@link #UV_TOOL_FOLDER}.
   */
  private static final Path LEGACY_UV_TOOL_DIR = Path.of("software", "python", "tools");

  private static final Logger LOG = LoggerFactory.getLogger(Uv.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Uv(IdeContext context) {

    super(context, "uv", Set.of(Tag.PYTHON));
  }

  /**
   * Installs a pristine, unmodified {@code Python} interpreter of the given version into the given directory using {@code uv python install}. {@code uv} lays
   * the interpreter out in a nested {@code cpython-«version»-«platform»} folder and adds management files (e.g. {@code .gitignore}, {@code .lock}) to the given
   * directory; the caller is responsible for relocating the actual interpreter folder and keeping the target directory pristine.
   *
   * @param installDir the directory passed to {@code uv} via {@code --install-dir}
   * @param resolvedVersion the {@link VersionIdentifier} of the {@code Python} version to install
   * @param processContext the {@link ProcessContext} used to execute the {@code uv} command
   */
  public void installInterpreter(Path installDir, VersionIdentifier resolvedVersion, ProcessContext processContext) {

    ProcessResult result = runTool(processContext, ProcessMode.DEFAULT_CAPTURE,
        List.of("python", "install", "--install-dir", installDir.toString(), resolvedVersion.toString()));
    assert result.isSuccessful();
  }

  /**
   * Creates a virtual environment at the given target path, backed by the given pristine {@code Python} interpreter.
   *
   * @param venvPath the {@link Path} where the virtual environment is created
   * @param interpreterPath the {@link Path} of the pristine {@code Python} interpreter to back the virtual environment
   * @param processContext the {@link ProcessContext} used to execute the {@code uv} command
   */
  public void createVirtualEnvironment(Path venvPath, Path interpreterPath, ProcessContext processContext) {

    ProcessResult result = runTool(processContext, ProcessMode.DEFAULT_CAPTURE,
        List.of("venv", venvPath.toString(), "--python", interpreterPath.toString(), "--allow-existing", "--seed"));
    assert result.isSuccessful();
  }

  /**
   * Installs the given tool (e.g. {@code ruff}) into the uv tool store so that it is available immediately.
   *
   * @param tool the name of the tool to install (e.g. {@code ruff})
   * @param processContext the {@link ProcessContext} used to execute the {@code uv} command
   */
  public void installTool(String tool, ProcessContext processContext) {

    // best effort: a failure to install the tool (e.g. offline) must not fail the python installation
    runTool(processContext.errorHandling(ProcessErrorHandling.NONE), ProcessMode.DEFAULT_CAPTURE, List.of("tool", "install", tool));
  }

  private static final ObjectMapper MAPPER = JsonMapping.create();

  /**
   * Parses the JSON output of {@code uv python list --output-format json} into a list of {@link PythonUvListEntry entries}.
   *
   * @param jsonLines the captured standard output lines of the {@code uv} command.
   * @return the {@link List} of parsed {@link PythonUvListEntry entries}.
   */
  public List<PythonUvListEntry> parsePythonListJson(List<String> jsonLines) {

    String json = String.join("\n", jsonLines).trim();
    List<PythonUvListEntry> entries = new ArrayList<>();
    if (json.isEmpty()) {
      return entries;
    }
    try {
      JsonNode root = MAPPER.readTree(json);
      if (root.isArray()) {
        for (JsonNode node : root) {
          JsonNode versionNode = node.get("version");
          if ((versionNode != null) && !versionNode.isNull()) {
            JsonNode implNode = node.get("implementation");
            String implementation = (implNode != null && !implNode.isNull()) ? implNode.asText() : null;
            entries.add(new PythonUvListEntry(versionNode.asText(), implementation));
          }
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse JSON output of 'uv python list'.", e);
    }
    return entries;
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    Path ideHome = this.context.getIdeHome();
    if (ideHome == null) {
      return;
    }
    // uv tool install places the tool in ~/.local/share/uv/tools by default - we want that to be per-project so projects don't collide (see #352).
    // The per-project IDE instance directory (IDE_HOME) is therefore used as the uv tool store.
    Path uvToolDir = ideHome.resolve(UV_TOOL_FOLDER);
    migrateUvToolStore(ideHome, uvToolDir);
    environmentContext.withEnvVar("UV_TOOL_DIR", uvToolDir.toString());
    environmentContext.withEnvVar("UV_TOOL_BIN_DIR", uvToolDir.resolve("bin").toString());
    environmentContext.withPathEntry(uvToolDir.resolve("bin"));
  }

  /**
   * One-time migration of the per-project uv tool store into {@link #UV_TOOL_FOLDER} (see #2561). Before the store was made per-project it lived in the
   * legacy location {@link #LEGACY_UV_TOOL_DIR} inside the per-project {@code software} folder (see #1518), so previously installed uv tools would otherwise be
   * left behind. The legacy store is moved into place if it still exists and the target is free; if the target already holds tools they are kept and the
   * legacy store is backed up instead. This is best effort and never fails the environment setup (a failure would break PATH resolution for every uv tool).
   *
   * @param ideHome the per-project {@code IDE_HOME} directory (never {@code null} here)
   * @param uvToolDir the new per-project uv tool store location ({@code <IDE_HOME>/.uv-tools})
   */
  private void migrateUvToolStore(Path ideHome, Path uvToolDir) {

    Path legacyToolDir = ideHome.resolve(LEGACY_UV_TOOL_DIR);
    if (!Files.isDirectory(legacyToolDir)) {
      return;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    try {
      if (Files.exists(uvToolDir)) {
        // the per-project store already holds tools - keep those and preserve the legacy ones via a backup rather than losing them
        LOG.warn("Uv tool store {} already exists - moving legacy store {} to backup instead.", uvToolDir, legacyToolDir);
        fileAccess.backup(legacyToolDir);
      } else {
        LOG.info("Migrating legacy uv tool store {} into the per-project store {}.", legacyToolDir, uvToolDir);
        fileAccess.move(legacyToolDir, uvToolDir);
      }
    } catch (Exception e) {
      LOG.warn("Failed to migrate the legacy uv tool store from {} to {} - it is kept at the legacy location.", legacyToolDir, uvToolDir, e);
    }
  }
}
