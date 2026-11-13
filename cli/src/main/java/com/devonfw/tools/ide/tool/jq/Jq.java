package com.devonfw.tools.ide.tool.jq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;

/**
 * {@link AbstractToolCommandlet} for <a href="https://jqlang.github.io/jq/">jq</a>, a lightweight and flexible command-line JSON processor.
 * <p>
 * jq is distributed as a self-contained, platform-specific native binary (no archive), so {@link #isExtract() is not extracted} and the
 * downloaded file is renamed to the executable name (with a {@code .exe} suffix on Windows) to match {@link #getBinaryName()}.
 */
public class Jq extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Jq(IdeContext context) {

    super(context, "jq", Set.of(Tag.JSON));
  }

  @Override
  public String getToolHelpArguments() {

    return "-h";
  }

  @Override
  protected boolean isExtract() {

    // jq is distributed as a raw native binary per OS/architecture, not as an archive to extract.
    return false;
  }

  @Override
  protected void installDownloadedToolPayload(ToolInstallRequest request, Path installationPath, Path downloadedToolFile) {

    // jq is a raw, platform-specific binary (no archive) so the download must be moved instead of extracted, and renamed to the
    // executable name. In a real install the download is the release asset (e.g. jq-linux-amd64); the {@code ToolRepositoryMock}
    // hands us the (OS-specific) fixture folder in COPY mode, whose sole entry is the extensionless binary.
    String binaryName = getBinaryName();
    if (this.context.getSystemInfo().isWindows()) {
      binaryName += ".exe";
    }
    Path source = Files.isDirectory(downloadedToolFile) ? downloadedToolFile.resolve(getBinaryName()) : downloadedToolFile;
    this.context.getFileAccess().mkdirs(installationPath);
    this.context.getFileAccess().move(source, installationPath.resolve(binaryName));
  }
}
