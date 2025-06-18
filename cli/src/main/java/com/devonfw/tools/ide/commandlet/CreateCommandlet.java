package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * {@link Commandlet} to create a new IDEasy instance
 */
public class CreateCommandlet extends AbstractUpdateCommandlet {

  /** {@link StringProperty} for the name of the new project */
  public final StringProperty newProject;

  /** {@link FlagProperty} for creating a project with settings inside a code repository */
  public final FlagProperty codeRepositoryFlag;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CreateCommandlet(IdeContext context) {

    super(context);
    this.newProject = add(new StringProperty("", true, "project"));
    this.codeRepositoryFlag = add(new FlagProperty("--code"));
    add(this.settingsRepo);
  }

  @Override
  public String getName() {

    return "create";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  public void run() {

    String newProjectName = this.newProject.getValue();
    Path newProjectPath = this.context.getIdeRoot().resolve(newProjectName);

    this.context.info("Creating new IDEasy project in {}", newProjectPath);
    if (!this.context.getFileAccess().isEmptyDir(newProjectPath)) {
      this.context.askToContinue("Directory " + newProjectPath + " already exists. Do you want to continue?");
    } else {
      this.context.getFileAccess().mkdirs(newProjectPath);
    }

    initializeProject(newProjectPath);
    this.context.setIdeHome(newProjectPath);
    this.context.verifyIdeMinVersion(true);
    super.run();
    this.context.verifyIdeMinVersion(true);
    this.context.getFileAccess().writeFileContent(IdeVersion.getVersionString(), newProjectPath.resolve(IdeContext.FILE_SOFTWARE_VERSION));
    this.context.success("Successfully created new project '{}'.", newProjectName);
  }

  private void initializeProject(Path newInstancePath) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN));
  }

  /**
   * Check project name convention of a code repository. When project name contains settings keyword, it shows a warning.
   *
   * @param projectName the project name of the repository
   */
  @Override
  protected void checkProjectNameConvention(String projectName) {
    if (projectName.contains(SETTINGS_REPOSITORY_KEYWORD)) {
      String warningTemplate = """
          Your git URL is pointing to the project name {0} that contains the keyword ''{1}''.
          Therefore we assume that you did a mistake by adding the '--code' option to the ide project creation.
          Do you really want to create the project?
          """;
      String warning = MessageFormat.format(warningTemplate, projectName, SETTINGS_REPOSITORY_KEYWORD);
      this.context.askToContinue(warning);
    }
  }

  /**
   * Handles cases for settings and code repository during creation.
   */
  @Override
  protected void processRepository() {
    RepositoryStrategy repositoryStrategy = new SettingsRepositoryStrategy();
    if (this.codeRepositoryFlag.isTrue()) {
      repositoryStrategy = new CodeRepositoryStrategy();
    }

    processRepositoryUsingStrategy(repositoryStrategy);
  }

  private void processRepositoryUsingStrategy(RepositoryStrategy strategy) {
    Step step = strategy.createNewStep(this.context);
    String repository = this.settingsRepo.getValue();
    if (repository == null || repository.isBlank()) {
      repository = strategy.handleBlankRepository(this.context);
    }
    if ("-".equals(repository)) {
      repository = IdeContext.DEFAULT_SETTINGS_REPO_URL;
    }
    GitUrl gitUrl = GitUrl.of(repository);
    strategy.checkProjectNameConvention(this.context, gitUrl.getProjectName());
    strategy.initializeRepository(this.context, gitUrl);
    strategy.resolveStep(step);
  }

  /**
   * Strategy for handling repository.
   */
  interface RepositoryStrategy {

    String handleBlankRepository(IdeContext context);

    void checkProjectNameConvention(IdeContext context, String projectName);

    void initializeRepository(IdeContext context, GitUrl gitUrl);

    Step createNewStep(IdeContext context);

    void resolveStep(Step step);
  }

  /**
   * Strategy implementation for code repository.
   */
  class CodeRepositoryStrategy implements RepositoryStrategy {

    @Override
    public String handleBlankRepository(IdeContext context) {
      String message = """
          No code repository was given after '--code'.
          Please give the code repository below that includes your settings folder.
          Further details can be found here: https://github.com/devonfw/IDEasy/blob/main/documentation/settings.adoc
          Code repository URL:
          """;
      return context.askForInput(message);
    }

    @Override
    public void checkProjectNameConvention(IdeContext context, String projectName) {
      if (projectName.contains(SETTINGS_REPOSITORY_KEYWORD)) {
        String warningTemplate = """
            Your git URL is pointing to the project name {0} that contains the keyword ''{1}''.
            Therefore we assume that you did a mistake by adding the '--code' option to the ide project creation.
            Do you really want to create the project?
            """;
        String warning = MessageFormat.format(warningTemplate, projectName, SETTINGS_REPOSITORY_KEYWORD);
        context.askToContinue(warning);
      }
    }

    @Override
    public void initializeRepository(IdeContext context, GitUrl gitUrl) {
      // clone the given repository into IDE_HOME/workspaces/main
      Path codeRepoPath = context.getWorkspacePath().resolve(gitUrl.getProjectName());
      context.getGitContext().pullOrClone(gitUrl, codeRepoPath);

      // check for settings folder and create symlink to IDE_HOME/settings
      Path settingsFolder = codeRepoPath.resolve(IdeContext.FOLDER_SETTINGS);
      if (Files.exists(settingsFolder)) {
        context.getFileAccess().symlink(settingsFolder, context.getSettingsPath());
        // create a file in IDE_HOME with the current local commit id
        context.getGitContext().saveCurrentCommitId(codeRepoPath, context.getSettingsCommitIdPath());
      } else {
        context.warning("No settings folder was found inside the code repository.");
      }
    }

    @Override
    public Step createNewStep(IdeContext context) {
      return context.newStep("Clone code repository");
    }

    @Override
    public void resolveStep(Step step) {
      step.success("Successfully updated code repository.");
    }
  }

  /**
   * Strategy implementation for settings repository.
   */
  class SettingsRepositoryStrategy implements RepositoryStrategy {

    @Override
    public String handleBlankRepository(IdeContext context) {
      Path settingsPath = context.getSettingsPath();
      String message = "Missing your settings at " + settingsPath + " and no SETTINGS_URL is defined.\n"
          + "Further details can be found here: https://github.com/devonfw/IDEasy/blob/main/documentation/settings.adoc\n"
          + "Please contact the technical lead of your project to get the SETTINGS_URL for your project.\n"
          + "In case you just want to test IDEasy you may simply hit return to install the default settings.\n" + "Settings URL ["
          + IdeContext.DEFAULT_SETTINGS_REPO_URL + "]:";
      return context.askForInput(message, IdeContext.DEFAULT_SETTINGS_REPO_URL);
    }

    @Override
    public void checkProjectNameConvention(IdeContext context, String projectName) {
      if (!projectName.contains(SETTINGS_REPOSITORY_KEYWORD)) {
        String warningTemplate = """
            Your git URL is pointing to the project name {0} that does not contain the keyword ''{1}''.
            Therefore we assume that you forgot to add the '--code' option to the ide project creation.
            Do you really want to create the project?
            """;
        String warning = MessageFormat.format(warningTemplate, projectName, SETTINGS_REPOSITORY_KEYWORD);
        context.askToContinue(warning);
      }
    }

    @Override
    public void initializeRepository(IdeContext context, GitUrl gitUrl) {
      Path settingsPath = context.getSettingsPath();
      GitContext gitContext = context.getGitContext();
      gitContext.pullOrClone(gitUrl, settingsPath);
      context.getGitContext().saveCurrentCommitId(settingsPath, context.getSettingsCommitIdPath());
    }

    @Override
    public Step createNewStep(IdeContext context) {
      return context.newStep("Clone settings repository");
    }

    @Override
    public void resolveStep(Step step) {
      step.success("Successfully updated settings repository.");
    }
  }
}
