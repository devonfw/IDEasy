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

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.PackageManagerCommand;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://www.pgadmin.org/">pgadmin</a>
 */
public class PgAdmin extends GlobalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(PgAdmin.class);

  private static final String PGADMIN_APP = "pgAdmin 4.app";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public PgAdmin(IdeContext context) {

    super(context, "pgadmin", Set.of(Tag.DB, Tag.ADMIN));
  }

  @Override
  protected ToolInstallation doInstall(ToolInstallRequest request) {

    if (this.context.getSystemInfo().isMac()) {
      return doInstallOnMac(request);
    }
    return super.doInstall(request);
  }

  private ToolInstallation doInstallOnMac(ToolInstallRequest request) {

    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    ToolEdition toolEdition = getToolWithConfiguredEdition();
    Path installationPath = getInstallationPath(toolEdition.edition(), resolvedVersion);
    if ((installationPath != null) && !this.context.isForceMode()) {
      return toolAlreadyInstalled(request);
    }

    resolvedVersion = cveCheck(request);
    Path dmg = getToolRepository().download(this.tool, toolEdition.edition(), resolvedVersion, this);
    Path appPath = installMacDmg(dmg);

    installationPath = getInstallationPath(toolEdition.edition(), resolvedVersion);
    if (installationPath == null) {
      throw new CliException("The tool " + this.tool + " was installed but the pgAdmin app could not be found in " + getMacApplicationsPath() + ".");
    }
    IdeLogLevel.SUCCESS.log(LOG, "Successfully installed {} in version {} at {}", this.tool, resolvedVersion, appPath);
    Step step = request.getStep();
    if (step != null) {
      step.success(true);
    }
    return createToolInstallation(installationPath, resolvedVersion, true, request.getProcessContext(), request.isAdditionalInstallation());
  }

  private Path installMacDmg(Path dmg) {

    FileAccess fileAccess = this.context.getFileAccess();
    Path mountPath = fileAccess.createTempDir("pgadmin-dmg");
    boolean mounted = false;
    try {
      this.context.newProcess().executable("hdiutil").addArgs("attach", "-quiet", "-nobrowse", "-mountpoint", mountPath, dmg).run();
      mounted = true;

      Path sourceApp = getMacOsHelper().findAppDir(mountPath);
      if (sourceApp == null) {
        throw new CliException("No pgAdmin .app bundle was found in " + dmg + ".");
      }

      Path targetApp = getMacApplicationsPath().resolve(PGADMIN_APP);
      if (Files.exists(targetApp) && this.context.isForceMode()) {
        runWithPrivilegeFallback("/bin/rm", "-rf", targetApp.toString());
      }
      runWithPrivilegeFallback("/usr/bin/ditto", sourceApp.toString(), targetApp.toString());
      return targetApp;
    } finally {
      if (mounted) {
        this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING).executable("hdiutil")
            .addArgs("detach", "-force", mountPath).run(ProcessMode.DEFAULT_SILENT);
      }
      deleteMountPath(mountPath, fileAccess);
    }
  }

  private void deleteMountPath(Path mountPath, FileAccess fileAccess) {

    try {
      if (Files.isDirectory(mountPath) && (fileAccess.findFirst(mountPath, p -> true, false) != null)) {
        LOG.warn("Skipping deletion of temporary pgAdmin DMG mount path {} because it still contains files.", mountPath);
        return;
      }
      fileAccess.delete(mountPath);
    } catch (RuntimeException e) {
      LOG.warn("Failed to delete temporary pgAdmin DMG mount path {}.", mountPath, e);
    }
  }

  private void runPrivileged(String... command) {

    logPrivilegedCommands(List.of(toSudoCommandLine(command)));
    this.context.newProcess().executable("sudo").addArgs(command).run();
  }

  private void runWithPrivilegeFallback(String... command) {

    ProcessResult result = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(command[0])
        .addArgs(Arrays.copyOfRange(command, 1, command.length)).run(ProcessMode.DEFAULT);
    if (!result.isSuccessful()) {
      LOG.debug("Command {} failed without elevated privileges. Retrying with sudo.", List.of(command));
      runPrivileged(command);
    }
  }

  private static String toSudoCommandLine(String... command) {

    StringBuilder commandLine = new StringBuilder("sudo");
    for (String argument : command) {
      commandLine.append(' ');
      commandLine.append(quoteShellArgument(argument));
    }
    return commandLine.toString();
  }

  private static String quoteShellArgument(String argument) {

    if (argument.indexOf(' ') < 0 && argument.indexOf('\'') < 0) {
      return argument;
    }
    return "'" + argument.replace("'", "'\"'\"'") + "'";
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

    if (this.context.getSystemInfo().isMac()) {
      return "pgAdmin 4";
    }
    return "pgadmin4";
  }

  @Override
  protected Path getInstallationPath(String edition, VersionIdentifier resolvedVersion) {

    Path installationPath = super.getInstallationPath(edition, resolvedVersion);
    if (installationPath != null) {
      return installationPath;
    }
    if (this.context.getSystemInfo().isMac()) {
      return getMacOsAppPath();
    }
    if (this.context.getSystemInfo().isWindows()) {
      return getExecutableFolderFromWindowsRegistry();
    }
    return null;
  }

  protected Path getMacApplicationsPath() {

    return Path.of("/Applications");
  }

  private Path getMacOsAppPath() {

    Path appPath = getMacApplicationsPath().resolve(PGADMIN_APP);
    Path binary = appPath.resolve("Contents").resolve("MacOS").resolve(getBinaryName());
    if (Files.isExecutable(binary)) {
      this.context.getPath().setPath(getName(), binary.getParent());
      return appPath;
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
