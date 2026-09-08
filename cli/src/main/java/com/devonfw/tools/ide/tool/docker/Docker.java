package com.devonfw.tools.ide.tool.docker;

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
import com.devonfw.tools.ide.tool.EditionAndVersion;
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

  private static final Logger LOG = LoggerFactory.getLogger(Docker.class);

  private static final String PODMAN = "podman";

  private static final Pattern RDCTL_CLIENT_VERSION_PATTERN = Pattern.compile("client version:\\s*v([\\d.]+)", Pattern.CASE_INSENSITIVE);

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

  private boolean isDockerInstalled() {
    return isCommandAvailable("docker");
  }

  private boolean isRancherDesktopInstalled() {
    return isCommandAvailable("rdctl");
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
            List.of(),
            List.of("sudo zypper addrepo https://download.opensuse.org/repositories/isv:/Rancher:/stable/rpm/isv:Rancher:stable.repo"),
            null,
            true
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
            ),
            false
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
  protected EditionAndVersion computeInstalledEditionAndVersion() {

    if (!isDockerInstalled()) {
      return null;
    }

    if (isRancherDesktopInstalled()) {
      VersionIdentifier version = getRancherDesktopClientVersion();
      return new EditionAndVersion("rancher", version);
    }

    // Docker Desktop: the edition is always "docker" (matching getWindowsRegistryAppNames()); on Windows it is
    // resolved from the registry by super. Only the version source differs per OS: Windows reads the registry app
    // version, Linux the docker-desktop package version, macOS the Docker.app bundle version.
    VersionIdentifier version = switch (this.context.getSystemInfo().getOs()) {
      case WINDOWS -> {
        EditionAndVersion fromRegistry = super.computeInstalledEditionAndVersion();
        yield (fromRegistry != null) ? fromRegistry.version() : null;
      }
      case LINUX -> getDockerDesktopVersionLinux();
      case MAC -> getDockerDesktopVersionMac();
      default -> null;
    };

    if (version == null) {
      LOG.error("Couldn't get installed version of " + this.getName());
    }

    return new EditionAndVersion("docker", version);
  }

  @Override
  public Map<String, String> getWindowsRegistryAppNames() {

    return Map.of("docker", "Docker Desktop", "rancher", "Rancher Desktop");
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

  private VersionIdentifier getRancherDesktopClientVersion() {

    String output = this.context.newProcess().runAndGetSingleOutput("rdctl", "version");
    return resolveVersionWithPattern(output, RDCTL_CLIENT_VERSION_PATTERN);
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
