package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.UpgradeMode;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.mvn.MvnBasedLocalToolCommandlet;
import com.devonfw.tools.ide.tool.repository.MavenRepository;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.IdeVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link MvnBasedLocalToolCommandlet} for IDEasy (ide-cli).
 */
public class IdeasyCommandlet extends MvnBasedLocalToolCommandlet {

  /** The {@link MvnArtifact} for IDEasy. */
  public static final MvnArtifact ARTIFACT = MvnArtifact.ofIdeasyCli("*!", "tar.gz", "${os}-${arch}");

  private static final String BASH_CODE_SOURCE_FUNCTIONS = "source \"$IDE_ROOT/_ide/installation/functions\"";

  /** The {@link #getName() tool name}. */
  public static final String TOOL_NAME = "ideasy";

  private final UpgradeMode mode;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public IdeasyCommandlet(IdeContext context) {
    this(context, UpgradeMode.STABLE);
  }

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param mode the {@link UpgradeMode}.
   */
  public IdeasyCommandlet(IdeContext context, UpgradeMode mode) {

    super(context, TOOL_NAME, ARTIFACT, Set.of(Tag.PRODUCTIVITY, Tag.IDE));
    this.mode = mode;
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return IdeVersion.getVersionIdentifier();
  }

  @Override
  public String getConfiguredEdition() {

    return this.tool;
  }

  @Override
  public VersionIdentifier getConfiguredVersion() {

    UpgradeMode upgradeMode = this.mode;
    if (upgradeMode == null) {
      if (IdeVersion.getVersionString().contains("SNAPSHOT")) {
        upgradeMode = UpgradeMode.SNAPSHOT;
      } else {
        if (IdeVersion.getVersionIdentifier().getDevelopmentPhase().isStable()) {
          upgradeMode = UpgradeMode.STABLE;
        } else {
          upgradeMode = UpgradeMode.UNSTABLE;
        }
      }
    }
    return upgradeMode.getVersion();
  }

  @Override
  public Path getToolPath() {

    return this.context.getIdeInstallationPath();
  }

  @Override
  public boolean install(boolean silent) {

    if (IdeVersion.isUndefined()) {
      this.context.warning("You are using IDEasy version {} which indicates local development - skipping upgrade.", IdeVersion.getVersionString());
      return false;
    }
    return super.install(silent);
  }

  /**
   * @return the latest released {@link VersionIdentifier version} of IDEasy.
   */
  public VersionIdentifier getLatestVersion() {

    VersionIdentifier currentVersion = IdeVersion.getVersionIdentifier();
    if (IdeVersion.isUndefined()) {
      return currentVersion;
    }
    VersionIdentifier configuredVersion = getConfiguredVersion();
    return getToolRepository().resolveVersion(this.tool, getConfiguredEdition(), configuredVersion, this);
  }

  /**
   * Checks if an update is available and logs according information.
   *
   * @return {@code true} if an update is available, {@code false} otherwise.
   */
  public boolean checkIfUpdateIsAvailable() {
    VersionIdentifier installedVersion = getInstalledVersion();
    VersionIdentifier latestVersion = getLatestVersion();
    if (installedVersion.equals(latestVersion)) {
      this.context.success("Your version of IDEasy is {} which is the latest released version.", installedVersion);
      return false;
    } else {
      this.context.interaction("Your version of IDEasy is {} but version {} is available. Please run the following command to upgrade to the latest version:\n"
          + "ide upgrade", installedVersion, latestVersion);
      return true;
    }
  }

  /**
   * Initial installation of IDEasy.
   *
   * @param cwd the {@link Path} to the current working directory.
   * @see com.devonfw.tools.ide.commandlet.InstallCommandlet
   */
  public void installIdeasy(Path cwd) {
    Path ideRoot = determineIdeRoot(cwd);
    Path idePath = ideRoot.resolve(IdeContext.FOLDER_UNDERSCORE_IDE);
    Path installationPath = idePath.resolve(IdeContext.FOLDER_INSTALLATION);
    Path ideasySoftwarePath = idePath.resolve(IdeContext.FOLDER_SOFTWARE).resolve(MavenRepository.ID).resolve(IdeasyCommandlet.TOOL_NAME)
        .resolve(IdeasyCommandlet.TOOL_NAME);
    Path ideasyVersionPath = ideasySoftwarePath.resolve(IdeVersion.getVersionString());
    if (Files.isDirectory(ideasyVersionPath)) {
      throw new CliException("IDEasy is already installed at " + ideasyVersionPath + " - if your installation is broken, delete it manually and rerun setup!");
    }
    FileAccess fileAccess = this.context.getFileAccess();
    List<Path> installationArtifacts = new ArrayList<>();
    boolean success = true;
    success &= addInstallationArtifact(cwd, "bin", true, installationArtifacts);
    success &= addInstallationArtifact(cwd, "functions", true, installationArtifacts);
    success &= addInstallationArtifact(cwd, "internal", true, installationArtifacts);
    success &= addInstallationArtifact(cwd, "system", true, installationArtifacts);
    success &= addInstallationArtifact(cwd, "IDEasy.pdf", true, installationArtifacts);
    success &= addInstallationArtifact(cwd, "setup", true, installationArtifacts);
    success &= addInstallationArtifact(cwd, "setup.bat", false, installationArtifacts);
    if (!success) {
      throw new CliException("IDEasy release is inconsistent at " + cwd);
    }
    fileAccess.mkdirs(ideasyVersionPath);
    for (Path installationArtifact : installationArtifacts) {
      fileAccess.copy(installationArtifact, ideasyVersionPath);
    }
    fileAccess.symlink(ideasyVersionPath, installationPath);
    addToShellRc(".bashrc", ideRoot, null);
    addToShellRc(".zshrc", ideRoot, "autoload -U +X bashcompinit && bashcompinit");
    installIdeasyWindowsEnv(ideRoot, installationPath);
    this.context.success("IDEasy has been installed successfully on your system.");
    this.context.warning("IDEasy has been setup for new shells but it cannot work in your current shell(s).\n"
        + "Reboot or open a new terminal to make it work.");
  }

  private void installIdeasyWindowsEnv(Path ideRoot, Path installationPath) {
    if (this.context.getSystemInfo().isWindows()) {
      WindowsHelper helper = WindowsHelper.get(this.context);
      helper.setUserEnvironmentValue(IdeVariables.IDE_ROOT.getName(), ideRoot.toString());
      String userPath = helper.getUserEnvironmentValue(IdeVariables.PATH.getName());
      if (userPath == null) {
        this.context.error("Could not read user PATH from registry!");
      } else {
        this.context.info("Found user PATH={}", userPath);
        Path ideasyBinPath = installationPath.resolve("bin");
        userPath = removeObsoleteEntryFromWindowsPath(userPath);
        if (userPath.isEmpty()) {
          this.context.warning("ATTENTION:\n"
              + "Your user specific PATH variable seems to be empty.\n"
              + "You can double check this by pressing [Windows][r] and launch the program SystemPropertiesAdvanced.\n"
              + "Then click on 'Environment variables' and check if 'PATH' is set in the 'user variables' from the upper list.\n"
              + "In case 'PATH' is defined there non-empty and you get this message, please abort and give us feedback:\n"
              + "https://github.com/devonfw/IDEasy/issues\n"
              + "Otherwise all is correct and you can continue.");
          this.context.askToContinue("Are you sure you want to override your PATH?");
          userPath = ideasyBinPath.toString();
        } else {
          userPath = userPath + ";" + ideasyBinPath;
        }
        helper.setUserEnvironmentValue(IdeVariables.PATH.getName(), userPath);
      }
    }
  }

  static String removeObsoleteEntryFromWindowsPath(String userPath) {
    int len = userPath.length();
    int start = 0;
    while ((start >= 0) && (start < len)) {
      int end = userPath.indexOf(';', start);
      if (end < 0) {
        end = len;
      }
      String entry = userPath.substring(start, end);
      if (entry.endsWith("\\_ide\\bin")) {
        String prefix = "";
        int offset = 1;
        if (start > 0) {
          prefix = userPath.substring(0, start - 1);
          offset = 0;
        }
        if (end == len) {
          return prefix;
        } else {
          return prefix + userPath.substring(end + offset);
        }
      }
      start = end + 1;
    }
    return userPath;
  }

  /**
   * Adds ourselves to the shell RC (run-commands) configuration file.
   *
   * @param filename the name of the RC file.
   * @param ideRoot the IDE_ROOT {@link Path}.
   */
  private void addToShellRc(String filename, Path ideRoot, String extraLine) {

    this.context.info("Configuring IDEasy in {}", filename);
    Path rcFile = this.context.getUserHome().resolve(filename);
    FileAccess fileAccess = this.context.getFileAccess();
    List<String> lines = fileAccess.readFileLines(rcFile);
    if (lines == null) {
      lines = new ArrayList<>();
    } else {
      // since it is unspecified if the returned List may be immutable we want to get sure
      lines = new ArrayList<>(lines);
    }
    Iterator<String> iterator = lines.iterator();
    int removeCount = 0;
    while (iterator.hasNext()) {
      String line = iterator.next();
      line = line.trim();
      if (isObsoleteRcLine(line)) {
        this.context.info("Removing obsolete line from {}: {}", filename, line);
        iterator.remove();
        removeCount++;
      } else if (line.equals(extraLine)) {
        extraLine = null;
      }
    }
    if (extraLine != null) {
      lines.add(extraLine);
    }
    if (this.context.getSystemInfo().isWindows()) {
      lines.add("export IDE_ROOT=\"" + WindowsPathSyntax.MSYS.format(ideRoot) + "\"");
    }
    lines.add(BASH_CODE_SOURCE_FUNCTIONS);
    fileAccess.writeFileLines(lines, rcFile);
  }

  private static boolean isObsoleteRcLine(String line) {
    if (line.startsWith("alias ide=")) {
      return true;
    } else if (line.startsWith("export IDE_ROOT=")) {
      return true;
    } else if (line.equals("ide")) {
      return true;
    } else if (line.equals("ide init")) {
      return true;
    } else if (line.startsWith("source \"$IDE_ROOT/_ide/")) {
      return true;
    }
    return false;
  }

  private boolean addInstallationArtifact(Path cwd, String artifactName, boolean required, List<Path> installationArtifacts) {

    Path artifactPath = cwd.resolve(artifactName);
    if (Files.exists(artifactPath)) {
      installationArtifacts.add(artifactPath);
    } else if (required) {
      this.context.error("Missing required file {}", artifactName);
      return false;
    }
    return true;
  }

  private Path determineIdeRoot(Path cwd) {
    Path ideRoot = this.context.getIdeRoot();
    if (ideRoot == null) {
      Path home = this.context.getUserHome();
      Path installRoot = home;
      if (this.context.getSystemInfo().isWindows()) {
        if (!cwd.startsWith(home)) {
          installRoot = cwd.getRoot();
        }
      }
      ideRoot = installRoot.resolve(IdeContext.FOLDER_PROJECTS);
    } else {
      assert (Files.isDirectory(ideRoot)) : "IDE_ROOT directory does not exist!";
    }
    return ideRoot;
  }

}
