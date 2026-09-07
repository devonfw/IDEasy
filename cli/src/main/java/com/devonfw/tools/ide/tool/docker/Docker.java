package com.devonfw.tools.ide.tool.docker;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackage;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://www.docker.com/">docker</a> either as
 * <a href="https://rancherdesktop.io/">Rancher Desktop</a> or as
 * <a href="https://www.docker.com/products/docker-desktop/">Docker Desktop</a>.
 */
public class Docker extends GlobalToolCommandlet {

  private static final String PODMAN = "podman";

  /** The {@link #getWindowsRegistryAppNames() edition name} of Docker Desktop. */
  private static final String DOCKER_EDITION = "docker";

  /** The {@link #getWindowsRegistryAppNames() edition name} of Rancher Desktop. */
  private static final String RANCHER_EDITION = "rancher";

  private static final Pattern DOCKER_DESKTOP_VERSION_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+){1,2})");

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
        )
    );
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

    String dockerDesktopVersionMacCommand = "plutil -extract CFBundleShortVersionString raw /Applications/Docker.app/Contents/Info.plist";
    // Log a warning and return null (instead of throwing) when the command produces no usable output, e.g. when
    // Docker Desktop is not installed at /Applications/Docker.app.
    String output = this.context.newProcess().runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", dockerDesktopVersionMacCommand);
    return (output != null) ? resolveVersionWithPattern(output, DOCKER_DESKTOP_VERSION_PATTERN) : null;
  }

  private VersionIdentifier getRancherDesktopVersionMac() {

    String rancherDesktopVersionMacCommand = "plutil -extract CFBundleShortVersionString raw /Applications/Rancher Desktop.app/Contents/Info.plist";
    // Log a warning and return null (instead of throwing) when the command produces no usable output, e.g. when
    // Rancher Desktop is not installed at /Applications/Rancher Desktop.app.
    String output = this.context.newProcess().runAndGetSingleOutput(IdeLogLevel.WARNING, "bash", "-lc", rancherDesktopVersionMacCommand);
    return (output != null) ? resolveVersionWithPattern(output, DOCKER_DESKTOP_VERSION_PATTERN) : null;
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
