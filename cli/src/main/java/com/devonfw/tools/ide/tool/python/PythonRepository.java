package com.devonfw.tools.ide.tool.python;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.repository.AbstractToolRepository;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link com.devonfw.tools.ide.tool.repository.ToolRepository ToolRepository} for Python.
 */
public class PythonRepository extends AbstractToolRepository {

  /** {@link #getId() ID} of this repository. */
  public static final String ID = "python";

  private static final Logger LOG = LoggerFactory.getLogger(PythonRepository.class);

  private List<VersionIdentifier> cachedVersions;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public PythonRepository(IdeContext context) {

    super(context);
  }

  @Override
  public String getId() {

    return ID;
  }

  @Override
  public List<String> getSortedEditions(String tool) {

    return List.of(tool);
  }

  @Override
  public List<VersionIdentifier> getSortedVersions(String tool, String edition, ToolCommandlet toolCommandlet) {

    if (this.cachedVersions == null) {
      this.cachedVersions = computeSortedVersions();
    }
    return this.cachedVersions;
  }

  private List<VersionIdentifier> computeSortedVersions() {

    List<PythonUvListEntry> entries = fetchUvPythonList();
    List<VersionIdentifier> versions = new ArrayList<>();
    for (PythonUvListEntry entry : entries) {
      if (entry.isCpython()) {
        VersionIdentifier version = VersionIdentifier.of(entry.version());
        if ((version != null) && !versions.contains(version)) {
          versions.add(version);
        }
      }
    }
    versions.sort(Comparator.reverseOrder());
    LOG.debug("Found {} Python version(s) available via uv for the current platform.", versions.size());
    return versions;
  }

  /**
   * Runs {@code uv python list} and parses the result.
   * <p>
   * This method never triggers an installation of {@code uv}: if {@code uv} is not installed (e.g. when listing versions for auto-completion), a warning is logged
   * and an empty list is returned. The installation path ensures that {@code uv} is present before the Python version is resolved (see {@link Python#completeRequest}).
   *
   * @return the parsed {@link PythonUvListEntry entries}, or an empty list if {@code uv} is not installed.
   */
  protected List<PythonUvListEntry> fetchUvPythonList() {

    Uv uv = this.context.getCommandletManager().getCommandlet(Uv.class);
    if (!uv.isInstalled()) {
      LOG.warn("uv is not installed, run 'ide install uv'");
      return List.of();
    }
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI);
    this.context.setEnvironmentOfInstalledTools(pc);
    ProcessResult result = uv.runTool(pc, ProcessMode.DEFAULT_CAPTURE,
        List.of("python", "list", "--all-versions", "--only-downloads", "--output-format", "json", "--no-config"));
    return uv.parsePythonListJson(result.getOut());
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    throw new UnsupportedOperationException(
        "Python is installed via uv and is never downloaded from a URL. This repository only resolves versions.");
  }
}
