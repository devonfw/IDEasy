package com.devonfw.tools.ide.tool.pgadmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.PackageManagerCommand;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.util.WindowsRegistryUtil;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://www.pgadmin.org/">pgadmin</a>
 */
public class PgAdmin extends GlobalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(PgAdmin.class);

  private static final String PG_ADMIN_APP_NAME = "pgAdmin 4";

  private static final String PG_ADMIN_EXE = "pgAdmin4.exe";

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
    // does not work for wsl
    return List.of(packageManagerCommand);
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    if (this.context.getSystemInfo().isWindows()) {
      Optional<String> version = WindowsRegistryUtil.getDisplayVersion(PG_ADMIN_APP_NAME);
      if (version.isPresent()) {
        return VersionIdentifier.of(version.get());
      }
    } else if (this.context.getSystemInfo().isLinux()) {

    }
    return null;
  }

  @Override
  public void uninstall() {

    super.uninstall();

    if (this.context.getSystemInfo().isLinux()) {
      runWithPackageManager(false, getPackageManagerCommandsUninstall());
    }
  }

  @Override
  protected String getBinaryName() {

    if (this.context.getSystemInfo().isWindows()) {
      return PG_ADMIN_EXE;
    }
    return "pgadmin";
  }

  @Override
  public String getWindowsAppName() {
    return PG_ADMIN_APP_NAME;
  }

  private List<PackageManagerCommand> getPackageManagerCommandsUninstall() {

    List<PackageManagerCommand> pmCommands = new ArrayList<>();

    pmCommands.add(new PackageManagerCommand(NativePackageManager.APT,
        Arrays.asList("sudo apt -y autoremove pgadmin4 pgadmin4-server pgadmin4-desktop pgadmin4-web")));

    return pmCommands;
  }
}
