package com.devonfw.tools.ide.tool.python;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
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
   * Runs {@code uv python list} and parses the result. Extracted as a protected method so tests can stub the {@code uv} interaction.
   *
   * @return the parsed {@link PythonUvListEntry entries}.
   */
  protected List<PythonUvListEntry> fetchUvPythonList() {

    Uv uv = this.context.getCommandletManager().getCommandlet(Uv.class);
    // We use the runTool variant that ensures uv is installed before running the command.
    ProcessResult result = uv.runTool(ProcessMode.DEFAULT_CAPTURE, null, ProcessErrorHandling.THROW_CLI,
        List.of("python", "list", "--all-versions", "--only-downloads", "--output-format", "json", "--no-config"));
    return uv.parsePythonListJson(result.getOut());
  }

  @Override
  protected UrlDownloadFileMetadata getMetadata(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    throw new UnsupportedOperationException(
        "Python is installed via uv and is never downloaded from a URL. This repository only resolves versions.");
  }
}
