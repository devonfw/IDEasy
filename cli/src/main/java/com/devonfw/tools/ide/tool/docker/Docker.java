package com.devonfw.tools.ide.tool.docker;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackage;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.PackageManagerCommand;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://www.docker.com/">docker</a> either as
 * <a href="https://rancherdesktop.io/">Rancher Desktop</a> or as
 * <a href="https://www.docker.com/products/docker-desktop/">Docker Desktop</a>.
 */
public class Docker extends GlobalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Docker.class);

  private static final String PODMAN = "podman";

  /** The {@link #getWindowsRegistryAppNames() edition name} of Docker Desktop. */
  private static final String DOCKER_EDITION = "docker";

  /** The {@link #getWindowsRegistryAppNames() edition name} of Rancher Desktop. */
  private static final String RANCHER_EDITION = "rancher";

  private static final Pattern DOCKER_DESKTOP_VERSION_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+){1,2})");

  private static final String EDITION_DOCKER = "docker";

  private Path downloadedDebPackageForDocker;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Docker(IdeContext context) {

    super(context, "docker", Set.of(Tag.DOCKER));
  }

  @Override
  public String getBinaryName() {
    return detectContainerRuntime();
  }

  private String detectContainerRuntime() {
    if (isCommandAvailable(this.tool)) {
      return this.tool;
    } else if (isCommandAvailable(PODMAN)) {
      return PODMAN;
    } else {
      return this.tool;
    }
  }

  @Override
  protected List<NativePackage> getNativePackages() {

    return getNativePackages(getConfiguredEdition());
  }

  /**
   * @param edition the edition to get the {@link NativePackage}s for. Use the {@link #getConfiguredEdition() configured edition} to install and the
   *     {@link #getInstalledEdition() installed edition} to uninstall.
   * @return the {@link List} of {@link NativePackage}s of the given edition.
   */
  private List<NativePackage> getNativePackages(String edition) {

    if (EDITION_DOCKER.equals(edition)) {

      List<Path> artifactPaths = (this.downloadedDebPackageForDocker == null) ? List.of() : List.of(this.downloadedDebPackageForDocker);

      return List.of(
          new NativePackage(
              NativePackageManager.APT,
              List.of("docker-desktop"),
              List.of("--allow-downgrades"),
              List.of(
                  "sudo install -m 0755 -d /etc/apt/keyrings",
                  "sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc",
                  "sudo chmod a+r /etc/apt/keyrings/docker.asc",
                  "echo \"deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] "
                      + "https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo \\\"$VERSION_CODENAME\\\") stable\" | "
                      + "sudo tee /etc/apt/sources.list.d/docker.list > /dev/null",
                  "sudo apt update"
              ),
              List.of(
                  "sudo rm -f /etc/apt/sources.list.d/docker.list",
                  "sudo rm -f /etc/apt/keyrings/docker.asc"
              ),
              artifactPaths
          )
      );
    }

    return List.of(
        new NativePackage(
            NativePackageManager.ZYPPER,
            List.of("rancher-desktop"),
            List.of("--no-gpg-checks"),
            List.of("sudo zypper addrepo https://download.opensuse.org/repositories/isv:/Rancher:/stable/rpm/isv:Rancher:stable.repo"),
            null
        ),
        new NativePackage(
            NativePackageManager.APT,
            List.of("rancher-desktop"),
            List.of("--allow-downgrades"),
            List.of(
                "curl -s https://download.opensuse.org/repositories/isv:/Rancher:/stable/deb/Release.key | "
                    + "gpg --dearmor | sudo dd status=none of=/usr/share/keyrings/isv-rancher-stable-archive-keyring.gpg",
                "echo 'deb [signed-by=/usr/share/keyrings/isv-rancher-stable-archive-keyring.gpg] "
                    + "https://download.opensuse.org/repositories/isv:/Rancher:/stable/deb/ ./' | "
                    + "sudo dd status=none of=/etc/apt/sources.list.d/isv-rancher-stable.list",
                "sudo apt update"
            ),
            List.of(
                "sudo rm -f /etc/apt/sources.list.d/isv-rancher-stable.list",
                "sudo rm -f /usr/share/keyrings/isv-rancher-stable-archive-keyring.gpg"
            )
        ),
        new NativePackage(NativePackageManager.YAY, List.of("rancher-desktop")),
        new NativePackage(NativePackageManager.BREW_CASK, List.of("docker"))
    );
  }

  @Override
  protected List<PackageManagerCommand> getUninstallPackageManagerCommands() {

    if (this.context.getSystemInfo().isMac()) {
      // the cask of the installed edition has to be removed, regardless of which edition is configured
      String cask = RANCHER_EDITION.equals(getInstalledEdition()) ? RANCHER_EDITION : EDITION_DOCKER;
      return List.of(new NativePackage(NativePackageManager.BREW_CASK, List.of(cask)).uninstall());
    }
    // the packages of the installed edition have to be removed, regardless of which edition is configured
    String installedEdition = getInstalledEdition();
    if (installedEdition == null) {
      return super.getUninstallPackageManagerCommands();
    }
    return getNativePackages(installedEdition).stream().map(NativePackage::uninstall).toList();
  }

  @Override
  public String getMacApplicationName() {

    return "Docker";
  }

  @Override
  public String getMacApplicationName(String edition) {

    return RANCHER_EDITION.equals(edition) ? "Rancher Desktop" : getMacApplicationName();
  }

  @Override
  protected ToolEditionAndVersion adjustRequestedEdition(ToolEditionAndVersion requested) {

    if (this.context.getSystemInfo().isLinux()) {
      ToolEdition edition = requested.getEdition();
      if (!RANCHER_EDITION.equals(edition.edition())) {
        LOG.warn("Docker Desktop is not yet supported by IDEasy on Linux, installing Rancher Desktop instead.");
        requested.replaceEdition(new ToolEdition(this.tool, RANCHER_EDITION));
      }
    }
    return requested;
  }

  @Override
  protected ToolInstallation doInstall(ToolInstallRequest request) {
    if (isDockerDesktopEditionConfigured()) {
      downloadDebPackageStepAndSetPackagePath(request.getRequested().getResolvedVersion());
    }
    return super.doInstall(request);
  }

  private void downloadDebPackageStepAndSetPackagePath(VersionIdentifier resolvedVersion) {
    ToolRepository toolRepository = this.context.getDefaultToolRepository();
    this.downloadedDebPackageForDocker = toolRepository.download(this.tool, EDITION_DOCKER, resolvedVersion, this);
  }

  @Override
  protected List<PackageManagerCommand> getInstallPackageManagerCommands(VersionIdentifier resolvedVersion) {
    if (!isDockerDesktopEditionConfigured()) {
      return super.getInstallPackageManagerCommands(resolvedVersion);
    }

    return getNativePackages().stream().map(nativePackage -> nativePackage.install(null)).toList();
  }

  @Override
  public boolean isExtract() {

    return switch (this.context.getSystemInfo().getOs()) {
      case WINDOWS -> false;
      case MAC -> this.context.getSystemInfo().getArchitecture().equals(SystemArchitecture.ARM64);
      case LINUX -> true;
    };
  }

  @Override
  protected List<String> getEditionNames() {

    // probed in order, so on Windows the default edition (docker) is preferred over rancher when both are present.
    return List.of(DOCKER_EDITION, RANCHER_EDITION);
  }

  /**
   * Resolves the installed version of the given edition on Linux and macOS (on Windows the base class reads the version from the registry via
   * {@link #getWindowsRegistryAppNames()} instead, so this method is not called there). Both editions resolve the same way on each OS: the {@code docker}
   * edition reads the Docker Desktop version ({@code docker-desktop} package on Linux, {@code Docker.app} bundle on macOS) and the {@code rancher} edition
   * reads Rancher Desktop (the {@code rancher-desktop} package on Linux, the {@code Rancher Desktop.app} bundle on macOS).
   */
  @Override
  protected VersionIdentifier getInstalledVersionForEdition(String edition) {

    if (DOCKER_EDITION.equals(edition)) {
      return switch (this.context.getSystemInfo().getOs()) {
        case LINUX -> getDockerDesktopVersionLinux();
        case MAC -> getDockerDesktopVersionMac();
        default -> null;
      };
    }
    // the rancher edition is installed via the native package manager on Linux or from the app bundle on macOS
    return switch (this.context.getSystemInfo().getOs()) {
      case LINUX -> getNativePackageVersion();
      case MAC -> getRancherDesktopVersionMac();
      default -> null;
    };
  }

  @Override
  public Map<String, String> getWindowsRegistryAppNames() {

    return Map.of(DOCKER_EDITION, "Docker Desktop", RANCHER_EDITION, "Rancher Desktop");
  }

  private VersionIdentifier getDockerDesktopVersionLinux() {

    String dockerDesktopVersionLinuxCommand = "apt list --installed | grep docker-desktop | awk '{print $2}'";
    // Log a warning and return null (instead of throwing) when the command produces no usable output, e.g. when
    // Docker Desktop is not installed via apt.
    String output = this.context.newProcess().runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", dockerDesktopVersionLinuxCommand);
    return (output != null) ? resolveVersionWithPattern(output, DOCKER_DESKTOP_VERSION_PATTERN) : null;
  }

  private VersionIdentifier getDockerDesktopVersionMac() {

    return getMacAppVersion("/Applications/Docker.app");
  }

  private VersionIdentifier getRancherDesktopVersionMac() {

    return getMacAppVersion("/Applications/Rancher Desktop.app");
  }

  /**
   * Reads the installed version of a macOS application bundle (e.g. {@code Docker.app} or {@code Rancher Desktop.app}) from its
   * {@code CFBundleShortVersionString}.
   *
   * @param appBundlePath the absolute path to the {@code .app} bundle, e.g. {@code /Applications/Docker.app}.
   * @return the version read from the bundle or {@code null} if the bundle is not installed or its version cannot be determined.
   */
  private VersionIdentifier getMacAppVersion(String appBundlePath) {

    // Call plutil directly rather than through `bash -lc` so the app bundle path (which contains a space for
    // "Rancher Desktop.app") is passed as a single, correctly quoted argument. A missing bundle makes plutil exit
    // non-zero - that is a legitimate "not installed" case, so swallow the failure and report the edition as not
    // installed so the other editions are still probed.
    String output = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE)
        .runAndGetSingleOutput(IdeLogLevel.WARNING, "plutil", "-extract", "CFBundleShortVersionString", "raw",
            appBundlePath + "/Contents/Info.plist");
    return (output != null) ? resolveVersionWithPattern(output, DOCKER_DESKTOP_VERSION_PATTERN) : null;
  }

  private boolean isDockerDesktopEditionConfigured() {
    return EDITION_DOCKER.equals(getConfiguredEdition());
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }

  @Override
  public String getWindowsRegistryAppName() {

    return "Docker Desktop";
  }
}
