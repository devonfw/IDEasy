package com.devonfw.tools.ide.tool.docker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.PackageManagerCommand;
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

  private static final Pattern RDCTL_CLIENT_VERSION_PATTERN = Pattern.compile("client version:\\s*v?([\\d.]+)", Pattern.CASE_INSENSITIVE);

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
    return resolveRancherDesktopCommand("docker") != null;
  }

  private boolean isRancherDesktopInstalled() {
    return resolveRancherDesktopCommand("rdctl") != null;
  }

  private String detectContainerRuntime() {
    String docker = resolveRancherDesktopCommand(this.tool);
    if (docker != null) {
      return docker;
    } else if (isCommandAvailable(PODMAN)) {
      return PODMAN;
    } else {
      return this.tool;
    }
  }

  /**
   * Rancher Desktop links its CLI tools (docker, kubectl, rdctl) into the fixed {@code ~/.rd/bin} directory, which it creates on its first launch.
   * Depending on the user's path management strategy this directory may not be on the PATH, so we look it up there explicitly as a fallback.
   *
   * @param command the name of the Rancher Desktop CLI to resolve.
   * @return the {@code command} unchanged if available on PATH, otherwise its absolute path inside {@code ~/.rd/bin} or {@code null} if not found.
   */
  private String resolveRancherDesktopCommand(String command) {
    if (isCommandAvailable(command)) {
      return command;
    }
    Path rancherDesktopBinary = getRancherDesktopBinDir().resolve(command);
    if (Files.exists(rancherDesktopBinary)) {
      return rancherDesktopBinary.toString();
    }
    return null;
  }

  private Path getRancherDesktopBinDir() {
    return this.context.getUserHome().resolve(".rd").resolve("bin");
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
  protected ToolInstallation doInstall(ToolInstallRequest request) {

    ToolInstallation installation = super.doInstall(request);
    if (this.context.getSystemInfo().isLinux() && !Files.isDirectory(getRancherDesktopBinDir())) {
      throw new CliException("Rancher Desktop has been installed but not launched yet. Please start Rancher Desktop once so that it sets up its "
          + "command-line tools (docker, kubectl, ...) in ~/.rd/bin, then re-run your command.", 2);
    }
    return installation;
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
        LOG.error("Couldn't get installed version of " + this.getName());
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

    String rdctl = resolveRancherDesktopCommand("rdctl");
    String output = this.context.newProcess().runAndGetSingleOutput(rdctl, "version");
    return super.resolveVersionWithPattern(output, RDCTL_CLIENT_VERSION_PATTERN);
  }

  @Override
  public String getInstalledEdition() {

    if (!isDockerInstalled()) {
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
        new PackageManagerCommand(NativePackageManager.ZYPPER, List.of("sudo zypper remove rancher-desktop")));
    pmCommands.add(
        new PackageManagerCommand(NativePackageManager.APT, List.of("sudo apt -y autoremove rancher-desktop")));

    return pmCommands;
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}
