package com.devonfw.tools.ide.tool.docker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.PackageManager;
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

  private boolean isCommandAvailable(String command) {
    return this.context.getPath().hasBinaryOnPath(command);
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

    return List.of(new PackageManagerCommand(PackageManager.ZYPPER, List.of(
            "sudo zypper addrepo https://download.opensuse.org/repositories/isv:/Rancher:/stable/rpm/isv:Rancher:stable.repo",
            String.format("sudo zypper --no-gpg-checks install rancher-desktop=%s*", resolvedVersion))),
        new PackageManagerCommand(PackageManager.APT, List.of(
            "curl -s https://download.opensuse.org/repositories/isv:/Rancher:/stable/deb/Release.key | gpg --dearmor |"
                + " sudo dd status=none of=/usr/share/keyrings/isv-rancher-stable-archive-keyring.gpg",
            "echo 'deb [signed-by=/usr/share/keyrings/isv-rancher-stable-archive-keyring.gpg]"
                + " https://download.opensuse.org/repositories/isv:/Rancher:/stable/deb/ ./' |"
                + " sudo dd status=none of=/etc/apt/sources.list.d/isv-rancher-stable.list", "sudo apt update",
            String.format("sudo apt install -y --allow-downgrades rancher-desktop=%s*", resolvedVersion))));
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
        new PackageManagerCommand(PackageManager.ZYPPER, Arrays.asList("sudo zypper remove rancher-desktop")));
    pmCommands.add(
        new PackageManagerCommand(PackageManager.APT, Arrays.asList("sudo apt -y autoremove rancher-desktop")));

    return pmCommands;
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}
