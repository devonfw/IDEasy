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

  /**
   * The folder name of the uv tool store, located inside the per-project {@code software} folder (see {@link #getUvToolStorePath}).
   */
  private static final String UV_TOOL_FOLDER = "uv-tools";

  /**
   * The legacy uv tool store location, introduced by #1518, before the store was made per-project (see #2561): it sat inside the per-project {@code software}
   * folder ({@code <IDE_HOME>/software/python/tools}). Used to migrate previously installed tools into {@link #UV_TOOL_FOLDER}.
   */
  private static final Path LEGACY_UV_TOOL_DIR = Path.of("software", "python", "tools");

  /**
   * The uv tool store location used by #2561 before this fix moved the store into the per-project {@code software} folder: it sat in a dot-folder directly
   * under {@code IDE_HOME} ({@code <IDE_HOME>/.uv-tools}). Because a launchable binary there is invisible to {@link com.devonfw.tools.ide.common.SystemPath},
   * uv-based tools installed there were neither resolvable nor uninstallable. Used to migrate those tools into {@link #UV_TOOL_FOLDER} so they are not left
   * behind.
   */
  private static final Path PREVIOUS_UV_TOOL_DIR = Path.of(".uv-tools");

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
   * @param context the {@link IdeContext}.
   * @return the {@link Path} to the per-project uv tool store (see {@link #UV_TOOL_FOLDER}), or {@code null} if not running inside a project. The store is
   *     placed inside the per-project {@code software} folder (not a dot-folder) so that {@link com.devonfw.tools.ide.common.SystemPath} registers the
   *     {@code uv-tools/bin} launchers automatically - this makes uv-based tools (e.g. {@code ruff}, {@code just}) resolvable and thus uninstallable via
   *     {@code ide uninstall «tool»} (see <a href="https://github.com/devonfw/IDEasy/issues/2561">#2561</a>), mirroring the per-project global npm prefix.
   */
  public static Path getUvToolStorePath(IdeContext context) {

    Path softwarePath = context.getSoftwarePath();
    return softwarePath == null ? null : softwarePath.resolve(UV_TOOL_FOLDER);
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
      // running in global mode (no project) - there is no per-project uv tool store to point to
      return;
    }
    // uv tool install places the tool in ~/.local/share/uv/tools by default - we want that to be per-project so projects don't collide (see #352).
    // The store is placed inside the per-project "software" folder so that its launchers are picked up by SystemPath automatically (see #2561).
    Path uvToolDir = getUvToolStorePath(this.context);
    migrateUvToolStore(ideHome, uvToolDir);
    environmentContext.withEnvVar("UV_TOOL_DIR", uvToolDir.toString());
    environmentContext.withEnvVar("UV_TOOL_BIN_DIR", uvToolDir.resolve("bin").toString());
    environmentContext.withPathEntry(uvToolDir.resolve("bin"));
  }

  /**
   * One-time migration of the per-project uv tool store into {@link #UV_TOOL_FOLDER} (see #2561). Previously the store lived in two locations that are now
   * deprecated, so previously installed uv tools must be migrated into place or they would be left behind and become uninstallable: the legacy location
   * {@link #LEGACY_UV_TOOL_DIR} inside the per-project {@code software} folder (see #1518) and the {@link #PREVIOUS_UV_TOOL_DIR} dot-folder directly under
   * {@code IDE_HOME} that #2561 introduced before this fix. For each location that still exists, the tools are moved into {@link #UV_TOOL_FOLDER} if it is
   * still free and backed up instead (keeping the migrated tools) once it already holds tools. This is best effort and never fails the environment setup (a
   * failure would break PATH resolution for every uv tool).
   *
   * @param ideHome the per-project {@code IDE_HOME} directory (never {@code null} here)
   * @param uvToolDir the new per-project uv tool store location ({@code <IDE_HOME>/software/uv-tools})
   */
  private void migrateUvToolStore(Path ideHome, Path uvToolDir) {

    // migrate from the two deprecated per-project store locations in order of age (oldest first) so that a migration into a free target always wins
    for (Path legacyToolDir : List.of(ideHome.resolve(LEGACY_UV_TOOL_DIR), ideHome.resolve(PREVIOUS_UV_TOOL_DIR))) {
      migrateUvToolStoreFrom(legacyToolDir, uvToolDir);
    }
  }

  /**
   * Migrates the uv tool store from the given (deprecated) {@link #LEGACY_UV_TOOL_DIR legacy} or {@link #PREVIOUS_UV_TOOL_DIR previous} location into the
   * current per-project store {@link #UV_TOOL_FOLDER}, moving it while the target is still free and backing it up once the target already holds tools (see
   * {@link #migrateUvToolStore}). Does nothing if the source does not exist.
   *
   * @param legacyToolDir the deprecated per-project uv tool store location to migrate from
   * @param uvToolDir the current per-project uv tool store location to migrate into
   */
  private void migrateUvToolStoreFrom(Path legacyToolDir, Path uvToolDir) {

    if (!Files.isDirectory(legacyToolDir)) {
      return;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    try {
      if (Files.exists(uvToolDir)) {
        // the per-project store already holds tools - keep those and preserve this store via a backup rather than losing them
        LOG.warn("Uv tool store {} already exists - moving uv tool store {} to backup instead.", uvToolDir, legacyToolDir);
        fileAccess.backup(legacyToolDir);
      } else {
        LOG.info("Migrating uv tool store {} into the per-project store {}.", legacyToolDir, uvToolDir);
        fileAccess.move(legacyToolDir, uvToolDir);
      }
    } catch (Exception e) {
      LOG.warn("Failed to migrate the uv tool store from {} to {} - it is kept at its previous location.", legacyToolDir, uvToolDir, e);
    }
  }
}
