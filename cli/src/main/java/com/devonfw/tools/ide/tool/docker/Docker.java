package com.devonfw.tools.ide.tool.docker;

import java.nio.file.Files;
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
import com.devonfw.tools.ide.tool.EditionAndVersion;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackage;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://www.docker.com/">docker</a> either as
 * <a href="https://rancherdesktop.io/">Rancher Desktop</a> or as
 * <a href="https://www.docker.com/products/docker-desktop/">Docker Desktop</a>.
 */
public class Docker extends GlobalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Docker.class);

  private static final String PODMAN = "podman";

  /** The hidden folder Rancher Desktop creates in the user's home directory on its first launch, containing its CLI tools. */
  private static final String FOLDER_RANCHER_DESKTOP = ".rd";

  private static final Pattern RDCTL_CLIENT_VERSION_PATTERN = Pattern.compile("client version:\\s*v?([\\d.]+)", Pattern.CASE_INSENSITIVE);

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
    return resolveDockerCommand("docker") != null;
  }

  private boolean isRancherDesktopInstalled() {
    return resolveDockerCommand("rdctl") != null;
  }

  private String detectContainerRuntime() {
    String docker = resolveDockerCommand(this.tool);
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
   * @param command the name of a CLI shipped with the docker installation (e.g. docker, rdctl, kubectl).
   * @return the {@code command} unchanged if available on PATH, otherwise its absolute path inside {@code ~/.rd/bin} or {@code null} if not found.
   */
  public String resolveDockerCommand(String command) {
    if (isCommandAvailable(command)) {
      return command;
    }
    Path candidate = getRancherDesktopBinDir().resolve(command);
    Path rancherDesktopBinary = this.context.getPath().findBinary(candidate);
    if (rancherDesktopBinary != candidate) {
      return rancherDesktopBinary.toString();
    }
    return null;
  }

  private Path getRancherDesktopBinDir() {
    return this.context.getUserHome().resolve(FOLDER_RANCHER_DESKTOP).resolve(IdeContext.FOLDER_BIN);
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
        ),
        new NativePackage(NativePackageManager.YAY, List.of("rancher-desktop")),
        new NativePackage(NativePackageManager.BREW_CASK, List.of("docker"))
    );
  }

  @Override
  public String getMacApplicationName() {

    return "Docker";
  }

  @Override
  protected ToolEditionAndVersion adjustRequestedEdition(ToolEditionAndVersion requested) {

    if (this.context.getSystemInfo().isLinux()) {
      ToolEdition edition = requested.getEdition();
      if (!"rancher".equals(edition.edition())) {
        LOG.warn("Docker Desktop is not yet supported by IDEasy on Linux, installing Rancher Desktop instead.");
        requested.replaceEdition(new ToolEdition(this.tool, "rancher"));
      }
    }
    return requested;
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
    if (this.context.getSystemInfo().isLinux() && isRancherDesktopInstalled() && !Files.isDirectory(getRancherDesktopBinDir())) {
      LOG.warn("Rancher Desktop has been installed but not launched yet. Please start Rancher Desktop once so that it sets up its "
          + "command-line tools (docker, kubectl, ...) in {}, then re-run your command.", getRancherDesktopBinDir());
    }
    return installation;
  }

  @Override
  protected EditionAndVersion computeInstalledEditionAndVersion() {

    if (!isDockerInstalled()) {
      return null;
    }

<<<<<<< HEAD
    if (isRancherDesktopInstalled()) {
      VersionIdentifier version = getRancherDesktopClientVersion();
      if (version == null) {
        version = getNativePackageVersion();
      }
=======
    String rdctl = resolveDockerCommand("rdctl");
    if (rdctl != null) {
      VersionIdentifier version = getRancherDesktopClientVersion(rdctl);
>>>>>>> 1f41996c (#854: Pass resolved rdctl into getRancherDesktopClientVersion)
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

  private VersionIdentifier getRancherDesktopClientVersion(String rdctl) {

    // rdctl may be on the PATH as a dangling symlink (e.g. Rancher Desktop was removed but ~/.rd/bin remained) so executing it can fail to start the process
    String rdctl = resolveRancherDesktopCommand("rdctl");
    try {
      String output = this.context.newProcess().runAndGetSingleOutput(rdctl, "version");
      return resolveVersionWithPattern(output, RDCTL_CLIENT_VERSION_PATTERN);
    } catch (IllegalStateException e) {
      LOG.warn("Could not determine the installed Rancher Desktop version - rdctl could not be executed: {}", e.getMessage());
      return null;
    }
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
