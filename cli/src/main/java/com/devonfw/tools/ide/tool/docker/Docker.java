package com.devonfw.tools.ide.tool.docker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.os.WindowsHelperImpl;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.PackageManagerCommand;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://www.docker.com/">docker</a> either as
 * <a href="https://rancherdesktop.io/">Rancher Desktop</a> or as
 * <a href="https://www.docker.com/products/docker-desktop/">Docker Desktop</a>.
 */
public class Docker extends GlobalToolCommandlet {

  private static final String PODMAN = "podman";


  private static final Pattern RDCTL_CLIENT_VERSION_PATTERN = Pattern.compile("client version:\\s*v([\\d.]+)", Pattern.CASE_INSENSITIVE);
  
  private static final Pattern DOCKER_DESKTOP_LINUX_VERSION_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+){1,2})");

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
  public boolean isExtract() {

    return switch (this.context.getSystemInfo().getOs()) {
      case WINDOWS -> false;
      case MAC -> this.context.getSystemInfo().getArchitecture().equals(SystemArchitecture.ARM64);
      case LINUX -> true;
    };
  }

  @Override
  protected List<PackageManagerCommand> getInstallPackageManagerCommands() {

    String edition = getConfiguredEdition();
    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    String resolvedVersion = toolRepository.resolveVersion(this.tool, edition, configuredVersion, this).toString();

    return List.of(new PackageManagerCommand(NativePackageManager.ZYPPER, List.of(
            "sudo zypper addrepo https://download.opensuse.org/repositories/isv:/Rancher:/stable/rpm/isv:Rancher:stable.repo",
            String.format("sudo zypper --no-gpg-checks install rancher-desktop=%s*", resolvedVersion))),
        new PackageManagerCommand(NativePackageManager.APT, List.of(
            "curl -s https://download.opensuse.org/repositories/isv:/Rancher:/stable/deb/Release.key | gpg --dearmor |"
                + " sudo dd status=none of=/usr/share/keyrings/isv-rancher-stable-archive-keyring.gpg",
            "echo 'deb [signed-by=/usr/share/keyrings/isv-rancher-stable-archive-keyring.gpg]"
                + " https://download.opensuse.org/repositories/isv:/Rancher:/stable/deb/ ./' |"
                + " sudo dd status=none of=/etc/apt/sources.list.d/isv-rancher-stable.list", "sudo apt update",
            String.format("sudo apt install -y --allow-downgrades rancher-desktop=%s*", resolvedVersion))));
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    if (!isDockerInstalled()) {
      this.context.error("Couldn't get installed version of " + this.getName());
      return null;
    }

    if (isRancherDesktopInstalled()) {
      return getRancherDesktopClientVersion();
    } else {
      VersionIdentifier parsedVersion = switch (this.context.getSystemInfo().getOs()) {
        case WINDOWS -> getDockerDesktopVersionWindows();
        case LINUX -> getDockerDesktopVersionLinux();
        default -> null;
      };

      if (parsedVersion == null) {
        this.context.error("Couldn't get installed version of " + this.getName());
      }

      return parsedVersion;
    }
  }

  private VersionIdentifier getDockerDesktopVersionWindows() {

    String registryPath = "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Docker Desktop";

    WindowsHelper windowsHelper = ((com.devonfw.tools.ide.context.AbstractIdeContext) this.context).getWindowsHelper();
    String version = windowsHelper.getRegistryValue(registryPath, "DisplayVersion");

    return VersionIdentifier.of(version);
  }

  private VersionIdentifier getDockerDesktopVersionLinux() {

    String dockerDesktopVersionLinuxCommand = "apt list --installed | grep docker-desktop | awk '{print $2}'";
    String output = this.context.newProcess().runAndGetSingleOutput("bash", "-lc", dockerDesktopVersionLinuxCommand);
    return super.resolveVersionWithPattern(output, DOCKER_DESKTOP_LINUX_VERSION_PATTERN);
  }

  private VersionIdentifier getRancherDesktopClientVersion() {

    String output = this.context.newProcess().runAndGetSingleOutput("rdctl", "version");
    return super.resolveVersionWithPattern(output, RDCTL_CLIENT_VERSION_PATTERN);
  }

  @Override
  public String getInstalledEdition() {

    if (!isDockerInstalled()) {
      this.context.error("Couldn't get installed edition of " + this.getName());
      return null;
    }

    if (isRancherDesktopInstalled()) {
      return "rancher";
    } else {
      return "desktop";
    }
  }

  @Override
  public void uninstall() {

    if (this.context.getSystemInfo().isLinux()) {
      runWithPackageManager(false, getPackageManagerCommandsUninstall());
    } else {
      super.uninstall();
    }

  }

  private List<PackageManagerCommand> getPackageManagerCommandsUninstall() {

    List<PackageManagerCommand> pmCommands = new ArrayList<>();
    pmCommands.add(
        new PackageManagerCommand(NativePackageManager.ZYPPER, Arrays.asList("sudo zypper remove rancher-desktop")));
    pmCommands.add(
        new PackageManagerCommand(NativePackageManager.APT, Arrays.asList("sudo apt -y autoremove rancher-desktop")));

    return pmCommands;
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}
