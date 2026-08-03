package com.devonfw.tools.ide.tool.pgadmin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.PackageManagerCommand;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://www.pgadmin.org/">pgadmin</a>
 */
public class PgAdmin extends GlobalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public PgAdmin(IdeContext context) {

    super(context, "pgadmin", Set.of(Tag.DB, Tag.ADMIN));
  }

  @Override
  protected List<PackageManagerCommand> getInstallPackageManagerCommands() {

    String edition = getConfiguredEdition();
    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    String resolvedVersion = toolRepository.resolveVersion(this.tool, edition, configuredVersion, this).toString();

    PackageManagerCommand packageManagerCommand = new PackageManagerCommand(NativePackageManager.APT, List.of(
        "curl -fsS https://www.pgadmin.org/static/packages_pgadmin_org.pub | "
            + "sudo gpg --yes --dearmor -o /usr/share/keyrings/packages-pgadmin-org.gpg",
        "sudo sh -c 'echo \"deb [signed-by=/usr/share/keyrings/packages-pgadmin-org.gpg] "
            + "https://ftp.postgresql.org/pub/pgadmin/pgadmin4/apt/$(lsb_release -cs) pgadmin4 main\" "
            + "> /etc/apt/sources.list.d/pgadmin4.list && apt update'", String.format(
            "sudo apt install -y --allow-downgrades pgadmin4=%1$s pgadmin4-server=%1$s pgadmin4-desktop=%1$s pgadmin4-web=%1$s",
            resolvedVersion)));
    return List.of(packageManagerCommand);
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

    pmCommands.add(new PackageManagerCommand(NativePackageManager.APT,
        Arrays.asList("sudo apt -y autoremove pgadmin4 pgadmin4-server pgadmin4-desktop pgadmin4-web")));

    return pmCommands;
  }

  @Override
  protected String getBinaryName() {

    return "pgadmin4";
  }

  @Override
  protected Path getInstallationPath(String edition, VersionIdentifier resolvedVersion) {
    if (super.getInstallationPath(edition, resolvedVersion) == null) {
      if (this.context.getSystemInfo().isWindows()) {
        return getExecutableFolderFromWindowsRegistry();
      }
    }
    return null;
  }

  private Path getExecutableFolderFromWindowsRegistry() {

    WindowsHelper windowsHelper = WindowsHelper.get(this.context);
    String registryPath = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\pgAdmin 4v9_is1";
    String displayIcon = windowsHelper.getRegistryValue(registryPath, "DisplayIcon");
    if (displayIcon != null) {
      Path executablePath = Paths.get(displayIcon);
      if (Files.isExecutable(executablePath)) {
        Path installationDir = executablePath.getParent();
        this.context.getPath().setPath(getName(), installationDir);
        return installationDir;
      }
    }
    return null;
  }
}
