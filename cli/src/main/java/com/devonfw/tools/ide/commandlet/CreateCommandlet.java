package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitUrl;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.property.BooleanProperty;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * {@link Commandlet} to create a new IDEasy instance
 */
public class CreateCommandlet extends AbstractUpdateCommandlet {

  /** {@link StringProperty} for the name of the new project */
  public final StringProperty newProject;

  /** {@link FlagProperty} for skipping the setup of git repositories */
  public final FlagProperty skipRepositories;

  public final FlagProperty codeRepositoryFlag;

  public final StringProperty codeRepository;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CreateCommandlet(IdeContext context) {

    super(context);
    this.newProject = add(new StringProperty("", true, "project"));
    this.skipRepositories = add(new FlagProperty("--skip-repositories"));
    this.codeRepositoryFlag = add(new FlagProperty("--code"));
    this.codeRepository = add(new StringProperty("", false, ""));
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
    if (codeRepositoryFlag.isTrue()) {
      String repoUrl = codeRepository.getValue();
      if (repoUrl.isEmpty()) {
        this.context.info("Please provide a repository URL after using --code");
      } else {
        initializeCodeRepository(repoUrl);
      }
    }
    super.run();

    if (this.skipRepositories.isTrue()) {
      this.context.info("Skipping the cloning of project repositories as specified by the user.");
    } else {
      updateRepositories();
    }
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
    }

    // create a file in IDE_HOME with the current local commit id
    this.context.getGitContext().saveCurrentCommitId(codeRepoPath);
  }

  private void initializeProject(Path newInstancePath) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_SOFTWARE));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_PLUGINS));
    fileAccess.mkdirs(newInstancePath.resolve(IdeContext.FOLDER_WORKSPACES).resolve(IdeContext.WORKSPACE_MAIN));
  }

  private void updateRepositories() {

    this.context.getCommandletManager().getCommandlet(RepositoryCommandlet.class).run();
  }
}
