package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;
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
    super.run();
    this.context.getFileAccess().writeFileContent(IdeVersion.getVersionString(), newProjectPath.resolve(IdeContext.FILE_SOFTWARE_VERSION));
    this.context.success("Successfully created new project '{}'.", newProjectName);

    logWelcomeMessage(newProjectPath);
  }

  private void initializeProject(Path newInstancePath) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN));
  }

  @Override
  protected void processRepository() {
    RepositoryStrategy repositoryStrategy = new SettingsRepositoryStrategy();
    if (isCodeRepository()) {
      repositoryStrategy = new CodeRepositoryStrategy();
    }

    processRepositoryUsingStrategy(repositoryStrategy);
  }

  @Override
  protected boolean isCodeRepository() {
    return this.codeRepositoryFlag.isTrue();
  }

  private void logWelcomeMessage(Path newProjectPath) {
    GitUrl gitUrl = GitUrl.of(newProjectPath.toString());
    Path codeRepoPath = this.context.getWorkspacePath().resolve(gitUrl.getProjectName());
    Path settingsFolder = codeRepoPath.resolve(IdeContext.FOLDER_SETTINGS);
    if (Files.exists(settingsFolder)) {
      Predicate<Path> welcomePredicate = path -> String.valueOf(path.getFileName()).startsWith("welcome.");
      Path welcomeFilePath = this.context.getFileAccess().findFirst(settingsFolder, welcomePredicate, false);
      if (welcomeFilePath != null) {
        this.context.info(this.context.getFileAccess().readFileContent(welcomeFilePath));
      }
    }
  }
}
