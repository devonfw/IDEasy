package com.devonfw.tools.ide.context;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.IdeSystem;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.io.IdeProgressBarNone;
import com.devonfw.tools.ide.merge.DirectoryMerger;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.repository.CustomToolRepository;
import com.devonfw.tools.ide.tool.repository.MavenRepository;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Interface for interaction with the user allowing to input and output information.
 */
public interface IdeContext extends IdeStartContext {

  /**
   * The default settings URL.
   *
   * @see com.devonfw.tools.ide.commandlet.AbstractUpdateCommandlet
   */
  String DEFAULT_SETTINGS_REPO_URL = "https://github.com/devonfw/ide-settings.git";

  /** The name of the workspaces folder. */
  String FOLDER_WORKSPACES = "workspaces";

  /** The name of the {@link #getSettingsPath() settings} folder. */
  String FOLDER_SETTINGS = "settings";

  /** The name of the {@link #getSoftwarePath() software} folder. */
  String FOLDER_SOFTWARE = "software";

  /** The name of the {@link #getUrlsPath() urls} folder. */
  String FOLDER_URLS = "urls";

  /** The name of the conf folder for project specific user configurations. */
  String FOLDER_CONF = "conf";

  /**
   * The name of the folder inside IDE_ROOT reserved for IDEasy. Intentionally starting with an underscore and not a dot to prevent effects like OS hiding,
   * maven filtering, .gitignore and to distinguish from {@link #FOLDER_DOT_IDE}.
   *
   * @see #getIdePath()
   */
  String FOLDER_UNDERSCORE_IDE = "_ide";

  /**
   * The name of the folder inside {@link #FOLDER_UNDERSCORE_IDE} with the current IDEasy installation.
   *
   * @see #getIdeInstallationPath()
   */
  String FOLDER_INSTALLATION = "installation";

  /**
   * The name of the hidden folder for IDE configuration in the users home directory or status information in the IDE_HOME directory.
   *
   * @see #getUserHomeIde()
   */
  String FOLDER_DOT_IDE = ".ide";

  /** The name of the updates folder for temporary data and backup. */
  String FOLDER_UPDATES = "updates";

  /** The name of the volume folder for mounting archives like *.dmg. */
  String FOLDER_VOLUME = "volume";

  /** The name of the backups folder for backup. */
  String FOLDER_BACKUPS = "backups";

  /** The name of the downloads folder. */
  String FOLDER_DOWNLOADS = "Downloads";

  /** The name of the bin folder where executable files are found by default. */
  String FOLDER_BIN = "bin";

  /** The name of the repositories folder where properties files are stores for each repository */
  String FOLDER_REPOSITORIES = "repositories";

  /** The name of the repositories folder where properties files are stores for each repository */
  String FOLDER_LEGACY_REPOSITORIES = "projects";

  /** The name of the Contents folder inside a MacOS app. */
  String FOLDER_CONTENTS = "Contents";

  /** The name of the Resources folder inside a MacOS app. */
  String FOLDER_RESOURCES = "Resources";

  /** The name of the app folder inside a MacOS app. */
  String FOLDER_APP = "app";

  /** The name of the extra folder inside the software folder */
  String FOLDER_EXTRA = "extra";

  /**
   * The name of the {@link #getPluginsPath() plugins folder} and also the plugins folder inside the IDE folders of {@link #getSettingsPath() settings} (e.g.
   * settings/eclipse/plugins).
   */
  String FOLDER_PLUGINS = "plugins";

  /**
   * The name of the workspace folder inside the IDE specific {@link #FOLDER_SETTINGS settings} containing the configuration templates in #FOLDER_SETUP
   * #FOLDER_UPDATE.
   */
  String FOLDER_WORKSPACE = "workspace";

  /**
   * The name of the setup folder inside the {@link #FOLDER_WORKSPACE workspace} folder containing the templates for the configuration templates for the initial
   * setup of a workspace. This is closely related with the {@link #FOLDER_UPDATE update} folder.
   */
  String FOLDER_SETUP = "setup";

  /**
   * The name of the update folder inside the {@link #FOLDER_WORKSPACE workspace} folder containing the templates for the configuration templates for the update
   * of a workspace. Configurations in this folder will be applied every time the IDE is started. They will override the settings the user may have manually
   * configured every time. This is only for settings that have to be the same for every developer in the project. An example would be the number of spaces used
   * for indentation and other code-formatting settings. If all developers in a project team use the same formatter settings, this will actively prevent
   * diff-wars. However, the entire team needs to agree on these settings.<br> Never configure aspects inside this update folder that may be of personal flavor
   * such as the color theme. Otherwise developers will hate you as you actively take away their freedom to customize the IDE to their personal needs and
   * wishes. Therefore do all "biased" or "flavored" configurations in {@link #FOLDER_SETUP setup} so these are only pre-configured but can be changed by the
   * user as needed.
   */
  String FOLDER_UPDATE = "update";

  /**
   * The name of the folder inside {@link #FOLDER_UNDERSCORE_IDE _ide} folder containing internal resources and scripts of IDEasy.
   */
  String FOLDER_INTERNAL = "internal";

  /** The file where the installed software version is written to as plain text. */
  String FILE_SOFTWARE_VERSION = ".ide.software.version";

  /** The file where the installed software version is written to as plain text. */
  String FILE_LEGACY_SOFTWARE_VERSION = ".devon.software.version";

  /** The file for the license agreement. */
  String FILE_LICENSE_AGREEMENT = ".license.agreement";

  /** The file extension for a {@link java.util.Properties} file. */
  String EXT_PROPERTIES = ".properties";

  /** The default for {@link #getWorkspaceName()}. */
  String WORKSPACE_MAIN = "main";

  /** The folder with the configuration template files from the settings. */
  String FOLDER_TEMPLATES = "templates";

  /** Legacy folder name used as compatibility fallback if {@link #FOLDER_TEMPLATES} does not exist. */
  String FOLDER_LEGACY_TEMPLATES = "devon";

  /** The default folder name for {@link #getIdeRoot() IDE_ROOT}. */
  String FOLDER_PROJECTS = "projects";

  /** The filename of the configuration file in the settings for this {@link CustomToolRepository}. */
  String FILE_CUSTOM_TOOLS = "ide-custom-tools.json";

  /**
   * file containing the current local commit hash of the settings repository.
   */
  String SETTINGS_COMMIT_ID = ".commit.id";

  /** The IDEasy ASCII logo. */
  String LOGO = """
      __       ___ ___  ___
      ╲ ╲     |_ _|   ╲| __|__ _ ____ _
       > >     | || |) | _|/ _` (_-< || |
      /_/ ___ |___|___/|___╲__,_/__/╲_, |
         |___|                       |__/
      """.replace('╲', '\\');

  /**
   * @return {@code true} if {@link #isOfflineMode() offline mode} is active or we are NOT {@link #isOnline() online}, {@code false} otherwise.
   */
  default boolean isOffline() {

    return isOfflineMode() || !isOnline();
  }

  /**
   * @return {@code true} if we are currently online (Internet access is available), {@code false} otherwise.
   */
  boolean isOnline();

  /**
   * Print the IDEasy {@link #LOGO logo}.
   */
  default void printLogo() {

    info(LOGO);
  }

  /**
   * Asks the user for a single string input.
   *
   * @param message The information message to display.
   * @param defaultValue The default value to return when no input is provided.
   * @return The string input from the user, or the default value if no input is provided.
   */
  String askForInput(String message, String defaultValue);

  /**
   * Asks the user for a single string input.
   *
   * @param message The information message to display.
   * @return The string input from the user, or the default value if no input is provided.
   */
  String askForInput(String message);

  /**
   * @param question the question to ask.
   * @return {@code true} if the user answered with "yes", {@code false} otherwise ("no").
   */
  default boolean question(String question) {

    String yes = "yes";
    String option = question(question, yes, "no");
    if (yes.equals(option)) {
      return true;
    }
    return false;
  }

  /**
   * @param <O> type of the option. E.g. {@link String}.
   * @param question the question to ask.
   * @param options the available options for the user to answer. There should be at least two options given as otherwise the question cannot make sense.
   * @return the option selected by the user as answer.
   */
  @SuppressWarnings("unchecked")
  <O> O question(String question, O... options);

  /**
   * Will ask the given question. If the user answers with "yes" the method will return and the process can continue. Otherwise if the user answers with "no" an
   * exception is thrown to abort further processing.
   *
   * @param question the yes/no question to {@link #question(String) ask}.
   * @throws CliAbortException if the user answered with "no" and further processing shall be aborted.
   */
  default void askToContinue(String question) {

    boolean yesContinue = question(question);
    if (!yesContinue) {
      throw new CliAbortException();
    }
  }

  /**
   * @param purpose the purpose why Internet connection is required.
   * @throws CliException if you are {@link #isOffline() offline}.
   */
  default void requireOnline(String purpose) {

    if (isOfflineMode()) {
      throw CliOfflineException.ofPurpose(purpose);
    }
  }

  /**
   * @return the {@link SystemInfo}.
   */
  SystemInfo getSystemInfo();

  /**
   * @return the {@link EnvironmentVariables} with full inheritance.
   */
  EnvironmentVariables getVariables();

  /**
   * @return the {@link FileAccess}.
   */
  FileAccess getFileAccess();

  /**
   * @return the {@link CommandletManager}.
   */
  CommandletManager getCommandletManager();

  /**
   * @return the default {@link ToolRepository}.
   */
  ToolRepository getDefaultToolRepository();

  /**
   * @return the {@link CustomToolRepository}.
   */
  CustomToolRepository getCustomToolRepository();

  /**
   * @return the {@link MavenRepository}.
   */
  MavenRepository getMavenToolRepository();

  /**
   * @return the {@link Path} to the IDE instance directory. You can have as many IDE instances on the same computer as independent tenants for different
   *     isolated projects.
   * @see com.devonfw.tools.ide.variable.IdeVariables#IDE_HOME
   */
  Path getIdeHome();

  /**
   * @return the name of the current project.
   * @see com.devonfw.tools.ide.variable.IdeVariables#PROJECT_NAME
   */
  String getProjectName();

  /**
   * @return the IDEasy version the {@link #getIdeHome() current project} was created with or migrated to.
   */
  VersionIdentifier getProjectVersion();

  /**
   * @param version the new value of {@link #getProjectVersion()}.
   */
  void setProjectVersion(VersionIdentifier version);

  /**
   * @return the {@link Path} to the IDE installation root directory. This is the top-level folder where the {@link #getIdeHome() IDE instances} are located as
   *     sub-folder. There is a reserved ".ide" folder where central IDE data is stored such as the {@link #getUrlsPath() download metadata} and the central
   *     software repository.
   * @see com.devonfw.tools.ide.variable.IdeVariables#IDE_ROOT
   */
  Path getIdeRoot();

  /**
   * @return the {@link Path} to the {@link #FOLDER_UNDERSCORE_IDE}.
   * @see #getIdeRoot()
   * @see #FOLDER_UNDERSCORE_IDE
   */
  Path getIdePath();

  /**
   * @return the {@link Path} to the {@link #FOLDER_INSTALLATION installation} folder of IDEasy. This is a link to the (latest) installed release of IDEasy. On
   *     upgrade a new release is installed and the link is switched to the new release.
   */
  default Path getIdeInstallationPath() {

    return getIdePath().resolve(FOLDER_INSTALLATION);
  }

  /**
   * @return the current working directory ("user.dir"). This is the directory where the user's shell was located when the IDE CLI was invoked.
   */
  Path getCwd();

  /**
   * @return the {@link Path} for the temporary directory to use. Will be different from the OS specific temporary directory (java.io.tmpDir).
   */
  Path getTempPath();

  /**
   * @return the {@link Path} for the temporary download directory to use.
   */
  Path getTempDownloadPath();

  /**
   * @return the {@link Path} to the download metadata (ide-urls). Here a git repository is cloned and updated (pulled) to always have the latest metadata to
   *     download tools.
   * @see com.devonfw.tools.ide.url.model.folder.UrlRepository
   */
  Path getUrlsPath();

  /**
   * @return the {@link UrlMetadata}. Will be lazily instantiated and thereby automatically be cloned or pulled (by default).
   */
  UrlMetadata getUrls();

  /**
   * @return the {@link Path} to the download cache. All downloads will be placed here using a unique naming pattern that allows to reuse these artifacts. So if
   *     the same artifact is requested again it will be taken from the cache to avoid downloading it again.
   */
  Path getDownloadPath();

  /**
   * @return the {@link Path} to the software folder inside {@link #getIdeHome() IDE_HOME}. All tools for that IDE instance will be linked here from the
   *     {@link #getSoftwareRepositoryPath() software repository} as sub-folder named after the according tool.
   */
  Path getSoftwarePath();

  /**
   * @return the {@link Path} to the extra folder inside software folder inside {@link #getIdeHome() IDE_HOME}. All tools for that IDE instance will be linked
   *     here from the {@link #getSoftwareRepositoryPath() software repository} as sub-folder named after the according tool.
   */
  Path getSoftwareExtraPath();

  /**
   * @return the {@link Path} to the global software repository. This is the central directory where the tools are extracted physically on the local disc. Those
   *     are shared among all IDE instances (see {@link #getIdeHome() IDE_HOME}) via symbolic links (see {@link #getSoftwarePath()}). Therefore this repository
   *     follows the sub-folder structure {@code «repository»/«tool»/«edition»/«version»/}. So multiple versions of the same tool exist here as different
   *     folders. Further, such software may not be modified so e.g. installation of plugins and other kind of changes to such tool need to happen strictly out
   *     of the scope of this folders.
   */
  Path getSoftwareRepositoryPath();

  /**
   * @return the {@link Path} to the {@link #FOLDER_PLUGINS plugins folder} inside {@link #getIdeHome() IDE_HOME}. All plugins of the IDE instance will be
   *     stored here. For each tool that supports plugins a sub-folder with the tool name will be created where the plugins for that tool get installed.
   */
  Path getPluginsPath();

  /**
   * @return the {@link Path} to the central tool repository. All tools will be installed in this location using the directory naming schema of
   *     {@code «repository»/«tool»/«edition»/«version»/}. Actual {@link #getIdeHome() IDE instances} will only contain symbolic links to the physical tool
   *     installations in this repository. This allows to share and reuse tool installations across multiple {@link #getIdeHome() IDE instances}. The variable
   *     {@code «repository»} is typically {@code default} for the tools from our standard {@link #getUrlsPath() ide-urls download metadata} but this will
   *     differ for custom tools from a private repository.
   */
  Path getToolRepositoryPath();

  /**
   * @return the {@link Path} to the users home directory. Typically initialized via the {@link System#getProperty(String) system property} "user.home".
   * @see com.devonfw.tools.ide.variable.IdeVariables#HOME
   */
  Path getUserHome();

  /**
   * @return the {@link Path} to the ".ide" subfolder in the {@link #getUserHome() users home directory}.
   */
  Path getUserHomeIde();

  /**
   * @return the {@link Path} to the {@link #FOLDER_SETTINGS settings} folder with the cloned git repository containing the project configuration.
   */
  Path getSettingsPath();

  /**
   * @return the {@link Path} to the {@link #FOLDER_REPOSITORIES repositories} folder with legacy fallback if not present or {@code null} if not found.
   */
  default Path getRepositoriesPath() {

    Path settingsPath = getSettingsPath();
    if (settingsPath == null) {
      return null;
    }
    Path repositoriesPath = settingsPath.resolve(IdeContext.FOLDER_REPOSITORIES);
    if (Files.isDirectory(repositoriesPath)) {
      return repositoriesPath;
    }
    Path legacyRepositoriesPath = settingsPath.resolve(IdeContext.FOLDER_LEGACY_REPOSITORIES);
    if (Files.isDirectory(legacyRepositoriesPath)) {
      return legacyRepositoriesPath;
    }
    return null;
  }

  /**
   * @return the {@link Path} to the {@code settings} folder with the cloned git repository containing the project configuration only if the settings repository
   *     is in fact a git repository.
   */
  Path getSettingsGitRepository();

  /**
   * @return {@code true} if the settings repository is a symlink or a junction.
   */
  boolean isSettingsRepositorySymlinkOrJunction();

  /**
   * @return the {@link Path} to the file containing the last tracked commit Id of the settings repository.
   */
  Path getSettingsCommitIdPath();

  /**
   * @return the {@link Path} to the templates folder inside the {@link #getSettingsPath() settings}. The relative directory structure in this templates folder
   *     is to be applied to {@link #getIdeHome() IDE_HOME} when the project is set up.
   */
  default Path getSettingsTemplatePath() {
    Path settingsFolder = getSettingsPath();
    Path templatesFolder = settingsFolder.resolve(IdeContext.FOLDER_TEMPLATES);
    if (!Files.isDirectory(templatesFolder)) {
      Path templatesFolderLegacy = settingsFolder.resolve(IdeContext.FOLDER_LEGACY_TEMPLATES);
      if (Files.isDirectory(templatesFolderLegacy)) {
        templatesFolder = templatesFolderLegacy;
      } else {
        warning("No templates found in settings git repo neither in {} nor in {} - configuration broken", templatesFolder, templatesFolderLegacy);
        return null;
      }
    }
    return templatesFolder;
  }

  /**
   * @return the {@link Path} to the {@code conf} folder with instance specific tool configurations and the
   *     {@link EnvironmentVariablesType#CONF user specific project configuration}.
   */
  Path getConfPath();

  /**
   * @return the {@link Path} to the workspace.
   * @see #getWorkspaceName()
   */
  Path getWorkspacePath();

  /**
   * @return the name of the workspace. Defaults to {@link #WORKSPACE_MAIN}.
   */
  String getWorkspaceName();

  /**
   * @return the value of the system {@link IdeVariables#PATH PATH} variable. It is automatically extended according to the tools available in
   *     {@link #getSoftwarePath() software path} unless {@link #getIdeHome() IDE_HOME} was not found.
   */
  SystemPath getPath();

  /**
   * @return a new {@link ProcessContext} to {@link ProcessContext#run() run} external commands.
   */
  ProcessContext newProcess();

  /**
   * @param title the {@link IdeProgressBar#getTitle() title}.
   * @param size the {@link IdeProgressBar#getMaxSize() expected maximum size}.
   * @param unitName the {@link IdeProgressBar#getUnitName() unit name}.
   * @param unitSize the {@link IdeProgressBar#getUnitSize() unit size}.
   * @return the new {@link IdeProgressBar} to use.
   */
  IdeProgressBar newProgressBar(String title, long size, String unitName, long unitSize);

  /**
   * @param title the {@link IdeProgressBar#getTitle() title}.
   * @param size the {@link IdeProgressBar#getMaxSize() expected maximum size} in bytes.
   * @return the new {@link IdeProgressBar} to use.
   */
  default IdeProgressBar newProgressBarInMib(String title, long size) {

    if ((size > 0) && (size < 1024)) {
      return new IdeProgressBarNone(title, size, IdeProgressBar.UNIT_NAME_MB, IdeProgressBar.UNIT_SIZE_MB);
    }
    return newProgressBar(title, size, IdeProgressBar.UNIT_NAME_MB, IdeProgressBar.UNIT_SIZE_MB);
  }

  /**
   * @param size the {@link IdeProgressBar#getMaxSize() expected maximum size} in bytes.
   * @return the new {@link IdeProgressBar} for copy.
   */
  default IdeProgressBar newProgressBarForDownload(long size) {

    return newProgressBarInMib(IdeProgressBar.TITLE_DOWNLOADING, size);
  }

  /**
   * @param size the {@link IdeProgressBar#getMaxSize() expected maximum size} in bytes.
   * @return the new {@link IdeProgressBar} for extracting.
   */
  default IdeProgressBar newProgressbarForExtracting(long size) {

    return newProgressBarInMib(IdeProgressBar.TITLE_EXTRACTING, size);
  }

  /**
   * @param size the {@link IdeProgressBar#getMaxSize() expected maximum size} in bytes.
   * @return the new {@link IdeProgressBar} for copy.
   */
  default IdeProgressBar newProgressbarForCopying(long size) {

    return newProgressBarInMib(IdeProgressBar.TITLE_COPYING, size);
  }

  /**
   * @return the {@link DirectoryMerger} used to configure and merge the workspace for an {@link com.devonfw.tools.ide.tool.ide.IdeToolCommandlet IDE}.
   */
  DirectoryMerger getWorkspaceMerger();

  /**
   * @return the {@link Path} to the working directory from where the command is executed.
   */
  Path getDefaultExecutionDirectory();

  /**
   * @return the {@link IdeSystem} instance wrapping {@link System}.
   */
  IdeSystem getSystem();

  /**
   * @return the {@link GitContext} used to run several git commands.
   */
  GitContext getGitContext();

  /**
   * @return the String value for the variable MAVEN_ARGS, or null if called outside an IDEasy installation.
   */
  default String getMavenArgs() {

    if (getIdeHome() == null) {
      return null;
    }
    Mvn mvn = getCommandletManager().getCommandlet(Mvn.class);
    return mvn.getMavenArgs();
  }

  /**
   * @return the {@link Path} pointing to the maven configuration directory (where "settings.xml" or "settings-security.xml" are located).
   */
  default Path getMavenConfigurationFolder() {

    Path confPath = getConfPath();
    Path mvnConfFolder = null;
    if (confPath != null) {
      mvnConfFolder = confPath.resolve(Mvn.MVN_CONFIG_FOLDER);
      if (!Files.isDirectory(mvnConfFolder)) {
        Path m2LegacyFolder = confPath.resolve(Mvn.MVN_CONFIG_LEGACY_FOLDER);
        if (Files.isDirectory(m2LegacyFolder)) {
          mvnConfFolder = m2LegacyFolder;
        } else {
          mvnConfFolder = null; // see fallback below
        }
      }
    }
    if (mvnConfFolder == null) {
      // fallback to USER_HOME/.m2 folder
      mvnConfFolder = getUserHome().resolve(Mvn.MVN_CONFIG_LEGACY_FOLDER);
    }
    return mvnConfFolder;
  }

  /**
   * Updates the current working directory (CWD) and configures the environment paths according to the specified parameters. This method is central to changing
   * the IDE's notion of where it operates, affecting where configurations, workspaces, settings, and other resources are located or loaded from.
   *
   * @return the current {@link Step} of processing.
   */
  Step getCurrentStep();

  /**
   * @param name the {@link Step#getName() name} of the new {@link Step}.
   * @return the new {@link Step} that has been created and started.
   */
  default Step newStep(String name) {

    return newStep(name, Step.NO_PARAMS);
  }

  /**
   * @param name the {@link Step#getName() name} of the new {@link Step}.
   * @param parameters the {@link Step#getParameter(int) parameters} of the {@link Step}.
   * @return the new {@link Step} that has been created and started.
   */
  default Step newStep(String name, Object... parameters) {

    return newStep(false, name, parameters);
  }

  /**
   * @param silent the {@link Step#isSilent() silent flag}.
   * @param name the {@link Step#getName() name} of the new {@link Step}.
   * @param parameters the {@link Step#getParameter(int) parameters} of the {@link Step}.
   * @return the new {@link Step} that has been created and started.
   */
  Step newStep(boolean silent, String name, Object... parameters);

  /**
   * Updates the current working directory (CWD) and configures the environment paths according to the specified parameters. This method is central to changing
   * the IDE's notion of where it operates, affecting where configurations, workspaces, settings, and other resources are located or loaded from.
   *
   * @param ideHome The path to the IDE home directory.
   */
  default void setIdeHome(Path ideHome) {

    setCwd(ideHome, WORKSPACE_MAIN, ideHome);
  }

  /**
   * Updates the current working directory (CWD) and configures the environment paths according to the specified parameters. This method is central to changing
   * the IDE's notion of where it operates, affecting where configurations, workspaces, settings, and other resources are located or loaded from.
   *
   * @param userDir The path to set as the current working directory.
   * @param workspace The name of the workspace within the IDE's environment.
   * @param ideHome The path to the IDE home directory.
   */
  void setCwd(Path userDir, String workspace, Path ideHome);

  /**
   * Finds the path to the Bash executable.
   *
   * @return the {@link String} to the Bash executable, or {@code null} if Bash is not found
   */
  String findBash();

  /**
   * Finds the path to the Bash executable.
   *
   * @return the {@link String} to the Bash executable. Throws an {@link IllegalStateException} if no bash was found.
   */
  default String findBashRequired() {
    String bash = findBash();
    if (bash == null) {
      String message = "Could not find bash what is a prerequisite of IDEasy.";
      if (getSystemInfo().isWindows()) {
        message = message + "\nPlease install Git for Windows and rerun.";
      }
      throw new IllegalStateException(message);
    }
    return bash;
  }

  /**
   * @return the {@link WindowsPathSyntax} used for {@link Path} conversion or {@code null} for no such conversion (typically if not on Windows).
   */
  WindowsPathSyntax getPathSyntax();

  /**
   * logs the status of {@link #getIdeHome() IDE_HOME} and {@link #getIdeRoot() IDE_ROOT}.
   */
  void logIdeHomeAndRootStatus();

  /**
   * @param version the {@link VersionIdentifier} to write.
   * @param installationPath the {@link Path directory} where to write the version to a {@link #FILE_SOFTWARE_VERSION version file}.
   */
  void writeVersionFile(VersionIdentifier version, Path installationPath);

}
