package com.devonfw.tools.ide.tool.git;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.HttpClientFactory;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackage;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.custom.CustomToolMetadata;
import com.devonfw.tools.ide.tool.custom.CustomToolRepositoryImpl;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://git-scm.com/">Git</a>.
 */
public class Git extends GlobalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Git.class);

  private static final String RELEASES_URL = "https://github.com/git-for-windows/git/releases";

  private static final Pattern LATEST_RELEASE_PATTERN = Pattern.compile(".*/releases/tag/(v(\\d+\\.\\d+\\.\\d+)\\.windows\\.(\\d+))");

  private ToolRepository latestInstallerRepository;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Git(IdeContext context) {
    super(context, "git", Set.of(Tag.GIT));
  }

  /**
   * @return {@code false} since Git is required to create a project and can therefore also be installed outside of a project.
   */
  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  /**
   * A fresh Windows machine has neither Git nor the URL metadata that can only be cloned with Git. Only in this case the latest installer is used directly (see
   * {@link #fetchLatestInstaller()}). On Linux and macOS no download is needed since Git is installed via the native package manager.
   *
   * @return the {@link ToolRepository}.
   */
  @Override
  public ToolRepository getToolRepository() {

    if ((this.latestInstallerRepository == null) && isFreshWindowsMachine()) {
      this.latestInstallerRepository = createLatestInstallerRepository();
    }
    return (this.latestInstallerRepository != null) ? this.latestInstallerRepository : super.getToolRepository();
  }

  private boolean isFreshWindowsMachine() {

    if (!this.context.getSystemInfo().isWindows()) {
      return false;
    }
    Path urlsPath = this.context.getUrlsPath();
    boolean urlMetadataAvailable = (urlsPath != null) && Files.isDirectory(urlsPath.resolve(getName()));
    return !urlMetadataAvailable && (this.context.getGitContext().findGit() == null);
  }

  /**
   * @return the {@link ToolRepository} that only provides the {@link #fetchLatestInstaller() latest installer}.
   */
  protected ToolRepository createLatestInstallerRepository() {

    return new CustomToolRepositoryImpl(this.context, List.of(fetchLatestInstaller()));
  }

  /**
   * GitHub redirects the latest release to its tag (e.g. {@code v2.56.0.windows.4}), from which the version and the download URL of the installer are derived.
   * This is the same scheme as in the {@code GitUrlUpdater} that generates the URL metadata. No checksum is available this way, so the download is not verified
   * by a checksum.
   *
   * @return the {@link CustomToolMetadata} of the latest installer.
   */
  CustomToolMetadata fetchLatestInstaller() {

    String latestReleaseUrl = getReleasesUrl() + "/latest";
    if (this.context.getNetworkStatus().isOffline()) {
      throw CliOfflineException.ofDownloadViaUrl(latestReleaseUrl);
    }
    String finalUrl = followRedirects(latestReleaseUrl);
    Matcher matcher = LATEST_RELEASE_PATTERN.matcher(finalUrl);
    if (!matcher.matches()) {
      throw new CliException("Received unexpected latest Git release '" + finalUrl + "' from " + latestReleaseUrl + ".\n"
          + IdeContext.PLEASE_DOWNLOAD_AND_INSTALL_GIT + ":\n " + IdeContext.WINDOWS_GIT_DOWNLOAD_URL);
    }
    String tag = matcher.group(1);
    String gitVersion = matcher.group(2);
    String revision = matcher.group(3);
    String version = gitVersion + "." + revision;
    // the revision is only part of the file name once it is larger than 1 (e.g. "Git-2.56.0-64-bit.exe" for windows.1, "Git-2.56.0.2-64-bit.exe" for windows.2)
    String fileVersion = "1".equals(revision) ? gitVersion : version;
    SystemInfo systemInfo = this.context.getSystemInfo();
    String architecture = switch (systemInfo.getArchitecture()) {
      case X64 -> "64-bit";
      case ARM64 -> "arm64";
    };
    String installerUrl = getReleasesUrl() + "/download/" + tag + "/Git-" + fileVersion + "-" + architecture + ".exe";
    LOG.info("No URL metadata available yet, using latest Git installer from {} without checksum verification.", installerUrl);
    return new CustomToolMetadata(getName(), version, systemInfo.getOs(), systemInfo.getArchitecture(), installerUrl, null, getReleasesUrl());
  }

  private static String followRedirects(String url) {

    try (HttpClient client = HttpClientFactory.create()) {
      HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
      return client.send(request, HttpResponse.BodyHandlers.discarding()).uri().toString();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to determine the latest Git release from " + url, e);
    }
  }

  /**
   * @return the URL of the releases of Git for Windows. May be overridden for testing.
   */
  protected String getReleasesUrl() {

    return RELEASES_URL;
  }

  /**
   * Skips the check for the latest installer, as the CVE information is part of the URL metadata that does not exist in this case.
   */
  @Override
  protected VersionIdentifier cveCheck(ToolInstallRequest request) {

    if (getToolRepository() == this.latestInstallerRepository) {
      return request.getRequested().getResolvedVersion();
    }
    return super.cveCheck(request);
  }

  @Override
  protected List<NativePackage> getNativePackages() {
    return List.of(
        NativePackage.of(NativePackageManager.APT, "git"),
        NativePackage.of(NativePackageManager.ZYPPER, "git"),
        NativePackage.of(NativePackageManager.BREW, "git"));
  }

  @Override
  public String getWindowsRegistryAppName() {
    return "Git";
  }

  /**
   * The installer is awaited instead of being started in the background. Background processes are started via bash that may not exist yet and whose path
   * conversion (MSYS) would turn an argument like {@code /VERYSILENT} into {@code C:/Program Files/Git/VERYSILENT}.
   */
  @Override
  protected ProcessMode getInstallerProcessMode() {

    return ProcessMode.DEFAULT;
  }

  /**
   * @return the arguments for the installer. The first installation shows the installation wizard, while upgrades and the batch mode are silent.
   */
  @Override
  protected List<String> getInstallerArguments() {

    if ((getInstalledVersion() != null) || this.context.isBatchMode()) {
      return List.of("/VERYSILENT", "/NORESTART", "/NOCANCEL", "/SP-");
    }
    return List.of("/NORESTART", "/NOCANCEL", "/SP-");
  }

  /**
   * On macOS Git is installed via Homebrew (see {@link #getNativePackages()}) rather than via the DMG installer artifact, so the package manager based
   * installation flow is used there too - exactly like on Linux. On Windows the standard installer artifact is still used.
   *
   * @return {@code true} if Git is installed via the OS native package manager.
   */
  @Override
  protected boolean isPackageManagerBasedInstallation() {
    return this.context.getSystemInfo().isLinux() || this.context.getSystemInfo().isMac();
  }
}
