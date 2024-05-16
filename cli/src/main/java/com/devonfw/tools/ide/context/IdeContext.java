package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.merge.DirectoryMerger;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.repo.CustomToolRepository;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.variable.IdeVariables;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Interface for interaction with the user allowing to input and output information.
 */
public interface IdeContext extends IdeLogger {

  /**
   * The default settings URL.
   *
   * @see com.devonfw.tools.ide.commandlet.AbstractUpdateCommandlet
   */
  String DEFAULT_SETTINGS_REPO_URL = "https://github.com/devonfw/ide-settings.git";

  /** The name of the workspaces folder. */
  String FOLDER_WORKSPACES = "workspaces";

  /** The name of the settings folder. */
  String FOLDER_SETTINGS = "settings";

  /** The name of the software folder. */
  String FOLDER_SOFTWARE = "software";

  /** The name of the conf folder for project specific user configurations. */
  String FOLDER_CONF = "conf";

  /**
   * The base folder name of the IDE inside IDE_ROOT. Intentionally starting with an underscore and not a dot (to prevent effects like OS hiding, maven
   * filtering, .gitignore, etc.).
   */
  String FOLDER_IDE = "_ide";

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

  /** The file where the installed software version is written to as plain text. */
  String FILE_SOFTWARE_VERSION = ".ide.software.version";

  /** The file where the installed software version is written to as plain text. */
  String FILE_LEGACY_SOFTWARE_VERSION = ".devon.software.version";

  /** The file extension for a {@link java.util.Properties} file. */
  String EXT_PROPERTIES = ".properties";

  /** The default for {@link #getWorkspaceName()}. */
  String WORKSPACE_MAIN = "main";

  /** The folder with the configuration template files from the settings. */
  String FOLDER_TEMPLATES = "templates";

  /** Legacy folder name used as compatibility fallback if {@link #FOLDER_TEMPLATES} does not exist. */
  String FOLDER_LEGACY_TEMPLATES = "devon";

  /**
   * @return {@code true} in case of quiet mode (reduced output), {@code false} otherwise.
   */
  boolean isQuietMode();

  /**
   * @return {@code true} in case of batch mode (no {@link #question(String) user-interaction}), {@code false} otherwise.
   */
  boolean isBatchMode();

  /**
   * @return {@code true} in case of force mode, {@code false} otherwise.
   */
  boolean isForceMode();

  /**
   * @return {@code true} if offline mode is activated (-o/--offline), {@code false} otherwise.
   */
  boolean isOfflineMode();

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
      throw new CliOfflineException("You are offline but Internet access is required for " + purpose);
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
   * @return the {@link Path} to the IDE instance directory. You can have as many IDE instances on the same computer as independent tenants for different
   * isolated projects.
   * @see com.devonfw.tools.ide.variable.IdeVariables#IDE_HOME
   */
  Path getIdeHome();

  /**
   * @return the name of the current project.
   * @see com.devonfw.tools.ide.variable.IdeVariables#PROJECT_NAME
   */
  String getProjectName();

  /**
   * @return the {@link Path} to the IDE installation root directory. This is the top-level folder where the {@link #getIdeHome() IDE instances} are located as
   * sub-folder. There is a reserved ".ide" folder where central IDE data is stored such as the {@link #getUrlsPath() download metadata} and the central
   * software repository.
   * @see com.devonfw.tools.ide.variable.IdeVariables#IDE_ROOT
   */
  Path getIdeRoot();

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
   * download tools.
   * @see com.devonfw.tools.ide.url.model.folder.UrlRepository
   */
  Path getUrlsPath();

  /**
   * @return the {@link UrlMetadata}. Will be lazily instantiated and thereby automatically be cloned or pulled (by default).
   */
  UrlMetadata getUrls();

  /**
   * @return the {@link Path} to the download cache. All downloads will be placed here using a unique naming pattern that allows to reuse these artifacts. So if
   * the same artifact is requested again it will be taken from the cache to avoid downloading it again.
   */
  Path getDownloadPath();

  /**
   * @return the {@link Path} to the software folder inside {@link #getIdeHome() IDE_HOME}. All tools for that IDE instance will be linked here from the
   * {@link #getSoftwareRepositoryPath() software repository} as sub-folder named after the according tool.
   */
  Path getSoftwarePath();

  /**
   * @return the {@link Path} to the extra folder inside software folder inside {@link #getIdeHome() IDE_HOME}. All tools for that IDE instance will be linked
   * here from the {@link #getSoftwareRepositoryPath() software repository} as sub-folder named after the according tool.
   */
  Path getSoftwareExtraPath();

  /**
   * @return the {@link Path} to the global software repository. This is the central directory where the tools are extracted physically on the local disc. Those
   * are shared among all IDE instances (see {@link #getIdeHome() IDE_HOME}) via symbolic links (see {@link #getSoftwarePath()}). Therefore this repository
   * follows the sub-folder structure {@code «repository»/«tool»/«edition»/«version»/}. So multiple versions of the same tool exist here as different folders.
   * Further, such software may not be modified so e.g. installation of plugins and other kind of changes to such tool need to happen strictly out of the scope
   * of this folders.
   */
  Path getSoftwareRepositoryPath();

  /**
   * @return the {@link Path} to the {@link #FOLDER_PLUGINS plugins folder} inside {@link #getIdeHome() IDE_HOME}. All plugins of the IDE instance will be
   * stored here. For each tool that supports plugins a sub-folder with the tool name will be created where the plugins for that tool get installed.
   */
  Path getPluginsPath();

  /**
   * @return the {@link Path} to the central tool repository. All tools will be installed in this location using the directory naming schema of
   * {@code «repository»/«tool»/«edition»/«version»/}. Actual {@link #getIdeHome() IDE instances} will only contain symbolic links to the physical tool
   * installations in this repository. This allows to share and reuse tool installations across multiple {@link #getIdeHome() IDE instances}. The variable
   * {@code «repository»} is typically {@code default} for the tools from our standard {@link #getUrlsPath() ide-urls download metadata} but this will differ
   * for custom tools from a private repository.
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
   * @return the {@link Path} to the {@code settings} folder with the cloned git repository containing the project configuration.
   */
  Path getSettingsPath();

  /**
   * @return the {@link Path} to the {@code conf} folder with instance specific tool configurations and the
   * {@link EnvironmentVariablesType#CONF user specific project configuration}.
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
   * {@link #getSoftwarePath() software path} unless {@link #getIdeHome() IDE_HOME} was not found.
   */
  SystemPath getPath();

  /**
   * @return the current {@link Locale}. Either configured via command-line option or {@link Locale#getDefault() default}.
   */
  Locale getLocale();

  /**
   * @return a new {@link ProcessContext} to {@link ProcessContext#run() run} external commands.
   */
  ProcessContext newProcess();

  /**
   * Prepares the {@link IdeProgressBar} initializes task name and maximum size as well as the behaviour and style.
   *
   * @param taskName name of the task.
   * @param size of the content.
   * @return {@link IdeProgressBar} to use.
   */
  IdeProgressBar prepareProgressBar(String taskName, long size);

  /**
   * @return the {@link DirectoryMerger} used to configure and merge the workspace for an {@link com.devonfw.tools.ide.tool.ide.IdeToolCommandlet IDE}.
   */
  DirectoryMerger getWorkspaceMerger();

  /**
   * @return the {@link Path} to the working directory from where the command is executed.
   */
  Path getDefaultExecutionDirectory();

  /**
   * @return the {@link GitContext} used to run several git commands.
   */
  GitContext getGitContext();

  /**
   * @return the String value for the variable MAVEN_ARGS, or null if called outside an IDEasy installation.
   */
  default String getMavenArgs() {

    if (getIdeHome() != null) {
      Path mvnSettingsFile = getConfPath().resolve(Mvn.MVN_CONFIG_FOLDER).resolve(Mvn.SETTINGS_FILE);
      if (Files.exists(mvnSettingsFile)) {
        return "-s " + mvnSettingsFile;
      }
    }
    return null;

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

}
