package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

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
    this.context.verifyIdeMinVersion(true);
    this.context.success("Successfully created new project '{}'.", newProjectName);
  }

  private void initializeCodeRepository(String repoUrl) {

    // clone the given repository into IDE_HOME/workspaces/main
    GitUrl gitUrl = GitUrl.of(repoUrl);
    Path codeRepoPath = this.context.getWorkspacePath().resolve(gitUrl.getProjectName());
    this.context.getGitContext().pullOrClone(gitUrl, codeRepoPath);

    // check for settings folder and create symlink to IDE_HOME/settings
    Path settingsFolder = codeRepoPath.resolve(IdeContext.FOLDER_SETTINGS);
    if (Files.exists(settingsFolder)) {
      this.context.getFileAccess().symlink(settingsFolder, this.context.getSettingsPath());
      // create a file in IDE_HOME with the current local commit id
      this.context.getGitContext().saveCurrentCommitId(codeRepoPath, this.context.getSettingsCommitIdPath());
    } else {
      this.context.warning("No settings folder was found inside the code repository.");
    }
  }

  private void initializeProject(Path newInstancePath) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN));
  }

  @Override
  protected void updateSettings() {

    if (this.codeRepositoryFlag.isTrue()) {
      String codeRepository = this.settingsRepo.getValue();
      if (codeRepository == null || codeRepository.isBlank()) {
        String message = """
            No code repository was given after '--code'.
            Please give the code repository below that includes your settings folder.
            Further details can be found here: https://github.com/devonfw/IDEasy/blob/main/documentation/settings.adoc
            Code repository URL:
            """;
        codeRepository = this.context.askForInput(message);
      }
      initializeCodeRepository(codeRepository);
    } else {
      super.updateSettings();
    }

  }
}
