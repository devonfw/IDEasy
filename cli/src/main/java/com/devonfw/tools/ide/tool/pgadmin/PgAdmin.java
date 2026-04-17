package com.devonfw.tools.ide.tool.pgadmin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.PackageManagerCommand;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link GlobalToolCommandlet} for <a href="https://www.pgadmin.org/">pgadmin</a>
 */
public class PgAdmin extends GlobalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(PgAdmin.class);

  private static final String PG_ADMIN = "pgAdmin 4";

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
  public VersionIdentifier getInstalledVersion() {

    if (this.context.getSystemInfo().isWindows()) {
      Path installationPath = getWindowsInstallationPath();
      Path sbomPath;
      if (installationPath != null) {
        sbomPath = installationPath.resolve("sbom.json");
        if (Files.isRegularFile(sbomPath)) {
          try {
            JsonNode root = new ObjectMapper().readTree(sbomPath.toFile());
            for (JsonNode component : root.path("components")) {
              if (PG_ADMIN.equals(component.path("name").asText())) {
                return VersionIdentifier.of(
                    component.path("version")
                        .asText()
                        .replace("\"", "") // weirdly a quotation mark  present in the version string
                );
              }
            }
          } catch (Exception e) {
            LOG.error("Could not read pgAdmin version from SBOM file '{}'", sbomPath, e);
          }
        }
      }
    }

    return null;
  }

  @Override
  public String getInstalledEdition() {

    if (this.context.getSystemInfo().isWindows()) {
      Path installationPath = getWindowsInstallationPath();
      if (installationPath != null) {
        if (Files.exists(installationPath)) {
          return "pgadmin";
        }
      }
    }

    return null;
  }

  @Override
  public void uninstall() {

    if (this.context.getSystemInfo().isLinux()) {
      runWithPackageManager(false, getPackageManagerCommandsUninstall());
    } else if (this.context.getSystemInfo().isWindows()) {
      Path installationPath = getWindowsInstallationPath();
      if (installationPath != null) {
        Path uninstallExecutablePath = installationPath.resolve("unins000.exe");
        if (Files.isExecutable(uninstallExecutablePath)) {
          this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING)
              .executable(uninstallExecutablePath)
              .run(ProcessMode.BACKGROUND).getExitCode();
        }
      }
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

  private Path getWindowsInstallationPath() {

    String appDataPath = System.getenv("APPDATA");
    if (appDataPath != null && !appDataPath.isBlank()) {
      try {
        Path shortcut = Paths.get(appDataPath)
            .resolve("Microsoft")
            .resolve("Windows")
            .resolve("Start Menu")
            .resolve("Programs")
            .resolve(PG_ADMIN)
            .resolve(PG_ADMIN + ".lnk");

        Process process = new ProcessBuilder(
            "powershell",
            "-NoProfile",
            "-Command",
            "(New-Object -ComObject WScript.Shell)"
                + ".CreateShortcut('" + shortcut + "')"
                + ".TargetPath"
        ).redirectErrorStream(true).start();

        Path installationPath = Paths.get(new String(process.getInputStream().readAllBytes()).trim());
        return installationPath.getParent().getParent();
      } catch (Exception e) {
        LOG.error("Could not resolve installation path of {}", PG_ADMIN, e);
      }
    }

    return null;
  }
}
