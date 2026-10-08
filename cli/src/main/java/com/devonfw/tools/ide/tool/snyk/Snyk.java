package com.devonfw.tools.ide.tool.snyk;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;

/**
 * {@link ToolCommandlet} for the <a href="https://github.com/snyk/cli">Snyk CLI</a>.
 * <p>
 * Note: the Snyk CLI keeps its configuration in the user's config directory, honoring {@code $XDG_CONFIG_HOME} when
 * set (otherwise the XDG default under the user home). Unlike tools that expose a dedicated config-dir variable, Snyk
 * only honors the generic XDG setting, so IDEasy does not relocate it into {@code $IDE_HOME/conf}; by default its
 * config lives in the user home. This is a conscious exception to the "config in {@code $IDE_HOME/conf}" guideline.
 * Authentication is likewise not managed by IDEasy: it is provided via the standard {@code SNYK_TOKEN} environment
 * variable, which the user or a CI environment sets independently of IDEasy.
 */
public class Snyk extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Snyk(IdeContext context) {

    super(context, "snyk", Set.of(Tag.SECURITY, Tag.IAC));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

  @Override
  protected boolean isExtract() {

    return false; // Snyk ships self-contained executables, not archives
  }

  @Override
  protected void installDownloadedToolPayload(ToolInstallRequest request, Path installationPath, Path downloadedToolFile) {

    // Snyk publishes raw binaries (snyk-linux / snyk-win.exe) that must be normalized to the
    // binary name the launcher resolves (snyk / snyk.exe) and made executable.
    // Note: the downloaded file has no extension, so the download cache name falls back to the
    // default extension, this is intentional as Snyk does not publish versioned archive names.
    this.context.getFileAccess().mkdirs(installationPath);
    String binaryName = this.context.getSystemInfo().isWindows() ? "snyk.exe" : "snyk";
    Path target = installationPath.resolve(binaryName);
    this.context.getFileAccess().move(downloadedToolFile, target);
    this.context.getFileAccess().makeExecutable(target);
  }
}
